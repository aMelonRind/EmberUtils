package io.github.amelonrind.emberutils.feature;

import io.github.amelonrind.emberutils.EmberUtils;
import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtInt;
import net.minecraft.text.Text;

import java.util.function.Consumer;

import static io.github.amelonrind.emberutils.EmberUtils.mc;

public class UraniumHud {
    private static final ItemStack item = Items.PAPER.getDefaultStack();
    private static final String uranium = "鈾碎片";
    private static final Text checkText = EmberUtils.translatable("check_uranium");
    private static Consumer<DrawContext> renderMethod = UraniumHud::noRender;
    private static Text text = checkText;
    private static int x = 0;
    private static int y = 0;
    private static int color = 0xFFFFFF;
    private static long halfSecs = 0L;

    static {
        item.setSubNbt("CustomModelData", NbtInt.of(39));
    }

    public static void halfSec(long now, long halfSecs) {
        UraniumHud.halfSecs = halfSecs;
        check(now);
    }

    public static void onResolutionChanged(long now) {
        check(now);
    }

    public static void check(long now) {
        if (!Config.get().uraniumHud) {
            renderMethod = UraniumHud::noRender;
            return;
        }
        var ongoings = Notifier.instance().factories;
        if (ongoings.containsKey(uranium)) {
            renderMethod = UraniumHud::renderCountdown;
            text = Text.literal(EmberUtils.toDurationString((int) (ongoings.get(uranium) - now) / 1000));
            x = (EmberUtils.screenWidth - mc.textRenderer.getWidth(text) + 20) / 2;
        } else {
            renderMethod = UraniumHud::renderNotify;
            text = checkText;
            color = (halfSecs & 1) == 0 ? 0xFF0000 : 0xAA0000;
            x = EmberUtils.screenWidth / 2;
            y = EmberUtils.screenHeight / 2 - 24;
        }
    }

    public static void render(DrawContext context) {
        renderMethod.accept(context);
    }

    public static void noRender(DrawContext context) {}

    public static void renderCountdown(DrawContext context) {
        context.drawTextWithShadow(mc.textRenderer, text, x, 32, 0xDDDDDD);
        context.drawItem(item, x - 18, 29);
    }

    public static void renderNotify(DrawContext context) {
        context.drawCenteredTextWithShadow(mc.textRenderer, text, x, y, color);
    }

    public static void onProductFinish(String product, Config cfg) {
        if (uranium.equals(product)) {
            if (cfg.uraniumHud && !cfg.requestsAttention) EmberUtils.requestWindowAttention();
        }
    }

}
