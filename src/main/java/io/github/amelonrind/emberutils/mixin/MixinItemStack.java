package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @Unique
    private static final LinkedHashMap<String, String> enchantmentTextCache = new LinkedHashMap<>(32, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 24;
        }

    };
    @Unique
    private static final String prefix = "\uEA6E 뀁 \uEA72\uEA6F\uEA6E\uEA6C";
    @Unique
    private static final String prefixChar = "뀁";
    @Shadow
    @Nullable
    private NbtCompound nbt;
    @Unique
    private Boolean isMmoItem = null;

    @Unique
    private boolean isMmoItem() {
        if (isMmoItem == null) {
            assert nbt != null;
            if (nbt.contains("display", 10)) {
                NbtCompound nbtCompound = this.nbt.getCompound("display");
                if (nbtCompound.getType("Lore") == 9) {
                    NbtList nbtList = nbtCompound.getList("Lore", 8);
                    int size = nbtList.size();
                    if (size > 1) {
                        String string = nbtList.getString(size - 2);
                        try {
                            MutableText text = Text.Serialization.fromJson(string);
                            if (text != null && text.getString().startsWith(prefix)) {
                                isMmoItem = true;
                            }
                        } catch (Exception ignore) {}
                    }
                }
            }
            if (isMmoItem == null) isMmoItem = false;
        }
        return isMmoItem;
    }

    @ModifyArgs(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;appendEnchantments(Ljava/util/List;Lnet/minecraft/nbt/NbtList;)V"))
    private void appendEnchantments(Args args) {
        if (!Config.get().enchantmentTooltipFix) return;
        List<Text> tooltip = args.get(0);
        NbtList enchantments = args.get(1);
        args.set(1, new NbtList());
        Function<Text, Text> func = isMmoItem() ? t -> Text.literal(prefix).append(t) : t -> t;
        Language lang = Language.getInstance();
        for (int i = 0; i < enchantments.size(); ++i) {
            NbtCompound nbtCompound = enchantments.getCompound(i);
            Registries.ENCHANTMENT.getOrEmpty(EnchantmentHelper.getIdFromNbt(nbtCompound)).ifPresent((e) -> {
                String str = lang.get(e.getTranslationKey());
                String cache = enchantmentTextCache.get(str);
                if (cache != null) {
                    str = cache;
                } else {
                    String res = str;
                    int index = res.indexOf(prefix);
                    if (index != -1) {
                        res = res.substring(0, index) + res.substring(index + prefix.length());
                    } else {
                        index = res.indexOf(prefixChar);
                        if (index != -1) {
                            res = res.substring(0, index) + res.substring(index + 1);
                        }
                    }
                    enchantmentTextCache.put(str, res);
                    str = res;
                }

                MutableText text = Text.literal(str);
                text.formatted(e.isCursed() ? Formatting.RED : Formatting.GRAY);

                int level = EnchantmentHelper.getLevelFromNbt(nbtCompound);
                if (level != 1 || e.getMaxLevel() != 1) {
                    text.append(ScreenTexts.SPACE).append(Text.translatable("enchantment.level." + level));
                }

                tooltip.add(func.apply(text));
            });
        }
    }

}
