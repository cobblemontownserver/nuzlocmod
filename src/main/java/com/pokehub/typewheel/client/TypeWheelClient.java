package com.pokehub.typewheel.client;

import com.pokehub.typewheel.TypeWheelMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public class TypeWheelClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(TypeWheelMod.WHEEL_BE, TypeWheelRenderer::new);
    }
}
