package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    @Shadow private int scaledWidth;
    @Unique
    private static final String prefix1 = "\uEA6E ";
    @Unique
    private static final String chars = "뀍뀇뀐뀄뀀뀊";
    @Unique
    private static final String prefix2 = " \uEA72\uEA6F\uEA6E\uEA6C";
    @Unique
    private DrawContext context = null;

    @Inject(method = "renderHeldItemTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    public void getCurrentContext(DrawContext context, CallbackInfo ci) {
        this.context = context;
    }

    @ModifyArgs(method = "renderHeldItemTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"))
    public void processItemName(Args args) {
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

            if (cfg.centeredItemNameBackground) {
                context.drawText(args.get(0), String.valueOf(startStr.charAt(2)), (scaledWidth - 160) / 2, args.get(3), args.get(4), false);
            }
        }
    }

}
