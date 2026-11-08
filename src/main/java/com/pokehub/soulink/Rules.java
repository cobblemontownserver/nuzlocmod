package com.pokehub.soulink;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent;
import com.cobblemon.mod.common.api.events.pokeball.ThrownPokeballHitEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonGainedEvent;
import com.cobblemon.mod.common.api.storage.StoreCoordinates;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** All the Nuzlocke logic: catch gating, soul-linking, and death handling. */
public class Rules {
    public static final String TAG_LINKED = "soulinked";
    public static final String TAG_BIOME = "soulinked_biome";

    private record PendingDeath(UUID owner, Pokemon pokemon, String biome) {}

    /** battleId -> deaths waiting for the battle to finish before we release. */
    private final Map<UUID, List<PendingDeath>> pending = new HashMap<>();
    private int tickCounter = 0;

    private static NuzConfig cfg() { return SoulinkNuzlocke.CONFIG; }

    // ---------------------------------------------------------------- setup

    public void registerCobblemonEvents() {
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(Priority.HIGHEST, event -> {
            onBallHit(event);
            return Unit.INSTANCE;
        });
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            onCaptured(event);
            return Unit.INSTANCE;
        });
        CobblemonEvents.POKEMON_GAINED.subscribe(Priority.NORMAL, event -> {
            onGained(event);
            return Unit.INSTANCE;
        });
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.NORMAL, event -> {
            onFainted(event);
            return Unit.INSTANCE;
        });
    }

    // ---------------------------------------------------------------- helpers

    public static String biomeAt(Entity e) {
        return biomeAt(e.level(), e.blockPosition());
    }

    public static String biomeAt(net.minecraft.world.level.Level level, BlockPos pos) {
        Holder<Biome> holder = level.getBiome(pos);
        return holder.unwrapKey().map(k -> k.location().toString()).orElse("unknown");
    }

    private static boolean runActive(ServerPlayer player) {
        return cfg().enabled && SoulinkNuzlocke.STORE.get(player.getUUID()).active;
    }

    public static boolean isLinked(Pokemon pokemon) {
        if (cfg().linkAllOwned) return true;
        CompoundTag tag = pokemon.getPersistentData();
        return tag.contains(TAG_LINKED) && tag.getBoolean(TAG_LINKED);
    }

    private static void link(Pokemon pokemon, String biome) {
        CompoundTag tag = pokemon.getPersistentData();
        tag.putBoolean(TAG_LINKED, true);
        if (biome != null && !tag.contains(TAG_BIOME)) tag.putString(TAG_BIOME, biome);
    }

    private static boolean ownsSpecies(ServerPlayer player, ResourceLocation species) {
        for (Pokemon p : Cobblemon.INSTANCE.getStorage().getParty(player)) {
            if (p.getSpecies().getResourceIdentifier().equals(species)) return true;
        }
        for (Pokemon p : Cobblemon.INSTANCE.getStorage().getPC(player)) {
            if (p.getSpecies().getResourceIdentifier().equals(species)) return true;
        }
        return false;
    }

    private static String nameOf(Pokemon p) {
        return p.getDisplayName().getString();
    }

    // ---------------------------------------------------------------- catching

    private void onBallHit(ThrownPokeballHitEvent event) {
        Entity thrower = event.getPokeBall().getOwner();
        if (!(thrower instanceof ServerPlayer player)) return;
        if (!runActive(player)) return;

        PokemonEntity target = event.getPokemon();
        Pokemon pokemon = target.getPokemon();
        if (pokemon.isPlayerOwned()) return; // not a wild Pokemon, leave it alone

        // Shiny clause: always allowed.
        if (cfg().shinyClause && pokemon.getShiny()) return;

        // Dupes clause: can't catch a species you already own (biome is NOT used up).
        if (cfg().dupesClause && ownsSpecies(player, pokemon.getSpecies().getResourceIdentifier())) {
            event.cancel();
            Msg.warn(player, "Dupes clause: you already own a " + pokemon.getSpecies().getName()
                    + ". This biome's encounter is still open - find something new.");
            return;
        }

        // One catch per biome.
        if (cfg().oneCatchPerBiome) {
            String biome = biomeAt(target);
            RunData data = SoulinkNuzlocke.STORE.get(player.getUUID());
            if (data.usedBiomes.contains(biome)) {
                event.cancel();
                Msg.bad(player, "You already caught your one Pokemon in " + Msg.prettyBiome(biome)
                        + ". The ball bounces off.");
            }
        }
    }

    private void onCaptured(PokemonCapturedEvent event) {
        ServerPlayer player = event.getPlayer();
        Pokemon pokemon = event.getPokemon();
        if (!runActive(player)) return;

        String biome = biomeAt(event.getPokeBallEntity());
        RunData data = SoulinkNuzlocke.STORE.get(player.getUUID());

        boolean freeCatch = cfg().shinyClause && pokemon.getShiny();
        link(pokemon, biome);
        data.catches++;

        if (cfg().oneCatchPerBiome && !freeCatch) {
            data.usedBiomes.add(biome);
            Msg.good(player, nameOf(pokemon) + " is now soul-linked to you. "
                    + Msg.prettyBiome(biome) + " is used up - if it faints, it's gone forever.");
        } else if (freeCatch) {
            Msg.good(player, "Shiny clause! " + nameOf(pokemon)
                    + " is soul-linked and didn't use up " + Msg.prettyBiome(biome) + ".");
        } else {
            Msg.good(player, nameOf(pokemon) + " is now soul-linked to you.");
        }
        SoulinkNuzlocke.STORE.save(player.getUUID());
    }

    private void onGained(PokemonGainedEvent event) {
        if (!cfg().linkAllOwned) return;
        Pokemon pokemon = event.getPokemon();
        CompoundTag tag = pokemon.getPersistentData();
        if (tag.contains(TAG_LINKED)) return; // already handled (capture path)
        link(pokemon, null);
    }

    // ---------------------------------------------------------------- deaths

    private void onFainted(BattleFaintedEvent event) {
        if (!cfg().releaseOnFaint) return;
        Pokemon pokemon = event.getKilled().getEffectedPokemon();
        UUID owner = pokemon.getOwnerUUID();
        if (owner == null) return; // wild or NPC Pokemon
        if (!isLinked(pokemon)) return;
        if (!cfg().pvpCounts && event.getBattle().isPvP()) return;

        ServerPlayer player = null;
        for (ServerPlayer p : event.getBattle().getPlayers()) {
            if (p.getUUID().equals(owner)) { player = p; break; }
        }
        if (player != null && !runActive(player)) return;
        if (player == null && !cfg().enabled) return;

        String biome = pokemon.getPersistentData().contains(TAG_BIOME)
                ? pokemon.getPersistentData().getString(TAG_BIOME) : "unknown";

        pending.computeIfAbsent(event.getBattle().getBattleId(), k -> new ArrayList<>())
                .add(new PendingDeath(owner, pokemon, biome));

        if (player != null) {
            Msg.bad(player, nameOf(pokemon) + " has fallen. It will be released when the battle ends.");
        }
    }

    /** Runs every server tick. Releases fallen Pokemon once their battle is over, and handles biome-entry messages. */
    public void tick(MinecraftServer server) {
        tickCounter++;

        if (!pending.isEmpty()) {
            Iterator<Map.Entry<UUID, List<PendingDeath>>> it = pending.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, List<PendingDeath>> entry = it.next();
                if (BattleRegistry.INSTANCE.getBattle(entry.getKey()) == null) {
                    for (PendingDeath d : entry.getValue()) release(server, d);
                    it.remove();
                }
            }
        }

        if (cfg().biomeEntryMessages && cfg().enabled && tickCounter % 20 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                RunData data = SoulinkNuzlocke.STORE.get(player.getUUID());
                if (!data.active) continue;
                String biome = biomeAt(player);
                if (data.visitedBiomes.add(biome)) {
                    if (data.usedBiomes.contains(biome)) {
                        Msg.info(player, "Entering " + Msg.prettyBiome(biome) + " - encounter already used.");
                    } else {
                        Msg.send(player, "New biome: " + Msg.prettyBiome(biome)
                                + ". You may catch ONE Pokemon here.", ChatFormatting.AQUA);
                    }
                    SoulinkNuzlocke.STORE.save(player.getUUID());
                }
            }
        }
    }

    private void release(MinecraftServer server, PendingDeath d) {
        Pokemon pokemon = d.pokemon();
        String name = nameOf(pokemon);
        String species = pokemon.getSpecies().getName();
        int level = pokemon.getLevel();

        boolean removed = false;
        StoreCoordinates<?> coords = pokemon.getStoreCoordinates().get();
        if (coords != null) {
            removed = coords.getStore().remove(pokemon);
        }
        if (!removed) {
            SoulinkNuzlocke.LOGGER.warn("Could not release {} ({}) - it was not found in a store", name, pokemon.getUuid());
        }

        RunData data = SoulinkNuzlocke.STORE.get(d.owner());
        RunData.Grave grave = new RunData.Grave();
        grave.species = species;
        grave.nickname = name;
        grave.level = level;
        grave.biome = d.biome();
        grave.time = System.currentTimeMillis();
        data.graveyard.add(grave);
        SoulinkNuzlocke.STORE.save(d.owner());

        ServerPlayer owner = server.getPlayerList().getPlayer(d.owner());
        if (owner != null) {
            Msg.bad(owner, name + " (Lv. " + level + ") has been released. Rest well.");
            if (cfg().broadcastDeaths) {
                Component msg = Msg.prefix().append(Component.literal(owner.getName().getString() + "'s " + name
                        + " (Lv. " + level + ") has fallen in battle.").withStyle(ChatFormatting.DARK_RED));
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (p != owner) p.sendSystemMessage(msg);
                }
            }
        }
    }
}
