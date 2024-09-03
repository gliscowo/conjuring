package com.glisco.conjuring.items.soul_alloy_tools;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.entities.SoulProjectileEntity;
import com.glisco.conjuring.items.ConjuringItems;
import io.wispforest.owo.itemgroup.OwoItemSettings;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class SoulAlloySword extends SwordItem implements SoulAlloyTool {

    public SoulAlloySword() {
        super(SoulAlloyToolMaterial.INSTANCE, new OwoItemSettings().group(Conjuring.CONJURING_GROUP).rarity(Rarity.UNCOMMON)
            .attributeModifiers(createAttributeModifiers(SoulAlloyToolMaterial.INSTANCE, 3, -2.4f)));
    }

    @Override
    public void deriveStackComponents(ComponentMap source, ComponentChanges.Builder target) {
        if (SoulAlloyTool.getModifierLevel(source, SoulAlloyTool.SoulAlloyModifier.HASTE) < 1) return;

        var attributes = source.getOrDefault(
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            AttributeModifiersComponent.DEFAULT
        ).with(
            EntityAttributes.GENERIC_ATTACK_SPEED,
            new EntityAttributeModifier(
                Identifier.ofVanilla("base_attack_speed"),
                -2.4f + Math.pow(SoulAlloyTool.getModifierLevel(source, SoulAlloyTool.SoulAlloyModifier.HASTE), Conjuring.CONFIG.tools_config.sword_haste_exponent()),
                EntityAttributeModifier.Operation.ADD_VALUE
            ),
            AttributeModifierSlot.MAINHAND
        );

        target.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, attributes);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!SoulAlloyTool.isSecondaryEnabled(user.getStackInHand(hand))) return TypedActionResult.pass(user.getStackInHand(hand));

        if (!world.isClient()) {

            float baseDamage = (float) (user.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * 1.5f * Conjuring.CONFIG.tools_config.sword_projectile_damage_multiplier());

            for (int i = 0; i < 5; i++) {
                SoulProjectileEntity projectile = new SoulProjectileEntity(world, user);
                projectile.refreshPositionAndAngles(user.getX(), user.getEyeY(), user.getZ(), 0, 0);
                projectile.setVelocity(user, user.getPitch(), user.getYaw() - 10 + 5 * i, 0f, 1.5f, 1);
                projectile.setBaseDamage(baseDamage);
                projectile.setSwordStack(user.getStackInHand(hand));

                world.spawnEntity(projectile);
            }

            user.getItemCooldownManager().set(ConjuringItems.SOUL_ALLOY_SWORD, Conjuring.CONFIG.tools_config.sword_secondary_cooldown());
            user.getStackInHand(hand).damage(Conjuring.CONFIG.tools_config.sword_secondary_durability_cost(), user, LivingEntity.getSlotForHand(hand));
        }

        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void postProcessComponents(ItemStack stack) {
        super.postProcessComponents(stack);


    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return SoulAlloyTool.isSecondaryEnabled(stack) || super.isItemBarVisible(stack);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return SoulAlloyTool.isSecondaryEnabled(stack)
            ? 0x00FFFFF
            : super.getItemBarColor(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.addAll(SoulAlloyTool.getTooltip(stack));
    }
}
