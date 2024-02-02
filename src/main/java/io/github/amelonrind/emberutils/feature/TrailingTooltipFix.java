package io.github.amelonrind.emberutils.feature;

import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Supplier;

public class TrailingTooltipFix {
    private static final String prefix1 = "\uEA6E ";
    private static final String chars = "뀎뀈뀑뀅뀂뀋";
    private static final String prefix2 = " \uEA72\uEA6F\uEA6E\uEA6C";
    private static final Text prefix = Text.literal("\uEA6E 뀁 \uEA72\uEA6F\uEA6E\uEA6C");

    public static void onGetTooltip(List<Text> tooltip, TooltipContext context, Supplier<Boolean> isMmoItem) {
        if (!Config.get().trailingTooltipFix || !context.isAdvanced() || !isMmoItem.get()) return;
        int lastIndex = tooltip.size() - 1;
        int index = lastIndex;
        while (index > 0) {
            String str = tooltip.get(index).getString();
            if (str.startsWith(prefix1)) {
                if (str.length() != 8) return;
                if (!str.endsWith(prefix2)) return;
                if (!chars.contains(str.substring(2, 3))) return;
                break;
            }
            index--;
        }
        if (lastIndex == index) return;
        Text end = tooltip.remove(index); // remember, lastIndex is now size because removed one
        while (index < lastIndex) {
            Text text = tooltip.get(index);
            tooltip.set(index, Text.empty().append(prefix).append(text));
            index++;
        }
        tooltip.add(prefix);
        tooltip.add(end);
    }

}
