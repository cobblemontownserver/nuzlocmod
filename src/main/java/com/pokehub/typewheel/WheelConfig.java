package com.pokehub.typewheel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/** Optional settings, saved to config/typewheel.json. The defaults work with no editing. */
public class WheelConfig {
    public static WheelConfig INSTANCE = new WheelConfig();

    /** How long one spin takes. */
    public double spinSeconds = 7.0;
    /** Seconds each player must wait between spins (0 = no wait). */
    public int cooldownSeconds = 0;
    /** Players within this many blocks see the result in chat (0 = only the spinner). */
    public int announceRadius = 48;
    /** Server commands run when the wheel lands. %player% and %type% are filled in. */
    public List<String> commandsOnLand = new ArrayList<>();

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("typewheel.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.exists(file)) {
                WheelConfig c = gson.fromJson(Files.readString(file), WheelConfig.class);
                if (c != null) INSTANCE = c;
                if (INSTANCE.commandsOnLand == null) INSTANCE.commandsOnLand = new ArrayList<>();
            }
            Files.writeString(file, gson.toJson(INSTANCE));
        } catch (Exception e) {
            TypeWheelMod.LOGGER.warn("Could not read typewheel.json, using defaults", e);
            INSTANCE = new WheelConfig();
        }
    }
}
