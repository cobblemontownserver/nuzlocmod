package com.pokehub.typewheel.client;

import com.pokehub.typewheel.TypeWheelMod;
import net.fabricmc.api.ClientModInitializer;
import com.pokehub.typewheel.WheelResultPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class TypeWheelClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(TypeWheelMod.WHEEL_BE, TypeWheelRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(WheelResultPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ResultOverlay.show(payload.typeIndex(), payload.spinner())));
        HudRenderCallback.EVENT.register((graphics, delta) -> ResultOverlay.render(graphics));
    }
}
