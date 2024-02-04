package io.github.amelonrind.emberutils.mixin;

import io.github.amelonrind.emberutils.IMixinInteractionEntity;
import net.minecraft.entity.decoration.InteractionEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InteractionEntity.class)
public abstract class MixinInteractionEntity implements IMixinInteractionEntity {
    @Unique
    private boolean canHitOverride = true;

    @Override
    public void emberUtils$overrideCanHit(boolean value) {
        canHitOverride = value;
    }

    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void canHit(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(canHitOverride);
    }

}
