package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.EmberUtils;
import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class MixinChatHud {
    @Unique
    private static final List<String> chars = List.of("뀍", "뀇", "뀐", "뀄", "뀀", "뀊");
    @Unique
    private static final Pattern pattern = Pattern.compile("(?:\uEA6E )?[뀍뀇뀐뀄뀀뀊](?: \uEA72\uEA6F\uEA6E\uEA6C)?");

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At(value = "HEAD"),
            argsOnly = true
    )
    private Text removeMmoItemNameBackground(Text text) {
        if (!Config.get().mmoItemNameFix) return text;
        String str = text.getString();
        if (chars.stream().noneMatch(str::contains)) return text;
        MutableText t = Text.empty();
        text.visit((style, st) -> {
            t.append(Text.literal(pattern.matcher(st).replaceAll("")).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return t;
    }

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    private void keepMessages(boolean clearHistory, CallbackInfo ci) {
        if (EmberUtils.isClearingChatWithF3D) {
            EmberUtils.isClearingChatWithF3D = false;
            return;
        }
        if (Config.get().keepChat) ci.cancel();
    }

}
