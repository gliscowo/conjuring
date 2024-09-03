package com.glisco.conjuring.blocks.soulfire_forge;

import com.glisco.conjuring.blocks.ConjuringBlocks;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToIntFunction;
import java.util.stream.Stream;

public class SoulfireForgeBlock extends BlockWithEntity {

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty BURNING = BooleanProperty.of("burning");

    private static final VoxelShape BASE = Stream.of(
            Block.createCuboidShape(0, 0, 0, 16, 3, 16),
            Block.createCuboidShape(3, 3, 3, 13, 6, 13),
            Block.createCuboidShape(2, 6, 2, 14, 9, 14)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape N = Stream.of(
            Block.createCuboidShape(3, 9, 1, 13, 11, 3),
            Block.createCuboidShape(3, 9, 3, 13, 10, 13),
            Block.createCuboidShape(12, 10, 7, 13, 16, 9),
            Block.createCuboidShape(13, 9, 1, 15, 16, 13),
            Block.createCuboidShape(7, 10, 12, 9, 16, 13),
            Block.createCuboidShape(3, 10, 7, 4, 16, 9),
            Block.createCuboidShape(1, 9, 13, 15, 16, 15),
            Block.createCuboidShape(1, 9, 1, 3, 16, 13),
            Block.createCuboidShape(2, 16, 4, 14, 19, 14),
            Block.createCuboidShape(4, 19, 5, 12, 20, 11)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape S = Stream.of(
            Block.createCuboidShape(3, 9, 13, 13, 11, 15),
            Block.createCuboidShape(3, 9, 3, 13, 10, 13),
            Block.createCuboidShape(3, 10, 7, 4, 16, 9),
            Block.createCuboidShape(1, 9, 3, 3, 16, 15),
            Block.createCuboidShape(7, 10, 3, 9, 16, 4),
            Block.createCuboidShape(12, 10, 7, 13, 16, 9),
            Block.createCuboidShape(1, 9, 1, 15, 16, 3),
            Block.createCuboidShape(13, 9, 3, 15, 16, 15),
            Block.createCuboidShape(2, 16, 2, 14, 19, 12),
            Block.createCuboidShape(4, 19, 5, 12, 20, 11)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape W = Stream.of(
            Block.createCuboidShape(1, 9, 3, 3, 11, 13),
            Block.createCuboidShape(3, 9, 3, 13, 10, 13),
            Block.createCuboidShape(7, 10, 3, 9, 16, 4),
            Block.createCuboidShape(1, 9, 1, 13, 16, 3),
            Block.createCuboidShape(12, 10, 7, 13, 16, 9),
            Block.createCuboidShape(7, 10, 12, 9, 16, 13),
            Block.createCuboidShape(13, 9, 1, 15, 16, 15),
            Block.createCuboidShape(1, 9, 13, 13, 16, 15),
            Block.createCuboidShape(4, 16, 2, 14, 19, 14),
            Block.createCuboidShape(5, 19, 4, 11, 20, 12)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape E = Stream.of(
            Block.createCuboidShape(13, 9, 3, 15, 11, 13),
            Block.createCuboidShape(3, 9, 3, 13, 10, 13),
            Block.createCuboidShape(7, 10, 12, 9, 16, 13),
            Block.createCuboidShape(3, 9, 13, 15, 16, 15),
            Block.createCuboidShape(3, 10, 7, 4, 16, 9),
            Block.createCuboidShape(7, 10, 3, 9, 16, 4),
            Block.createCuboidShape(1, 9, 1, 3, 16, 15),
            Block.createCuboidShape(3, 9, 1, 15, 16, 3),
            Block.createCuboidShape(2, 16, 2, 12, 19, 14),
            Block.createCuboidShape(5, 19, 4, 11, 20, 12)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape SHAPE_N = VoxelShapes.union(BASE, N);
    private static final VoxelShape SHAPE_S = VoxelShapes.union(BASE, S);
    private static final VoxelShape SHAPE_W = VoxelShapes.union(BASE, W);
    private static final VoxelShape SHAPE_E = VoxelShapes.union(BASE, E);

    //Construction stuff
    public SoulfireForgeBlock() {
        super(FabricBlockSettings.create().requiresTool().strength(5.0F, 1200.0F).sounds(BlockSoundGroup.STONE).nonOpaque().luminance(getLightLevel()));
        setDefaultState(getStateManager().getDefaultState().with(BURNING, false));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SoulfireForgeBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case EAST -> SHAPE_E;
            case WEST -> SHAPE_W;
            case SOUTH -> SHAPE_S;
            default -> SHAPE_N;
        };
    }

    private static ToIntFunction<BlockState> getLightLevel() {
        return (blockState) -> blockState.get(BURNING) ? 13 : 0;
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(BURNING);
        builder.add(FACING);
    }

    //Actual Logic


    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.getItem().equals(Items.FLINT_AND_STEEL) && !world.getBlockState(pos).get(BURNING)) {
            if (world.isClient()) return ItemActionResult.SUCCESS;

            world.setBlockState(pos, world.getBlockState(pos).with(SoulfireForgeBlock.BURNING, true));
            stack.damage(1, player, LivingEntity.getSlotForHand(hand));

            return ItemActionResult.SUCCESS;
        }

        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);
        if (screenHandlerFactory != null) {
            player.openHandledScreen(screenHandlerFactory);
        }

        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ConjuringBlocks.Entities.SOULFIRE_FORGE, world.isClient ? SoulfireForgeBlockEntity.CLIENT_TICKER : SoulfireForgeBlockEntity.SERVER_TICKER);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof SoulfireForgeBlockEntity)) return 0;
        return Math.round(((SoulfireForgeBlockEntity) world.getBlockEntity(pos)).getProgress() * 0.46875f);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (world.getBlockEntity(pos) instanceof SoulfireForgeBlockEntity forge) ItemScatterer.spawn(world, pos, forge);
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

}
