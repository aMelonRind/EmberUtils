package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.feature.AutoSelectNextItem;
import io.github.amelonrind.emberutils.feature.DeliveryHelper;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;setStack(ILnet/minecraft/item/ItemStack;)V"))
    public void onInventorySlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        DeliveryHelper.onInventoryUpdate();
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/PlayerScreenHandler;setStackInSlot(IILnet/minecraft/item/ItemStack;)V"))
    private void onSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        AutoSelectNextItem.onSlotUpdate(packet);
        DeliveryHelper.onInventoryUpdate();
    }

}
