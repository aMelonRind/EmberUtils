package io.github.amelonrind.emberutils;

import io.github.amelonrind.emberutils.config.Config;
import io.github.amelonrind.emberutils.feature.DeliveryHelper;
import io.github.amelonrind.emberutils.feature.Notifier;
import io.github.amelonrind.emberutils.feature.UraniumHud;
import io.github.amelonrind.emberutils.feature.VisibleBossBar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
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

    public static void logTranslatableChat(String key) {
        logChat(translatable(key));
    }

    public static void logTranslatableChat(String key, Object ...args) {
        logChat(translatable(key, args));
    }

    @Override
    public void onInitializeClient() {
        Config.HANDLER.load();
        Config cfg = Config.get();
        VisibleBossBar.onConfigChanged(cfg);
        DeliveryHelper.onConfigChanged(cfg);
        Notifier.load();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private static long lastHalfSecs = 0;
    private void tick(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        Notifier.tick(now);
        DeliveryHelper.tick();

        long halfSec = now / 500;
        if (lastHalfSecs != halfSec) {
            lastHalfSecs = halfSec;
            halfSec(now);
        }
    }

    private void halfSec(long now) {
        UraniumHud.halfSec(now, lastHalfSecs);
        DeliveryHelper.halfSec(lastHalfSecs);
    }

    public static int screenWidth = 720;
    public static int screenHeight = 480;
    public static int guiScale = 2;
    public static void onResolutionChanged() {
        Window w = mc.getWindow();
        screenWidth = w.getScaledWidth();
        screenHeight = w.getHeight();
        guiScale = mc.options.getGuiScale().getValue();

        long now = System.currentTimeMillis();
        UraniumHud.onResolutionChanged(now);
        DeliveryHelper.onResolutionChanged();
    }

    public static void render(DrawContext context, float ignoredTickDelta) {
        UraniumHud.render(context);
        DeliveryHelper.render(context);
    }

    public static boolean isNpc(@NotNull Entity entity) {
        return entity.isSilent() && entity.hasNoGravity()
                && entity instanceof PlayerEntity e
                && e.getMaxHealth() == 1024.0;
    }

    public static void logChat(Text text) {
        mc.inGameHud.getChatHud().addMessage(Text.empty().append(chatPrefix).append(text));
    }

    public static int parseDuration(@Nullable String day, @Nullable String hour, @Nullable String minute, @Nullable String second) {
        int time = 0;
        if (day != null) try {
            int days = Integer.parseInt(day);
            time = days * 24;
        } catch (NumberFormatException ignore) {}
        if (hour != null) try {
            int hours = Integer.parseInt(hour);
            time += hours;
        } catch (NumberFormatException ignore) {}
        time *= 60;
        if (minute != null) try {
            int minutes = Integer.parseInt(minute);
            time += minutes;
        } catch (NumberFormatException ignore) {}
        time *= 60;
        if (second != null) {
            try {
                int seconds = Integer.parseInt(second);
                time += seconds;
            } catch (NumberFormatException ignore) {}
        }
        return time * 1000;
    }

    public static String toDurationString(int seconds) {
        String res;
        if (seconds < 0) {
            res = "-";
            seconds = -seconds;
        } else res = "";
        if (seconds >= 60 * 60) {
            if (seconds >= 24 * 60 * 60) {
                res += seconds / 60 / 60 / 24 + "d";
                int hours = seconds / 60 / 60 % 24;
                if (hours > 0) res += " " + hours + "h";
            } else {
                res += seconds / 60 / 60 + "h";
                int minutes = seconds / 60 % 60;
                if (minutes > 0) res += " " + minutes + "m";
            }
        } else if (seconds >= 60) {
            res += seconds / 60 + "m";
            int secs = seconds % 60;
            if (secs > 0) res += " " + secs + "s";
        } else res += seconds + "s";
        return res;
    }

    public static void requestWindowAttention() {
        if (!mc.isWindowFocused()) GLFW.glfwRequestWindowAttention(mc.getWindow().getHandle());
    }

}
