package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.feature.VisibleBossBar;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class MixinBossBarHud {

    @Shadow @Final Map<UUID, ClientBossBar> bossBars;

    @Inject(method = "handlePacket", at = @At("TAIL"))
    private void handlePacket(BossBarS2CPacket packet, CallbackInfo ci) {
        VisibleBossBar.onPacket(packet, bossBars);
    }

}
