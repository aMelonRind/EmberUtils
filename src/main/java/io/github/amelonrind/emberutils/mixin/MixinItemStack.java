package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.LoreHelper;
import io.github.amelonrind.emberutils.config.Config;
import io.github.amelonrind.emberutils.feature.EnchantmentTooltipFix;
import io.github.amelonrind.emberutils.feature.GemstoneTooltip;
import io.github.amelonrind.emberutils.feature.PrettierItemName;
import io.github.amelonrind.emberutils.feature.TrailingTooltipFix;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
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

@Mixin(value = ItemStack.class, priority = 1002)
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
            LoreHelper lore = LoreHelper.from((ItemStack) (Object) this);
            if (lore.size > 1 && lore.startsWith(-2, prefix)) {
                isMmoItem = true;
                MutableText text = lore.get(-1);
                if (text == null) return true;
                Optional<String> start = text.visit(Optional::of);
                if (start.isEmpty()) return true;
                String str = start.get();
                if (str.length() < 3) return true;
                int index = charsFrom.indexOf(str.charAt(2));
                if (index != -1) {
                    namePrefix = String.valueOf(charsTo.charAt(index));
                }
                return true;
            }
            return isMmoItem = false;
        }
        return isMmoItem;
    }

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void getName(CallbackInfoReturnable<Text> cir) {
        PrettierItemName.onGetName(cir, () -> {
            isMmoItem();
            return namePrefix;
        });
    }

    @ModifyArgs(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;appendEnchantments(Ljava/util/List;Lnet/minecraft/nbt/NbtList;)V"))
    private void appendEnchantments(Args args) {
        EnchantmentTooltipFix.onAppendEnchantments(args, this::isMmoItem);
    }

    @Inject(method = "getTooltip", at = @At("TAIL"))
    private void getTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        TrailingTooltipFix.onGetTooltip(cir.getReturnValue(), context, this::isMmoItem);
    }

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void revealRunes(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        GemstoneTooltip.revealRunes(cir, nbt);
    }

    // feature preventDoubleDurability
    @Unique
    private boolean overrideNextIsDamagedCall = false;

    @Inject(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isDamaged()Z"))
    private void onDamageTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        if (!Config.get().preventDoubleDurability) return;
        if (nbt == null || !nbt.contains("MMOITEMS_MAX_DURABILITY", NbtElement.INT_TYPE)) return;
        overrideNextIsDamagedCall = true;
    }

    @Inject(method = "isDamaged", at = @At("HEAD"), cancellable = true)
    private void isDamaged(CallbackInfoReturnable<Boolean> cir) {
        if (overrideNextIsDamagedCall) {
            cir.setReturnValue(false);
            overrideNextIsDamagedCall = false;
        }
    }

    // feature durabilityDisplayFix
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
