package com.pokehub.soulink;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Per-player Nuzlocke run state, saved as JSON in the world folder. */
public class RunData {
    public boolean active = true;
    /** Biome ids (e.g. "minecraft:forest") where the one allowed catch has been used. */
    public Set<String> usedBiomes = new HashSet<>();
    /** Biome ids the player has visited at least once (for the "new biome" message). */
    public Set<String> visitedBiomes = new HashSet<>();
    /** Fallen soul-linked Pokemon. */
    public List<Grave> graveyard = new ArrayList<>();
    public int catches = 0;

    public static class Grave {
        public String species;
        public String nickname;
        public int level;
        public String biome;
        public long time;
    }
}
