package com.pokehub.soulink;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoulinkNuzlocke implements ModInitializer {
    public static final String MOD_ID = "soulink_nuzlocke";
    public static final Logger LOGGER = LoggerFactory.getLogger("SoulinkNuzlocke");

    public static NuzConfig CONFIG = new NuzConfig();
    public static final RunStore STORE = new RunStore();
    public static final Rules RULES = new Rules();

    @Override
    public void onInitialize() {
        CONFIG = NuzConfig.load();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> STORE.attach(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> STORE.detach());
        ServerTickEvents.END_SERVER_TICK.register(RULES::tick);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> NuzCommands.register(dispatcher));

        RULES.registerCobblemonEvents();
        LOGGER.info("Soulink Nuzlocke loaded. One biome, one catch. Faint = gone.");
    }
}
