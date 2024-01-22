package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.EmberUtils;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Keyboard.class)
public class MixinKeyboard {

    @Inject(method = "processF3", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;clear(Z)V"))
    private void onF3plusD(int key, CallbackInfoReturnable<Boolean> cir) {
        EmberUtils.isClearingChatWithF3D = true;
    }

}
