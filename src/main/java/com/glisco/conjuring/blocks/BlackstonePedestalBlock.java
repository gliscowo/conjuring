package com.glisco.conjuring.blocks;

import com.mojang.serialization.MapCodec;
import io.wispforest.owo.ops.ItemOps;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BlackstonePedestalBlock extends BlockWithEntity {

    private static final VoxelShape BASE1 = Block.createCuboidShape(0, 0, 0, 16, 2, 16);
    private static final VoxelShape BASE2 = Block.createCuboidShape(2, 2, 2, 14, 4, 14);
    private static final VoxelShape PILLAR = Block.createCuboidShape(4, 4, 4, 12, 17, 12);

    private static final VoxelShape PLATE_SOUTH = Block.createCuboidShape(4, 12, 12, 12, 16, 13);
    private static final VoxelShape PLATE_NORTH = Block.createCuboidShape(4, 12, 3, 12, 16, 4);
    private static final VoxelShape PLATE_WEST = Block.createCuboidShape(12, 12, 4, 13, 16, 12);
    private static final VoxelShape PLATE_EAST = Block.createCuboidShape(3, 12, 4, 4, 16, 12);

    private static final VoxelShape ARM_NW = Block.createCuboidShape(12, 11, 2, 14, 20, 4);
    private static final VoxelShape ARM_SW = Block.createCuboidShape(12, 11, 12, 14, 20, 14);
    private static final VoxelShape ARM_SE = Block.createCuboidShape(2, 11, 12, 4, 20, 14);
    private static final VoxelShape ARM_NE = Block.createCuboidShape(2, 11, 2, 4, 20, 4);

    private static final VoxelShape SHAPE = VoxelShapes.union(BASE1, BASE2, PILLAR, PLATE_SOUTH, PLATE_NORTH, PLATE_WEST, PLATE_EAST, ARM_NW, ARM_SW, ARM_SE, ARM_NE);

    //Construction stuff

    public BlackstonePedestalBlock() {
        super(Settings.copy(Blocks.BLACKSTONE).nonOpaque());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BlackstonePedestalBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    //Actual Logic


    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        BlackstonePedestalBlockEntity pedestal = (BlackstonePedestalBlockEntity) world.getBlockEntity(pos);
        if (pedestal.isActive()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        final ItemStack playerStack = player.getStackInHand(hand);
        final ItemStack pedestalItem = pedestal.getItem();

        if (pedestalItem.isEmpty()) {
            if (world.isClient) return ItemActionResult.SUCCESS;

            pedestal.setItem(ItemOps.singleCopy(playerStack));
            ItemOps.decrementPlayerHandItem(player, hand);
        } else if (!world.isClient) {
            if (ItemOps.canStack(playerStack, pedestalItem)) {
                playerStack.increment(1);
            } else {
                ItemScatterer.spawn(world, pos.getX(), pos.getY() + 1f, pos.getZ(), pedestalItem);
            }

            pedestal.setItem(ItemStack.EMPTY);
        }

        return ItemActionResult.SUCCESS;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlackstonePedestalBlockEntity pedestal = (BlackstonePedestalBlockEntity) world.getBlockEntity(pos);
        if (pedestal.isActive()) return ActionResult.PASS;

        final ItemStack pedestalItem = pedestal.getItem();

        if (!pedestalItem.isEmpty()) {
            player.setStackInHand(Hand.MAIN_HAND, pedestalItem.copy());
            pedestal.setItem(ItemStack.EMPTY);

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {

            if (world.getBlockEntity(pos) instanceof BlackstonePedestalBlockEntity pedestalEntity) {
                if (!pedestalEntity.getItem().isEmpty()) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), pedestalEntity.getItem());
                }

                if (pedestalEntity.isLinked()) {
                    if (world.getBlockEntity(pedestalEntity.getLinkedFunnel()) instanceof RitualCore core) {
                        core.removePedestal(pos, pedestalEntity.isActive());
                    }
                }
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }
}
