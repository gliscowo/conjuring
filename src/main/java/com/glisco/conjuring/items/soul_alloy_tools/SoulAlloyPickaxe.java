package com.glisco.conjuring.items.soul_alloy_tools;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.entities.SoulDiggerEntity;
import com.glisco.conjuring.items.ConjuringItems;
import io.wispforest.owo.itemgroup.OwoItemSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SoulAlloyPickaxe extends PickaxeItem implements SoulAlloyTool {

    public SoulAlloyPickaxe() {
        super(SoulAlloyToolMaterial.INSTANCE, new OwoItemSettings().group(Conjuring.CONJURING_GROUP).rarity(Rarity.UNCOMMON)
                .attributeModifiers(createAttributeModifiers(SoulAlloyToolMaterial.INSTANCE, 1, -2.8f)));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!SoulAlloyTool.isSecondaryEnabled(user.getStackInHand(hand))) return TypedActionResult.pass(user.getStackInHand(hand));

        if (!world.isClient()) {
            SoulDiggerEntity digger = new SoulDiggerEntity(world, user);
            digger.refreshPositionAndAngles(user.getX(), user.getEyeY(), user.getZ(), 0, 0);
            digger.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 1.5f, 1);

            digger.setItem(user.getStackInHand(hand));

            world.spawnEntity(digger);

            user.getItemCooldownManager().set(ConjuringItems.SOUL_ALLOY_PICKAXE, Conjuring.CONFIG.tools_config.pickaxe_secondary_cooldown());
            user.getStackInHand(hand).damage(Conjuring.CONFIG.tools_config.pickaxe_secondary_durability_cost(), user, LivingEntity.getSlotForHand(hand));
        }

        return TypedActionResult.success(user.getStackInHand(hand));
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

    @Override
    public boolean canAoeDig() {
        return true;
    }
}
