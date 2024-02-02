package io.github.amelonrind.emberutils.feature;

import io.github.amelonrind.emberutils.EmberUtils;
import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FactoryNotifier {
    private static final String TITLE = "\uE500\uE500\uE500\uE500\uE500\uE500\uE500\uE500㊜";
    private static final int[] slots = new int[] { 21, 22, 23 };
    private static final Pattern timePattern = Pattern.compile("^剩餘時間: (?:(\\d+)d)?\\s?(?:(\\d+)h)?\\s?(?:(\\d+)m)?\\s?(?:(\\d+)s)?$", Pattern.CASE_INSENSITIVE);

    public static void onCloseGui(String title, GenericContainerScreenHandler handler) {
        if (!TITLE.equals(title)) return;
        long now = System.currentTimeMillis();
        for (int s : slots) {
            Slot slot = handler.getSlot(s);
            if (!slot.hasStack()) break;
            ItemStack item = slot.getStack();
            if (item.isEmpty()) break;
            if (!"生產進行中".equals(item.getName().getString())) continue;
            NbtCompound display = item.getSubNbt("display");
            if (display == null || !display.contains("Lore", NbtElement.LIST_TYPE)) break;
            NbtList lore = display.getList("Lore", NbtElement.STRING_TYPE);
            int size = lore.size();
            if (size < 3) break;

            Text productText = Text.Serialization.fromJson(lore.getString(size - 3));
            if (productText == null) break;
            String product = productText.getString();
            if (!product.startsWith("產品: ")) break;
            product = product.substring("產品: ".length());
            int index = product.indexOf("x ");
            if (index != -1) product = product.substring(index + 2);

            Text timeText = Text.Serialization.fromJson(lore.getString(size - 1));
            if (timeText == null) break;
            Matcher matcher = timePattern.matcher(timeText.getString());
            if (!matcher.matches()) break;
            int time = EmberUtils.parseDuration(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
            if (time <= 0) break;
            var ongoings = Notifier.instance().factories;
            if (!ongoings.containsKey(product) && Config.get().factoryNotification) {
                EmberUtils.logTranslatableChat("start_factory", product);
            }
            long ends = now + time + 1000;
            ongoings.put(product, ends);
            Notifier.minSoonest(ends);
            Notifier.save();
            break;
        }
    }

    public static boolean check(long now) {
        var ongoings = Notifier.instance().factories;
        for (String product : ongoings.keySet()) {
            if (now < ongoings.get(product)) continue;
            ongoings.remove(product);
            Config cfg = Config.get();
            if (cfg.factoryNotification) {
                EmberUtils.logTranslatableChat("finish_factory", product);
                if (cfg.requestsAttention) EmberUtils.requestWindowAttention();
            }
            UraniumHud.onProductFinish(product, cfg);
            Notifier.save();
            return true;
        }
        return false;
    }

}
