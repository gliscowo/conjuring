package com.glisco.conjuring.items;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.blocks.soulfire_forge.SoulfireForgeBlockEntity;
import com.glisco.conjuring.entities.SoulProjectileEntity;
import io.wispforest.owo.itemgroup.OwoItemSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class SuperiorConjuringScepter extends ConjuringScepter {

    public SuperiorConjuringScepter(Settings settings) {
        super(settings);
    }

    public SuperiorConjuringScepter() {
        this(new OwoItemSettings().group(Conjuring.CONJURING_GROUP).maxCount(1).rarity(Rarity.RARE));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getWorld().getBlockEntity(context.getBlockPos()) instanceof SoulfireForgeBlockEntity forge)) {
            return ConjuringScepter.onBlockUse(context);
        }

        if (!forge.isRunning()) return ActionResult.PASS;

        forge.finishInstantly();
        context.getPlayer().getItemCooldownManager().set(ConjuringItems.SUPERIOR_CONJURING_SCEPTER, 30);

        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (user.isSneaking()) {
            return ConjuringScepter.onUse(user, hand);
        }

        user.setCurrentHand(hand);
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity)) return;
        if (72000 - remainingUseTicks < 20) return;

        if (!world.isClient()) {
            SoulProjectileEntity projectile = new SoulProjectileEntity(world, user);
            projectile.refreshPositionAndAngles(user.getX(), user.getEyeY(), user.getZ(), 0, 0);
            projectile.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 1.5f, 1);
            world.spawnEntity(projectile);

            world.playSound(null, user.getBlockPos(), SoundEvents.PARTICLE_SOUL_ESCAPE.value(), SoundCategory.PLAYERS, 2, 1);
        }

        if (!((PlayerEntity) user).getAbilities().creativeMode) {
            ((PlayerEntity) user).getItemCooldownManager().set(ConjuringItems.SUPERIOR_CONJURING_SCEPTER, 100);
        }
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }
}
