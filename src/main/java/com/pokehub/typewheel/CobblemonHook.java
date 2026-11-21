package com.pokehub.typewheel;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/** Only loaded when Cobblemon is installed. Gives the spinner a random Pokemon of the landed type. */
public final class CobblemonHook {
    private static final Set<String> SPECIAL = Set.of("legendary", "mythical", "ultra_beast", "paradox", "restricted");

    private CobblemonHook() {}

    private static List<Species> pool(String type, boolean allowSpecial, boolean baseOnly) {
        List<Species> out = new ArrayList<>();
        for (Species s : PokemonSpecies.INSTANCE.getImplemented()) {
            boolean match = s.getPrimaryType().getName().equalsIgnoreCase(type)
                    || (s.getSecondaryType() != null && s.getSecondaryType().getName().equalsIgnoreCase(type));
            if (!match) continue;
            if (!allowSpecial) {
                boolean special = false;
                for (String l : s.getLabels()) {
                    if (SPECIAL.contains(l.toLowerCase())) {
                        special = true;
                        break;
                    }
                }
                if (special) continue;
            }
            if (baseOnly && s.getPreEvolution() != null) continue;
            out.add(s);
        }
        return out;
    }

    /** Returns the name of the Pokemon given, or null if none could be given. */
    public static String giveRandom(ServerPlayer player, String type, int level, boolean allowSpecial, boolean baseOnly) {
        List<Species> pool = pool(type, allowSpecial, baseOnly);
        if (pool.isEmpty() && baseOnly) pool = pool(type, allowSpecial, false);
        if (pool.isEmpty()) return null;
        Species pick = pool.get(player.getRandom().nextInt(pool.size()));
        Pokemon pokemon = pick.create(Math.max(1, level));
        Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemon);
        return pick.getName();
    }
}
