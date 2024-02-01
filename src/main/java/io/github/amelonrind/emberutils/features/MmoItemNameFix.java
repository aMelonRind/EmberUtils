package io.github.amelonrind.emberutils.features;

import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class MmoItemNameFix {
    private static final String prefix1 = "\uEA6E ";
    private static final String chars = "뀍뀇뀐뀄뀀뀊";
    private static final String prefix2 = " \uEA72\uEA6F\uEA6E\uEA6C";
    private static final Pattern pattern = Pattern.compile("(?:\uEA6E )?[뀍뀇뀐뀄뀀뀊](?: \uEA72\uEA6F\uEA6E\uEA6C)?");

    public static void onRenderHeldItemName(DrawContext context, int scaledWidth, Args args) {
        Config cfg = Config.get();
        if (!cfg.mmoItemNameFix && !cfg.centeredItemNameBackground) return;
        Text text = args.get(1);
        Optional<String> start = text.visit(Optional::of);
        if (start.isEmpty()) return;
        String startStr = start.get();
        if (startStr.startsWith(prefix1) && startStr.startsWith(prefix2, 3) && chars.contains(String.valueOf(startStr.charAt(2)))) {
            AtomicBoolean isFirst = new AtomicBoolean(true);
            MutableText t = Text.empty();
            text.visit((style, str) -> {
                if (isFirst.get()) {
                    try {
                        t.append(Text.literal(str.substring(8)).setStyle(style));
                    } catch (IndexOutOfBoundsException ignore) {}
                    isFirst.set(false);
                } else {
                    t.append(Text.literal(str).setStyle(style));
                }
                return Optional.empty();
            }, Style.EMPTY);
            args.set(1, t);
            TextRenderer tr = args.get(0);
            args.set(2, (int) args.get(2) + (tr.getWidth(text) - tr.getWidth(t)) / 2);

            if (cfg.centeredItemNameBackground) {
                context.drawText(tr, String.valueOf(startStr.charAt(2)), (scaledWidth - 161) / 2, args.get(3), args.get(4), false);
            }
        }
    }

    public static Text onAddChatMessage(Text text) {
        if (!Config.get().mmoItemNameFix) return text;
        String str = text.getString();
        if (chars.codePoints().allMatch(c -> str.indexOf(c) == -1)) return text;
        MutableText t = Text.empty();
        text.visit((style, st) -> {
            t.append(Text.literal(pattern.matcher(st).replaceAll("")).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return t;
    }

}
