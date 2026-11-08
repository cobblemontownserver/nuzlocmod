package com.pokehub.soulink;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class NuzCommands {
    private NuzCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nuzlocke")
                .executes(ctx -> status(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("graveyard").executes(ctx -> graveyard(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("biomes").executes(ctx -> biomes(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("admin").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("reload").executes(ctx -> {
                            SoulinkNuzlocke.CONFIG = NuzConfig.load();
                            ctx.getSource().sendSuccess(() -> Component.literal("Nuzlocke config reloaded."), true);
                            return 1;
                        }))
                        .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    SoulinkNuzlocke.STORE.reset(target.getUUID());
                                    Msg.warn(target, "Your Nuzlocke run was reset by an admin. All biomes are open again.");
                                    ctx.getSource().sendSuccess(() -> Component.literal("Reset run for " + target.getName().getString()), true);
                                    return 1;
                                })))
                        .then(Commands.literal("toggle").then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    RunData d = SoulinkNuzlocke.STORE.get(target.getUUID());
                                    d.active = !d.active;
                                    SoulinkNuzlocke.STORE.save(target.getUUID());
                                    Msg.warn(target, "Nuzlocke rules are now " + (d.active ? "ON" : "OFF") + " for you.");
                                    ctx.getSource().sendSuccess(() -> Component.literal("Nuzlocke " + (d.active ? "enabled" : "disabled") + " for " + target.getName().getString()), true);
                                    return 1;
                                })))
                        .then(Commands.literal("unlockbiome").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("biome", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    String biome = StringArgumentType.getString(ctx, "biome");
                                    RunData d = SoulinkNuzlocke.STORE.get(target.getUUID());
                                    boolean ok = d.usedBiomes.remove(biome);
                                    SoulinkNuzlocke.STORE.save(target.getUUID());
                                    ctx.getSource().sendSuccess(() -> Component.literal(ok ? "Unlocked " + biome : "That biome wasn't used: " + biome), true);
                                    return ok ? 1 : 0;
                                }))))));
    }

    private static int status(ServerPlayer p) {
        RunData d = SoulinkNuzlocke.STORE.get(p.getUUID());
        NuzConfig c = SoulinkNuzlocke.CONFIG;
        String here = Rules.biomeAt(p);
        Msg.send(p, "Run: " + (c.enabled && d.active ? "ACTIVE" : "OFF")
                + " | Catches: " + d.catches + " | Fallen: " + d.graveyard.size()
                + " | Biomes used: " + d.usedBiomes.size(), ChatFormatting.LIGHT_PURPLE);
        Msg.info(p, "Here: " + Msg.prettyBiome(here) + " - "
                + (d.usedBiomes.contains(here) ? "encounter USED" : "encounter OPEN"));
        List<String> clauses = new ArrayList<>();
        if (c.shinyClause) clauses.add("Shiny");
        if (c.dupesClause) clauses.add("Dupes");
        if (c.linkAllOwned) clauses.add("Starter linked");
        Msg.info(p, "Clauses: " + (clauses.isEmpty() ? "none" : String.join(", ", clauses)));
        return 1;
    }

    private static int graveyard(ServerPlayer p) {
        RunData d = SoulinkNuzlocke.STORE.get(p.getUUID());
        if (d.graveyard.isEmpty()) {
            Msg.info(p, "No fallen Pokemon. Keep it that way.");
            return 1;
        }
        Msg.send(p, "Graveyard (" + d.graveyard.size() + "):", ChatFormatting.DARK_RED);
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d");
        for (RunData.Grave g : d.graveyard) {
            String label = g.nickname != null && !g.nickname.equals(g.species) ? g.nickname + " the " + g.species : g.species;
            Msg.info(p, " - " + label + " Lv." + g.level + " from " + Msg.prettyBiome(g.biome) + " (" + fmt.format(new Date(g.time)) + ")");
        }
        return 1;
    }

    private static int biomes(ServerPlayer p) {
        RunData d = SoulinkNuzlocke.STORE.get(p.getUUID());
        if (d.usedBiomes.isEmpty()) {
            Msg.info(p, "You haven't used any biome encounters yet.");
            return 1;
        }
        Msg.send(p, "Used biomes (" + d.usedBiomes.size() + "):", ChatFormatting.GOLD);
        for (String b : d.usedBiomes) Msg.info(p, " - " + Msg.prettyBiome(b));
        return 1;
    }
}
