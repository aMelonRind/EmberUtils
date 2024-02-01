package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.features.MmoItemNameFix;
import io.github.amelonrind.emberutils.features.UraniumHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    @Shadow private int scaledWidth;
    @Unique
    private DrawContext context = null;

    @Inject(method = "renderHeldItemTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    public void getCurrentContext(DrawContext context, CallbackInfo ci) {
        this.context = context;
    }

    @ModifyArgs(method = "renderHeldItemTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"))
    public void processItemName(Args args) {
        MmoItemNameFix.onRenderHeldItemName(context, scaledWidth, args);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/DebugHud;shouldShowDebugHud()Z"), method = "render")
    public void renderHud(DrawContext context, float tickDelta, CallbackInfo ci) {
        UraniumHud.render(context);
    }

}
