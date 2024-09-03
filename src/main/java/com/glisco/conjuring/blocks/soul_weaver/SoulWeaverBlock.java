package com.glisco.conjuring.blocks.soul_weaver;

import com.glisco.conjuring.blocks.BlackstonePedestalBlockEntity;
import com.glisco.conjuring.blocks.ConjuringBlocks;
import com.glisco.conjuring.items.ConjuringItems;
import com.glisco.conjuring.items.ConjuringScepter;
import com.mojang.serialization.MapCodec;
import io.wispforest.owo.ops.ItemOps;
import io.wispforest.owo.particles.ClientParticles;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class SoulWeaverBlock extends BlockWithEntity {

    private static final VoxelShape SHAPE = Stream.of(
            Block.createCuboidShape(2, 0, 2, 14, 2, 14),
            Block.createCuboidShape(4, 2, 4, 12, 4, 12),
            Block.createCuboidShape(10, 4, 4, 12, 9, 6),
            Block.createCuboidShape(10, 4, 10, 12, 9, 12),
            Block.createCuboidShape(4, 14, 4, 12, 16, 12),
            Block.createCuboidShape(5, 9, 5, 11, 14, 11),
            Block.createCuboidShape(4, 9, 11, 12, 12, 12),
            Block.createCuboidShape(4, 9, 4, 12, 12, 5),
            Block.createCuboidShape(11, 9, 5, 12, 12, 11),
            Block.createCuboidShape(4, 9, 5, 5, 12, 11),
            Block.createCuboidShape(4, 4, 4, 6, 9, 6),
            Block.createCuboidShape(4, 4, 10, 6, 9, 12)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    public SoulWeaverBlock() {
        super(Settings.copy(Blocks.BLACKSTONE).nonOpaque());
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SoulWeaverBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }


    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        SoulWeaverBlockEntity weaver = (SoulWeaverBlockEntity) world.getBlockEntity(pos);
        if (weaver.isRunning()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        final ItemStack playerStack = player.getStackInHand(hand);
        final ItemStack weaverItem = weaver.getItem();

        if (playerStack.getItem().equals(ConjuringItems.CONJURATION_ESSENCE) && !weaver.isLit()) {
            weaver.setLit(true);
            if (!ItemOps.emptyAwareDecrement(playerStack)) player.setStackInHand(hand, ItemStack.EMPTY);
            return ItemActionResult.SUCCESS;
        }

        if (playerStack.getItem() instanceof ConjuringScepter) {
            weaver.tryStartRitual(player);
            return ItemActionResult.SUCCESS;
        }

        if (weaverItem.isEmpty()) {
            weaver.setItem(ItemOps.singleCopy(playerStack));
            ItemOps.decrementPlayerHandItem(player, hand);
        } else if (!world.isClient) {
            if (ItemOps.canStack(playerStack, weaverItem)) {
                playerStack.increment(1);
            } else {
                ItemScatterer.spawn(world, pos.getX(), pos.getY() + 1f, pos.getZ(), weaverItem);
            }

            weaver.setItem(ItemStack.EMPTY);
        }

        return ItemActionResult.SUCCESS;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        SoulWeaverBlockEntity weaver = (SoulWeaverBlockEntity) world.getBlockEntity(pos);
        if (weaver.isRunning()) return ActionResult.PASS;

        final ItemStack weaverItem = weaver.getItem();

        if (!weaverItem.isEmpty()) {
            player.setStackInHand(Hand.MAIN_HAND, weaverItem);
            weaver.setItem(ItemStack.EMPTY);

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ConjuringBlocks.Entities.SOUL_WEAVER, SoulWeaverBlockEntity.TICKER);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SoulWeaverBlockEntity weaverEntity) {
                weaverEntity.onBroken();
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        SoulWeaverBlockEntity weaver = (SoulWeaverBlockEntity) world.getBlockEntity(pos);

        for (BlockPos p : weaver.getPedestalPositions()) {
            if (random.nextDouble() > 0.5f) continue;
            BlackstonePedestalBlockEntity pedestal = (BlackstonePedestalBlockEntity) world.getBlockEntity(p);
            if (pedestal == null) continue;
            if (pedestal.getLinkedFunnel() == null) continue;
            if (pedestal.getLinkedFunnel().compareTo(pos) != 0) return;

            ClientParticles.spawnEnchantParticles(world, Vec3d.of(p).add(0.5, 0.5, 0.5), Vec3d.of(pos).add(0.5, 1.5, 0.5), 0);
        }
    }
}
