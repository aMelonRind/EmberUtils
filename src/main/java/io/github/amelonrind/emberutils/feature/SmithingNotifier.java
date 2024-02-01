package io.github.amelonrind.emberutils.feature;

import io.github.amelonrind.emberutils.EmberUtils;
import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.amelonrind.emberutils.EmberUtils.mc;

public class SmithingNotifier {
    private static final String TITLE = "\uEA6Eఋ";
    private static final int[] slots = new int[] { 38, 39, 40, 41, 42 };
    private static final Pattern timePattern = Pattern.compile("^即將完成 (?:(\\d+)m)?\\s?(?:(\\d+)s)?\\s*$");
    private static String lastNpcName = null;
    private static Entity lastNpc = null;

    public static void onInteractEntity(Entity entity) {
        if (!EmberUtils.isNpc(entity) || entity == lastNpc) return;
        Text name = entity.getCustomName();
        if (name == null) {
            lastNpcName = null;
            lastNpc = null;
        } else {
            lastNpcName = name.getString().replaceAll("\n", "").replaceAll("  +", " ");
            lastNpc = entity;
        }
    }

    public static void onCloseGui(String title, GenericContainerScreenHandler handler, boolean closing) {
        assert mc.player != null;
        if (lastNpcName == null || !lastNpc.isAlive() || !TITLE.equals(title) || mc.player.distanceTo(lastNpc) > 24) return;
        long now = System.currentTimeMillis();
        List<Long> times = new ArrayList<>(5);
        for (int s : slots) {
            Slot slot = handler.getSlot(s);
            if (!slot.hasStack()) break;
            ItemStack item = slot.getStack();
            if (item.isEmpty()) break;
            if (!"⏳".equals(item.getName().getString())) break;
            NbtCompound display = item.getSubNbt("display");
            if (display == null || !display.contains("Lore", NbtElement.LIST_TYPE)) break;
            NbtList lore = display.getList("Lore", NbtElement.STRING_TYPE);
            int size = lore.size();
            if (size == 2) {
                times.add(0L);
                continue;
            } else if (size != 3) break;

            Text timeText = Text.Serialization.fromJson(lore.getString(0));
            if (timeText == null) break;
            Matcher matcher = timePattern.matcher(timeText.getString());
            if (!matcher.matches()) break;
            int time = EmberUtils.parseDuration(matcher.group(1), matcher.group(2));
            times.add(time <= 0 ? 0L : now + time + 1000);
        }
        var ongoings = Notifier.instance().smiths;
        if (times.isEmpty() || times.stream().allMatch(t -> t == 0)) {
            if (ongoings.remove(lastNpcName) != null) Notifier.save();
        } else {
            if (closing && Config.get().smithingNotification) {
                EmberUtils.logChat(EmberUtils.translatable("start_smithing", lastNpcName, times.size()));
            }
            ongoings.put(lastNpcName, times);
            Notifier.updateSoonest();
            Notifier.save();
        }
    }

    public static void check(long now) {
        var ongoings = Notifier.instance().smiths;
        for (String smith : ongoings.keySet()) {
            List<Long> times = ongoings.get(smith);
            int lastIndex = times.size() - 1;
            int i = lastIndex;
            while (i >= 0 && now < times.get(i)) i--;
            if (i < 0 || times.get(i) == 0) continue;
            if (i == lastIndex) {
                if (Config.get().smithingNotification) {
                    EmberUtils.logChat(EmberUtils.translatable("finish_smithing", smith, times.size()));
                    if (Config.get().requestsAttention) EmberUtils.requestWindowAttention();
                }
                ongoings.remove(smith);
            } else {
                if (Config.get().smithingNotification) {
                    long left = (times.get(lastIndex) - now) / 1000 + 1;
                    String leftStr = (left % 60) + "s";
                    if (left >= 60) leftStr = (left / 60) + "m " + leftStr;
                    EmberUtils.logChat(EmberUtils.translatable("progress_smithing", smith, i + 1, times.size(), leftStr));
                }
                for (;i >= 0; i--) times.set(i, 0L);
            }
            Notifier.save();
            break;
        }
    }

}
