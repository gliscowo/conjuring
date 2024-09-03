package com.glisco.conjuring.mixin;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.items.ConjuringItems;
import com.glisco.conjuring.items.soul_alloy_tools.SoulAlloyTool;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "applyAttributeModifiers(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("RETURN"))
    private static void applyHaste(ItemStack stack, EquipmentSlot slot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeModifierConsumer, CallbackInfo ci) {
        if (!(stack.getItem() instanceof SoulAlloyTool) || slot != EquipmentSlot.MAINHAND) return;
        if (stack.getItem() == ConjuringItems.SOUL_ALLOY_SWORD) return;

        var level = SoulAlloyTool.getModifierLevel(stack, SoulAlloyTool.SoulAlloyModifier.HASTE);
        if (level > 0) {
            attributeModifierConsumer.accept(EntityAttributes.PLAYER_MINING_EFFICIENCY, new EntityAttributeModifier(
                    Conjuring.id("tool_modifier.efficiency"), level * level + 1, EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    @Inject(method = "applyAttributeModifiers(Lnet/minecraft/item/ItemStack;Lnet/minecraft/component/type/AttributeModifierSlot;Ljava/util/function/BiConsumer;)V", at = @At("RETURN"))
    private static void applyHaste(ItemStack stack, AttributeModifierSlot slot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeModifierConsumer, CallbackInfo ci) {
        if (!(stack.getItem() instanceof SoulAlloyTool) || slot != AttributeModifierSlot.MAINHAND) return;
        if (stack.getItem() == ConjuringItems.SOUL_ALLOY_SWORD) return;

        var level = SoulAlloyTool.getModifierLevel(stack, SoulAlloyTool.SoulAlloyModifier.HASTE);
        if (level > 0) {
            attributeModifierConsumer.accept(EntityAttributes.PLAYER_MINING_EFFICIENCY, new EntityAttributeModifier(
                    Conjuring.id("tool_modifier.efficiency"), level * level + 1, EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    @Inject(method = "getEquipmentLevel", at = @At("RETURN"), cancellable = true)
    private static void applyAbundanceLooting(RegistryEntry<Enchantment> enchantment, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (!enchantment.getKey().map(key -> key.equals(Enchantments.LOOTING)).orElse(false)) return;

        if (!(entity.getMainHandStack().getItem() == ConjuringItems.SOUL_ALLOY_SWORD || entity.getMainHandStack().getItem() == ConjuringItems.SOUL_ALLOY_HATCHET))
            return;

        cir.setReturnValue(cir.getReturnValue() + SoulAlloyTool.getModifierLevel(entity.getMainHandStack(), SoulAlloyTool.SoulAlloyModifier.ABUNDANCE));
    }

    @Inject(method = "getLevel", at = @At(value = "RETURN"), cancellable = true)
    private static void applyAbundanceFortune(RegistryEntry<Enchantment> enchantment, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!enchantment.getKey().map(key -> key == Enchantments.FORTUNE).orElse(false)) return;
        if (!(stack.getItem() == ConjuringItems.SOUL_ALLOY_PICKAXE
                || stack.getItem() == ConjuringItems.SOUL_ALLOY_SHOVEL
                || stack.getItem() == ConjuringItems.SOUL_ALLOY_SCYTHE)) return;

        cir.setReturnValue(cir.getReturnValue() + SoulAlloyTool.getModifierLevel(stack, SoulAlloyTool.SoulAlloyModifier.ABUNDANCE));
    }

    @Inject(method = "getItemDamage", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/mutable/MutableFloat;intValue()I"))
    private static void applyIgnoranceToDurability(ServerWorld world, ItemStack stack, int baseItemDamage, CallbackInfoReturnable<Integer> cir, @Local MutableFloat damage) {
        var ignoranceLevel = SoulAlloyTool.getModifierLevel(stack, SoulAlloyTool.SoulAlloyModifier.IGNORANCE);
        if (ignoranceLevel < 1 || stack.isOf(ConjuringItems.SOUL_ALLOY_SWORD)) return;

        var unbreaking = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getOrEmpty(Enchantments.UNBREAKING);
        if (unbreaking.isEmpty()) return;

        unbreaking.get().modifyItemDamage(world, ignoranceLevel, stack, damage);
    }

}
