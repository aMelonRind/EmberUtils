package io.github.amelonrind.emberutils.features;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

public class GemstoneTooltip {
    private static final String appliedGemstoneText = "\uEA6E 뀁 \uEA72\uEA6F\uEA6E\uEA6C♦ 已鑲嵌";
    private static final String prefix = "\uEA6E 뀁 \uEA72\uEA6F\uEA6E\uEA6C";
    private static final String prefix2 = " \uEA72\uEA6F\uEA6E\uEA6C";

    public static void revealRunes(CallbackInfoReturnable<List<Text>> cir, NbtCompound nbt) {
        if (!Config.get().gemstoneTooltip) return;
        if (nbt == null || !nbt.contains("MMOITEMS_GEM_STONES", NbtElement.STRING_TYPE)) return;
        try {
            JsonArray gemstones = JsonParser.parseString(nbt.getString("MMOITEMS_GEM_STONES"))
                    .getAsJsonObject().get("Gemstones").getAsJsonArray();
            if (gemstones.isEmpty()) return;
            List<Text> tooltip = cir.getReturnValue();
            int size = tooltip.size();

            int startIndex = 0;
            while (startIndex < size) {
                if (tooltip.get(startIndex).getString().equals(appliedGemstoneText)) break;
                startIndex++;
            }
            if (startIndex == size) return;
            int endIndex = startIndex + 1;
            int limit = Math.min(size, startIndex + gemstones.size() + 1);
            while (endIndex < limit) {
                if (!tooltip.get(endIndex).getString().equals(appliedGemstoneText)) break;
                endIndex++;
            }
            if (endIndex - startIndex != gemstones.size()) return;
            size = gemstones.size();
            for (int i = 0; i < size; i++) {
                JsonObject gemstone = gemstones.get(i).getAsJsonObject();
                String name = gemstone.get("Name").getAsString();
                String color = gemstone.get("Color").getAsString();
                if (name.startsWith(prefix2, 5)) name = name.substring(10);
                color = "§" + color.charAt(1);
                tooltip.set(startIndex + i, Text.literal(prefix + color + "<§f" + name + color + ">"));
            }
        } catch (Exception ignore) {}
    }

}
