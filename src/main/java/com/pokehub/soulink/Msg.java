package com.pokehub.soulink;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/** Chat helpers. */
public final class Msg {
    private Msg() {}

    public static MutableComponent prefix() {
        return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("Nuzlocke").withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void send(ServerPlayer p, String text, ChatFormatting color) {
        p.sendSystemMessage(prefix().append(Component.literal(text).withStyle(color)));
    }

    public static void info(ServerPlayer p, String text) { send(p, text, ChatFormatting.GRAY); }
    public static void good(ServerPlayer p, String text) { send(p, text, ChatFormatting.GREEN); }
    public static void warn(ServerPlayer p, String text) { send(p, text, ChatFormatting.GOLD); }
    public static void bad(ServerPlayer p, String text)  { send(p, text, ChatFormatting.RED); }

    /** "minecraft:dark_forest" -> "Dark Forest" */
    public static String prettyBiome(String id) {
        String s = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder out = new StringBuilder();
        for (String part : s.split("[_/]")) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
