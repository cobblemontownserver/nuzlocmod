package com.pokehub.typewheel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TypeWheelBlockEntity extends BlockEntity {
    public static final String[] TYPES = {
        "Normal", "Fire", "Water", "Electric", "Grass", "Ice", "Fighting", "Poison", "Ground",
        "Flying", "Psychic", "Bug", "Rock", "Ghost", "Dragon", "Dark", "Steel", "Fairy"};
    public static final int[] COLORS = {
        0xA8A77A, 0xEE8130, 0x6390F0, 0xF7D02C, 0x7AC74C, 0x96D9D6, 0xC22E28, 0xA33EA1, 0xE2BF65,
        0xA98FF3, 0xF95587, 0xA6B91A, 0xB6A136, 0x735797, 0x6F35FC, 0x705746, 0xB7B7CE, 0xD685AD};
    public static final float SEGMENT = 360.0F / TYPES.length;

    private static final Map<UUID, Long> LAST_SPIN = new HashMap<>();

    private float restAngle;
    private boolean spinning;
    private long spinStart;
    private float startAngle;
    private float endAngle;
    private int duration = 140;
    private int result = -1;
    private UUID spinner;
    private String spinnerName = "";

    // client only
    private int lastClick = Integer.MIN_VALUE;

    public TypeWheelBlockEntity(BlockPos pos, BlockState state) {
        super(TypeWheelMod.WHEEL_BE, pos, state);
    }

    /** Wheel rotation in degrees, clockwise when you look at the front. */
    public float getAngle(float partialTick) {
        if (!spinning || level == null) return restAngle;
        float t = (level.getGameTime() - spinStart + partialTick) / (float) Math.max(1, duration);
        t = Mth.clamp(t, 0.0F, 1.0F);
        float inv = 1.0F - t;
        float eased = 1.0F - inv * inv * inv;
        return startAngle + (endAngle - startAngle) * eased;
    }

    private Vec3 wheelCenter() {
        Direction f = getBlockState().getValue(TypeWheelBlock.FACING);
        return Vec3.atBottomCenterOf(worldPosition).add(f.getStepX() * 0.5, WheelLayout.CENTER_Y, f.getStepZ() * 0.5);
    }

    public void trySpin(ServerPlayer player) {
        if (level == null || level.isClientSide) return;
        if (spinning) {
            player.displayClientMessage(Component.literal("The wheel is already spinning!").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        WheelConfig cfg = WheelConfig.INSTANCE;
        long now = System.currentTimeMillis();
        if (cfg.cooldownSeconds > 0) {
            Long last = LAST_SPIN.get(player.getUUID());
            if (last != null) {
                long left = cfg.cooldownSeconds * 1000L - (now - last);
                if (left > 0) {
                    player.displayClientMessage(Component.literal("You can spin again in " + formatTime(left) + ".").withStyle(ChatFormatting.RED), true);
                    return;
                }
            }
        }
        LAST_SPIN.put(player.getUUID(), now);

        result = level.random.nextInt(TYPES.length);
        float jitter = (level.random.nextFloat() - 0.5F) * (SEGMENT * 0.7F);
        float finalMod = Mth.positiveModulo(-(result * SEGMENT + SEGMENT / 2.0F + jitter), 360.0F);
        float delta = Mth.positiveModulo(finalMod - Mth.positiveModulo(restAngle, 360.0F), 360.0F);
        int turns = 4 + level.random.nextInt(3);
        startAngle = restAngle;
        endAngle = restAngle + delta + 360.0F * turns;
        duration = Math.max(20, (int) Math.round(cfg.spinSeconds * 20.0));
        spinStart = level.getGameTime();
        spinning = true;
        spinner = player.getUUID();
        spinnerName = player.getGameProfile().getName();
        sync();
        Vec3 c = wheelCenter();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    private static String formatTime(long ms) {
        long s = (ms + 999) / 1000;
        if (s >= 3600) return (s / 3600) + "h " + ((s % 3600) / 60) + "m";
        if (s >= 60) return (s / 60) + "m " + (s % 60) + "s";
        return s + "s";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TypeWheelBlockEntity be) {
        if (be.spinning && level.getGameTime() >= be.spinStart + be.duration) be.finish();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TypeWheelBlockEntity be) {
        if (!be.spinning) {
            be.lastClick = Integer.MIN_VALUE;
            return;
        }
        float angle = be.getAngle(0.0F);
        int idx = Mth.floor(angle / SEGMENT);
        if (be.lastClick != Integer.MIN_VALUE && idx != be.lastClick) {
            float t = Mth.clamp((level.getGameTime() - be.spinStart) / (float) Math.max(1, be.duration), 0.0F, 1.0F);
            Vec3 c = be.wheelCenter();
            level.playLocalSound(c.x, c.y + WheelLayout.RADIUS, c.z, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS,
                    0.9F, 1.6F - 0.8F * t, false);
        }
        be.lastClick = idx;
    }

    private void finish() {
        spinning = false;
        restAngle = Mth.positiveModulo(endAngle, 360.0F);
        startAngle = endAngle = restAngle;
        sync();
        if (!(level instanceof ServerLevel sl) || result < 0 || result >= TYPES.length) return;

        String type = TYPES[result];
        int color = COLORS[result];
        Vec3 c = wheelCenter();
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.2F);
        sl.sendParticles(ParticleTypes.FIREWORK, c.x, c.y, c.z, 60, 1.2, 1.2, 1.2, 0.08);
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y, c.z, 30, 1.5, 1.5, 1.5, 0.0);

        MutableComponent typeText = Component.literal(type.toUpperCase())
                .withStyle(s -> s.withColor(TextColor.fromRgb(color)).withBold(true));
        MutableComponent msg = Component.literal("✦ ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(spinnerName).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" spun the Type Wheel and landed on ").withStyle(ChatFormatting.GRAY))
                .append(typeText)
                .append(Component.literal("!").withStyle(ChatFormatting.GRAY));

        MinecraftServer server = sl.getServer();
        ServerPlayer who = spinner == null ? null : server.getPlayerList().getPlayer(spinner);
        double r = WheelConfig.INSTANCE.announceRadius;
        WheelResultPayload payload = new WheelResultPayload(result, spinnerName);
        for (ServerPlayer p : sl.players()) {
            if (p == who || (r > 0 && p.distanceToSqr(c) <= r * r)) {
                p.sendSystemMessage(msg);
                if (ServerPlayNetworking.canSend(p, WheelResultPayload.TYPE)) {
                    ServerPlayNetworking.send(p, payload);
                } else if (p == who) {
                    p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(spinnerName + " spun the Type Wheel!").withStyle(ChatFormatting.GRAY)));
                    p.connection.send(new ClientboundSetTitleTextPacket(typeText.copy()));
                }
            }
        }
        for (String cmd : WheelConfig.INSTANCE.commandsOnLand) {
            if (cmd == null || cmd.isBlank()) continue;
            String line = cmd.replace("%player%", spinnerName).replace("%type%", type.toLowerCase());
            try {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), line);
            } catch (Exception e) {
                TypeWheelMod.LOGGER.warn("Type Wheel command failed: {}", line, e);
            }
        }
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("RestAngle", restAngle);
        tag.putBoolean("Spinning", spinning);
        tag.putLong("SpinStart", spinStart);
        tag.putFloat("StartAngle", startAngle);
        tag.putFloat("EndAngle", endAngle);
        tag.putInt("Duration", duration);
        tag.putInt("Result", result);
        tag.putString("SpinnerName", spinnerName);
        if (spinner != null) tag.putUUID("Spinner", spinner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        restAngle = tag.getFloat("RestAngle");
        spinning = tag.getBoolean("Spinning");
        spinStart = tag.getLong("SpinStart");
        startAngle = tag.getFloat("StartAngle");
        endAngle = tag.getFloat("EndAngle");
        duration = Math.max(1, tag.getInt("Duration"));
        result = tag.getInt("Result");
        spinnerName = tag.getString("SpinnerName");
        spinner = tag.hasUUID("Spinner") ? tag.getUUID("Spinner") : null;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
