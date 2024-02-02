package io.github.amelonrind.emberutils.feature;

import io.github.amelonrind.emberutils.EmberUtils;
import io.github.amelonrind.emberutils.LoreHelper;
import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

import static io.github.amelonrind.emberutils.EmberUtils.mc;

public class PurchaseBlocker {
    private static final Set<String> TITLES = Set.of(
            "\uE500\uE500\uE500\uE500\uE500\uE500\uE500\uE500㊜",
            "\uE500\uE500\uE500\uE500\uE500\uE500\uE500\uE500\uE503"
    );
    private static GenericContainerScreenHandler lastHandler = null;
    private static int lastSlot = -1;
    private static int clickedTimes = 0;

    public static void onClickSlot(ScreenHandler handler, int syncId, int slotId, CallbackInfo ci) {
        if (!Config.get().purchaseBlocker) return;
        if (slotId >= 45) return;
        if (syncId != handler.syncId) return;
        if (!(handler instanceof GenericContainerScreenHandler h)) return;
        if (h.getRows() != 5) return;
        Screen s = mc.currentScreen;
        if (s instanceof GenericContainerScreen gs && gs.getScreenHandler() == h) {
            if (!TITLES.contains(gs.getTitle().getString())) return;
        }
        Slot slot = handler.getSlot(slotId);
        if (!slot.hasStack()) return;
        ItemStack item = slot.getStack();
        if (item.isEmpty()) return;
        if (!LoreHelper.from(item).startsWith(-3, "購買")) return;

        if (lastHandler != h || lastSlot != slotId) {
            lastHandler = h;
            lastSlot = slotId;
            clickedTimes = 1;
            ci.cancel();
            EmberUtils.logTranslatableChat("purchase_blocked");
        } else {
            clickedTimes++;
            if (clickedTimes > 2) return;
            ci.cancel();
            EmberUtils.logTranslatableChat("purchase_blocked2");
        }
    }

}
