package com.pokehub.soulink;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Loads/saves RunData per player under <world>/soulink-nuzlocke/<uuid>.json */
public class RunStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, RunData> cache = new HashMap<>();
    private MinecraftServer server;

    public void attach(MinecraftServer server) {
        this.server = server;
        this.cache.clear();
    }

    public void detach() {
        saveAll();
        this.server = null;
        this.cache.clear();
    }

    private Path dir() {
        return server.getWorldPath(LevelResource.ROOT).resolve("soulink-nuzlocke");
    }

    public RunData get(UUID player) {
        return cache.computeIfAbsent(player, id -> {
            Path file = dir().resolve(id + ".json");
            if (Files.exists(file)) {
                try {
                    RunData d = GSON.fromJson(Files.readString(file), RunData.class);
                    if (d != null) return d;
                } catch (IOException e) {
                    SoulinkNuzlocke.LOGGER.error("Could not read run data for {}", id, e);
                }
            }
            return new RunData();
        });
    }

    public void save(UUID player) {
        if (server == null) return;
        RunData d = cache.get(player);
        if (d == null) return;
        try {
            Files.createDirectories(dir());
            Files.writeString(dir().resolve(player + ".json"), GSON.toJson(d));
        } catch (IOException e) {
            SoulinkNuzlocke.LOGGER.error("Could not save run data for {}", player, e);
        }
    }

    public void saveAll() {
        for (UUID id : cache.keySet()) save(id);
    }

    public void reset(UUID player) {
        cache.put(player, new RunData());
        save(player);
    }
}
