package io.github.amelonrind.emberutils;

import io.github.amelonrind.emberutils.config.Config;
import io.github.amelonrind.emberutils.features.Notifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class EmberUtils implements ClientModInitializer {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final String MOD_ID = "emberutils";
    public static final Logger LOGGER = LogManager.getLogger(EmberUtils.class);
    private static final Text chatPrefix = Text.empty()
            .append(Text.literal("[").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
            .append(Text.literal("EmberUtils").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)))
            .append(Text.literal("]").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
            .append(" ");

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull MutableText translatable(String key) {
        return Text.translatable(MOD_ID + "." + key);
    }

    public static @NotNull MutableText translatable(String key, Object ...args) {
        return Text.translatable(MOD_ID + "." + key, args);
    }

    @Override
    public void onInitializeClient() {
        Config.HANDLER.load();
        Notifier.load();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        Notifier.tick(now);
    }

    public static boolean isNpc(@NotNull Entity entity) {
        return entity.isSilent() && entity.hasNoGravity()
                && entity instanceof PlayerEntity e
                && e.getMaxHealth() == 1024.0;
    }

    public static void logChat(Text text) {
        mc.inGameHud.getChatHud().addMessage(Text.empty().append(chatPrefix).append(text));
    }

    public static int parseDuration(@Nullable String minute, @Nullable String second) {
        int time = 0;
        if (minute != null) {
            try {
                int minutes = Integer.parseInt(minute);
                time = minutes * 60;
            } catch (NumberFormatException ignore) {}
        }
        if (second != null) {
            try {
                int seconds = Integer.parseInt(second);
                time += seconds;
            } catch (NumberFormatException ignore) {}
        }
        return time * 1000;
    }

    public static void requestWindowAttention() {
        if (!mc.isWindowFocused()) GLFW.glfwRequestWindowAttention(mc.getWindow().getHandle());
    }

}
