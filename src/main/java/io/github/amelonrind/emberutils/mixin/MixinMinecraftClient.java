package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.EmberUtils;
import io.github.amelonrind.emberutils.feature.DeliveryHelper;
import io.github.amelonrind.emberutils.feature.FactoryNotifier;
import io.github.amelonrind.emberutils.feature.SmithingNotifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.GenericContainerScreenHandler;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Shadow
    @Nullable
    public Screen currentScreen;

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", opcode = Opcodes.PUTFIELD), method = "setScreen")
    public void onOpenScreen(Screen screen, CallbackInfo info) {
        DeliveryHelper.onOpenScreen(screen);
        if (screen != currentScreen && currentScreen instanceof GenericContainerScreen s) {
            GenericContainerScreenHandler h = s.getScreenHandler();
            int rows = h.getRows();
            if (rows == 5) {
                String title = s.getTitle().getString();
                FactoryNotifier.onCloseGui(title, h);
                DeliveryHelper.onCloseGui(title, h);
            } else if (rows == 6) {
                SmithingNotifier.onCloseGui(s.getTitle().getString(), h, screen == null);
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "onResolutionChanged")
    public void onResolutionChanged(CallbackInfo info) {
        EmberUtils.onResolutionChanged();
    }

}
