package io.github.amelonrind.emberutils.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.amelonrind.emberutils.config.Config;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
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
    @Unique
    private static final String prefix1 = "\uEA6E ";
    @Unique
    private static final String prefix2 = " \uEA72\uEA6F\uEA6E\uEA6C";
    @Unique
    private static final String charsFrom = "뀎뀈뀑뀅뀂뀋";
    @Unique
    private static final String charsTo = "뀍뀇뀐뀄뀀뀊";
    @Unique
    private static final String appliedGemstoneText = "\uEA6E 뀁 \uEA72\uEA6F\uEA6E\uEA6C♦ 已鑲嵌";
    @Shadow
    @Nullable
    private NbtCompound nbt;
    @Unique
    private Boolean isMmoItem = null;
    @Unique
    private String namePrefix = null;

    @Unique
    private boolean isMmoItem() {
        if (isMmoItem == null) {
            if (nbt != null && nbt.contains("display", NbtElement.COMPOUND_TYPE)) {
                NbtCompound nbtCompound = this.nbt.getCompound("display");
                if (nbtCompound.getType("Lore") == NbtElement.LIST_TYPE) {
                    NbtList nbtList = nbtCompound.getList("Lore", NbtElement.STRING_TYPE);
                    int size = nbtList.size();
                    if (size > 1) try {
                        MutableText text = Text.Serialization.fromJson(nbtList.getString(size - 2));
                        if (text != null && text.getString().startsWith(prefix)) {
                            isMmoItem = true;
                            MutableText text2 = Text.Serialization.fromJson(nbtList.getString(size - 1));
                            if (text2 != null) {
                                Optional<String> start = text2.visit(Optional::of);
                                if (start.isPresent()) {
                                    String str = start.get();
                                    if (str.length() >= 3) {
                                        int index = charsFrom.indexOf(str.charAt(2));
                                        if (index != -1) {
                                            namePrefix = String.valueOf(charsTo.charAt(index));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignore) {}
                }
            }
            if (isMmoItem == null) isMmoItem = false;
        }
        return isMmoItem;
    }

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void getName(CallbackInfoReturnable<Text> cir) {
        if (!Config.get().prettierItemName || !isMmoItem() || namePrefix == null) return;
        Text text = cir.getReturnValue();
        Optional<String> start = text.visit(Optional::of);
        if (start.isEmpty()) return;
        String startStr = start.get();
        if (startStr.contains(namePrefix)) return;
        if (startStr.startsWith(" ")) {
            AtomicBoolean isFirst = new AtomicBoolean(true);
            MutableText t = Text.empty();
            text.visit((style, str) -> {
                if (isFirst.get()) {
                    try {
                        t.append(Text.literal(str.stripLeading()).setStyle(style));
                    } catch (IndexOutOfBoundsException ignore) {}
                    isFirst.set(false);
                } else {
                    t.append(Text.literal(str).setStyle(style));
                }
                return Optional.empty();
            }, Style.EMPTY);
            text = t;
        }
        cir.setReturnValue(Text.empty()
                .append(Text.literal(prefix1 + namePrefix + prefix2)
                        .setStyle(Style.EMPTY.withItalic(false).withColor(0xFFFFFF)))
                .append(text)
        );
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

    @Unique
    private Integer barStep = null;
    @Unique
    private Integer barColor = null;

    @Inject(method = "isItemBarVisible", at = @At("RETURN"), cancellable = true)
    private void hasMmoDurability(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !Config.get().durabilityDisplayFix) return;
        if (nbt != null && nbt.contains("MMOITEMS_DURABILITY", NbtElement.INT_TYPE) && nbt.contains("MMOITEMS_MAX_DURABILITY", NbtElement.INT_TYPE)) {
            int dur = nbt.getInt("MMOITEMS_DURABILITY");
            int max = nbt.getInt("MMOITEMS_MAX_DURABILITY");
            barStep = Math.round(dur * 13.0F / max);
            float h = Math.max(0.0F, (float) dur / max);
            barColor = MathHelper.hsvToRgb(h / 3.0F, 1.0F, 1.0F);
            if (dur < max) cir.setReturnValue(true);
        }
    }

    @Inject(method = "getItemBarStep", at = @At("HEAD"), cancellable = true)
    private void overrideBarStep(CallbackInfoReturnable<Integer> cir) {
        if (barStep != null) {
            cir.setReturnValue(barStep);
            barStep = null;
        }
    }

    @Inject(method = "getItemBarColor", at = @At("HEAD"), cancellable = true)
    private void overrideBarColor(CallbackInfoReturnable<Integer> cir) {
        if (barColor != null) {
            cir.setReturnValue(barColor);
            barColor = null;
        }
    }

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void revealRunes(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
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
