package com.glisco.conjuring_testmod;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class TestInteractionHelper {

    public final TestContext ctx;
    private final ServerPlayerEntity player;

    public TestInteractionHelper(TestContext ctx) {
        this.ctx = ctx;
        this.player = FakePlayer.get(this.ctx.getWorld(), new GameProfile(UUID.randomUUID(), "test-mock-player"));
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public void setHandItem(ItemStack stack) {
        this.player.setStackInHand(Hand.MAIN_HAND, stack);
    }

    public void clickBlock(BlockPos relativePos, boolean sneaking) {
        this.player.setSneaking(sneaking);
        this.ctx.getBlockState(relativePos).onUse(this.ctx.getWorld(), this.player, createHitResult(relativePos));
    }

    public void interactItemAtBlock(BlockPos relativePos, boolean sneaking) {
        this.player.setSneaking(sneaking);
        this.player.getStackInHand(Hand.MAIN_HAND).useOnBlock(new ItemUsageContext(this.player, Hand.MAIN_HAND, createHitResult(relativePos)));
    }

    public void useItem(boolean sneaking) {
        this.player.setSneaking(sneaking);
        this.player.getStackInHand(Hand.MAIN_HAND).use(this.ctx.getWorld(), this.player, Hand.MAIN_HAND);
    }

    public BlockHitResult createHitResult(BlockPos relativePos) {
        var absolutePos = this.ctx.getAbsolutePos(relativePos);
        return new BlockHitResult(Vec3d.ofCenter(absolutePos), Direction.NORTH, absolutePos, true);
    }

}
