package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.feature.KeepChat;
import io.github.amelonrind.emberutils.feature.MmoItemNameFix;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class MixinChatHud {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At(value = "HEAD"),
            argsOnly = true
    )
    private Text removeMmoItemNameBackground(Text text) {
        return MmoItemNameFix.onAddChatMessage(text);
    }

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    private void keepMessages(boolean clearHistory, CallbackInfo ci) {
        KeepChat.onClearChat(ci);
    }

}
