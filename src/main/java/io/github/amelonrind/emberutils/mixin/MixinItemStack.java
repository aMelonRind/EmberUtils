package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.config.Config;
import io.github.amelonrind.emberutils.feature.EnchantmentTooltipFix;
import io.github.amelonrind.emberutils.feature.GemstoneTooltip;
import io.github.amelonrind.emberutils.feature.PrettierItemName;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
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

import java.util.List;
import java.util.Optional;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @Unique
    private static final String prefix = "\uEA6E 뀁 \uEA72\uEA6F\uEA6E\uEA6C";
    @Unique
    private static final String charsFrom = "뀎뀈뀑뀅뀂뀋";
    @Unique
    private static final String charsTo = "뀍뀇뀐뀄뀀뀊";
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
        PrettierItemName.onGetName(cir, this::isMmoItem, namePrefix);
    }

    @ModifyArgs(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;appendEnchantments(Ljava/util/List;Lnet/minecraft/nbt/NbtList;)V"))
    private void appendEnchantments(Args args) {
        EnchantmentTooltipFix.onAppendEnchantments(args, this::isMmoItem);
    }

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void revealRunes(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        GemstoneTooltip.revealRunes(cir, nbt);
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

}
