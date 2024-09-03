package com.glisco.conjuring.mixin;

import com.glisco.conjuring.items.soul_alloy_tools.SoulAlloyToolAbilities;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    // TODO pain
    @ModifyVariable(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getVelocity()Lnet/minecraft/util/math/Vec3d;"), ordinal = 3)
    public boolean disableSweepForScope(boolean sweep) {
        if (!SoulAlloyToolAbilities.canAoeHit((PlayerEntity) (Object) this)) return sweep;
        return false;
    }

}
