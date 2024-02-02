package io.github.amelonrind.emberutils.feature;

import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;

import static io.github.amelonrind.emberutils.EmberUtils.mc;

public class AutoSelectNextItem {

    public static void onSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet) {
        if (!Config.get().autoSelectNextItem) return;
        if (packet.getSyncId() != 0 || !packet.getStack().isEmpty()) return;
        assert mc.player != null;
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) return;
        PlayerScreenHandler h = mc.player.playerScreenHandler;
        int selected = mc.player.getInventory().selectedSlot;
        if (packet.getSlot() != PlayerScreenHandler.HOTBAR_START + selected) return;

        ItemStack item = h.getSlot(packet.getSlot()).getStack();
        if (item.isEmpty()) return;
        int next = selected == 8 ? 0 : selected + 1;
        int n = next;
        while (n != selected) {
            if (ItemStack.areEqual(item, h.getSlot(PlayerScreenHandler.HOTBAR_START + n).getStack())) break;
            n++;
            if (n > 8) n = 0;
        }
        if (n == selected) n = next;
        mc.player.getInventory().selectedSlot = n;
    }

}
