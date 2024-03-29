package io.github.amelonrind.emberutils.feature;

import io.github.amelonrind.emberutils.EmberUtils;
import io.github.amelonrind.emberutils.LoreHelper;
import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
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
    private static final Pattern timePattern = Pattern.compile("^即將完成 (?:(\\d+)d)?\\s?(?:(\\d+)h)?\\s?(?:(\\d+)m)?\\s?(?:(\\d+)s)?\\s*$", Pattern.CASE_INSENSITIVE);
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
        String days = null;
        String hours = null;
        String minutes = null;
        String seconds = null;
        for (int s : slots) {
            Slot slot = handler.getSlot(s);
            if (!slot.hasStack()) break;
            ItemStack item = slot.getStack();
            if (item.isEmpty()) break;
            if (!"⏳".equals(item.getName().getString())) break;
            LoreHelper lore = LoreHelper.from(item);
            if (lore.size == 2) {
                times.add(0L);
                continue;
            } else if (lore.size != 3) break;

            Text timeText = lore.get(0);
            if (timeText == null) break;
            Matcher matcher = timePattern.matcher(timeText.getString());
            if (!matcher.matches()) break;
            if (matcher.group(1) != null) days = matcher.group(1);
            if (matcher.group(2) != null) hours = matcher.group(2);
            if (matcher.group(3) != null) minutes = matcher.group(3);
            if (matcher.group(4) != null) seconds = matcher.group(4);
            int time = EmberUtils.parseDuration(days, hours, minutes, seconds);
            times.add(time <= 0 ? 0L : now + time + 1000);
        }
        var ongoings = Notifier.instance().smiths;
        if (times.isEmpty() || times.stream().allMatch(t -> t == 0)) {
            if (ongoings.remove(lastNpcName) != null) Notifier.save();
        } else {
            if (closing && Config.get().smithingNotification) {
                EmberUtils.logTranslatableChat("start_smithing", lastNpcName, times.size());
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
                    EmberUtils.logTranslatableChat("finish_smithing", smith, times.size());
                    if (Config.get().requestsAttention) EmberUtils.requestWindowAttention();
                }
                ongoings.remove(smith);
            } else {
                if (Config.get().smithingNotification) {
                    String leftStr = EmberUtils.toDurationString((int) ((times.get(lastIndex) - now) / 1000) + 1);
                    EmberUtils.logTranslatableChat("progress_smithing", smith, i + 1, times.size(), leftStr);
                }
                for (;i >= 0; i--) times.set(i, 0L);
            }
            Notifier.save();
            break;
        }
    }

}
