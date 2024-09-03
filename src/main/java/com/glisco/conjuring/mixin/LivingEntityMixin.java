package com.glisco.conjuring.mixin;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.items.soul_alloy_tools.CopycatPlayerDamageSource;
import com.glisco.conjuring.items.soul_alloy_tools.SoulAlloyTool;
import com.glisco.conjuring.items.soul_alloy_tools.SoulAlloyToolAbilities;
import com.glisco.conjuring.util.ConjuringParticleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Unique
    private float damageReduction = -1;

    @Shadow
    protected abstract void applyDamage(DamageSource source, float amount);

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    // ---
    // Scope handler
    // ---

    //Gets all entities in the weapons aoe and damages them according to scope level
    @Inject(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;applyArmorToDamage(Lnet/minecraft/entity/damage/DamageSource;F)F"))
    public void applySwordAoe(DamageSource source, float amount, CallbackInfo ci) {
        if (source instanceof CopycatPlayerDamageSource) return;
        if (!source.getName().equals("player")) return;
        if (source.getAttacker() == null) return;

        final PlayerEntity player = (PlayerEntity) source.getAttacker();

        if (!SoulAlloyToolAbilities.canAoeHit(player)) return;

        final int scopeLevel = SoulAlloyTool.getModifierLevel(player.getMainHandStack(), SoulAlloyTool.SoulAlloyModifier.SCOPE);
        final int range = 2 + scopeLevel;

        List<Entity> entities = getWorld().getOtherEntities(this, new Box(getPos().subtract(range, 1, range), getPos().add(range, 1, range)), entity -> entity instanceof LivingEntity);

        entities.remove(player);

        for (int i = 0; i < Conjuring.CONFIG.tools_config.sword_scope_max_entities() && i < entities.size(); i++) {
            entities.get(i).damage(new CopycatPlayerDamageSource(player), amount * Conjuring.CONFIG.tools_config.sword_scope_damage_multiplier() * scopeLevel);
            player.getMainHandStack().damage(4 * scopeLevel, player, EquipmentSlot.MAINHAND);

            if (!getWorld().isClient()) {
                ConjuringParticleEvents.LINE.spawn(getWorld(), getPos(), new ConjuringParticleEvents.Line(
                        getPos().add(0, 0.25 + random.nextDouble(), 0),
                        entities.get(i).getPos().add(0, 0.25 + random.nextDouble(), 0)
                ));
            }
        }

    }

    // ---
    //Ignorance handlers
    // ---

    //Calculates the amount of armor piercing damage and applies it
    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"))
    public void calculateDamageReduction(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!source.getName().equals("player")) return;
        if (source.getAttacker() == null) return;

        final PlayerEntity player = (PlayerEntity) source.getAttacker();

        if (!SoulAlloyToolAbilities.canArmorPierce(player)) return;

        float pierceDamage = SoulAlloyTool.getModifierLevel(player.getMainHandStack(), SoulAlloyTool.SoulAlloyModifier.IGNORANCE) * Conjuring.CONFIG.tools_config.sword_ignorance_multiplier() * amount;
        applyDamage(new CopycatPlayerDamageSource(player).pierceArmor(), pierceDamage);
        damageReduction = pierceDamage;
    }

    //Reduces the normal damage so that it's even with the original amount again
    @ModifyVariable(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isInvulnerableTo(Lnet/minecraft/entity/damage/DamageSource;)Z", shift = At.Shift.AFTER), ordinal = 0)
    public float applyDamageReduction(float amount) {
        if (damageReduction == -1) return amount;

        float reductionCopy = damageReduction;
        damageReduction = -1;
        return amount - reductionCopy;
    }
}
