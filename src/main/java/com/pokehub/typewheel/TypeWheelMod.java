package com.pokehub.typewheel;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeWheelMod implements ModInitializer {
    public static final String MOD_ID = "typewheel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Block WHEEL;
    public static Block PART;
    public static Item WHEEL_ITEM;
    public static BlockEntityType<TypeWheelBlockEntity> WHEEL_BE;

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static BlockBehaviour.Properties props() {
        // Unbreakable in survival (like bedrock); creative players can still remove it.
        return BlockBehaviour.Properties.of()
                .strength(-1.0F, 3600000.0F)
                .noOcclusion()
                .noLootTable()
                .pushReaction(PushReaction.BLOCK);
    }

    @Override
    public void onInitialize() {
        WheelConfig.load();
        PayloadTypeRegistry.playS2C().register(WheelResultPayload.TYPE, WheelResultPayload.CODEC);
        WHEEL = Registry.register(BuiltInRegistries.BLOCK, id("type_wheel"), new TypeWheelBlock(props()));
        PART = Registry.register(BuiltInRegistries.BLOCK, id("type_wheel_part"), new TypeWheelPartBlock(props()));
        WHEEL_ITEM = Registry.register(BuiltInRegistries.ITEM, id("type_wheel"),
                new TypeWheelItem(WHEEL, new Item.Properties()));
        WHEEL_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("type_wheel"),
                BlockEntityType.Builder.of(TypeWheelBlockEntity::new, WHEEL).build(null));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> e.accept(WHEEL_ITEM));
        LOGGER.info("Type Wheel loaded");
    }
}
