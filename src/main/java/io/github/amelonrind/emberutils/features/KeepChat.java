package io.github.amelonrind.emberutils.features;

import io.github.amelonrind.emberutils.config.Config;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class KeepChat {
    public static boolean isClearingChatWithF3D = false;

    public static void onClearChat(CallbackInfo info) {
        if (isClearingChatWithF3D) {
            isClearingChatWithF3D = false;
            return;
        }
        if (Config.get().keepChat) info.cancel();
    }

    public static void onPressF3D() {
        isClearingChatWithF3D = true;
    }

}
