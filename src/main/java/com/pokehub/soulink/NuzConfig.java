package com.pokehub.soulink;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config at config/soulink-nuzlocke.json */
public class NuzConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Master switch. */
    public boolean enabled = true;
    /** Only the first Pokemon caught in each biome counts; later throws in that biome bounce off. */
    public boolean oneCatchPerBiome = true;
    /** Shiny Pokemon can always be caught and do not use up the biome. */
    public boolean shinyClause = true;
    /** You cannot catch a species you already own (party or PC); the biome stays available. */
    public boolean dupesClause = true;
    /** Every Pokemon the player owns (starter, gifts, eggs...) is soul-linked, not just captures. */
    public boolean linkAllOwned = true;
    /** Fainting in battle releases the Pokemon. */
    public boolean releaseOnFaint = true;
    /** Whether fainting in a player-vs-player battle counts as a death. */
    public boolean pvpCounts = true;
    /** Announce deaths to the whole server. */
    public boolean broadcastDeaths = true;
    /** Show a chat message when the player enters a biome they haven't used yet. */
    public boolean biomeEntryMessages = true;
    /** Refund the Poke Ball when a throw is blocked by a rule. */
    public boolean refundBlockedBall = true;

    public static NuzConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("soulink-nuzlocke.json");
        NuzConfig cfg = new NuzConfig();
        try {
            if (Files.exists(path)) {
                cfg = GSON.fromJson(Files.readString(path), NuzConfig.class);
                if (cfg == null) cfg = new NuzConfig();
            }
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(cfg));
        } catch (IOException e) {
            SoulinkNuzlocke.LOGGER.error("Could not read/write config", e);
        }
        return cfg;
    }
}
