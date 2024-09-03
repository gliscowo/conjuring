package com.glisco.conjuring.blocks.soulfire_forge;

import com.glisco.conjuring.blocks.ConjuringBlocks;
import com.glisco.conjuring.util.ConjuringParticleEvents;
import com.glisco.conjuring.util.SoulfireForgeScreenHandler;
import io.wispforest.owo.ops.ItemOps;
import io.wispforest.owo.particles.ClientParticles;
import io.wispforest.owo.util.ImplementedInventory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SoulfireForgeBlockEntity extends BlockEntity implements ImplementedInventory, SidedInventory, NamedScreenHandlerFactory {

    public static final BlockEntityTicker<SoulfireForgeBlockEntity> SERVER_TICKER = (world1, pos1, state, blockEntity) -> blockEntity.tickServer();
    public static final BlockEntityTicker<SoulfireForgeBlockEntity> CLIENT_TICKER = (world1, pos1, state, blockEntity) -> blockEntity.tickClient();

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(10, ItemStack.EMPTY);

    private final int[] SIDE_AND_TOP_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    private final int[] BOTTOM_SLOT = new int[]{9};

    private int progress;
    private int smeltTime;
    private int targetSmeltTime;
    private SoulfireForgeRecipe cachedRecipe;

    public SoulfireForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ConjuringBlocks.Entities.SOULFIRE_FORGE, pos, state);
    }

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return progress;
        }

        @Override
        public void set(int index, int value) {
            progress = value;
        }

        @Override
        public int size() {
            return 1;
        }
    };

    public void tickClient() {
        if (!getCachedState().get(SoulfireForgeBlock.BURNING)) return;

        final Vec3d loc = Vec3d.of(pos);
        ClientParticles.setParticleCount(4);
        ClientParticles.spawnPrecise(ParticleTypes.SMOKE, world, loc.add(.5, .6, .5), .3, 0, .3);
    }

    //Tick Logic
    public void tickServer() {
        if (!getCachedState().get(SoulfireForgeBlock.BURNING) || !updateCachedRecipe()) return;

        targetSmeltTime = cachedRecipe.getSmeltTime();

        if (smeltTime == targetSmeltTime) {
            this.decrementCraftingItems();
            this.incrementOutput(cachedRecipe.getResult(null));

            this.progress = 0;
            this.smeltTime = 0;

            world.setBlockState(pos, world.getBlockState(pos).with(SoulfireForgeBlock.BURNING, false));
            this.markDirty();
        } else {
            this.smeltTime++;
            this.progress = Math.round(((float) smeltTime / (float) targetSmeltTime) * 32);

            if (world.random.nextDouble() < .05) {
                ConjuringParticleEvents.SOULFIRE_FORGE_SOULS.spawn(world, Vec3d.of(pos), null);
            }

        }

        world.updateComparators(pos, ConjuringBlocks.SOULFIRE_FORGE);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean updateCachedRecipe() {
        var recipe = world.getRecipeManager().getFirstMatch(SoulfireForgeRecipe.Type.INSTANCE, new SoulfireForgeRecipe.Input(this), world);
        if (recipe.isPresent()) {
            this.cachedRecipe = recipe.get().value();
            return ItemOps.canStack(this.getItems().get(9), recipe.get().value().getResult(null));
        } else {
            this.progress = 0;
            this.smeltTime = 0;
            return false;
        }
    }

    //Data Logic
    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(tag, registries);
        Inventories.writeNbt(tag, items, registries);
        tag.putInt("Progress", progress);
        tag.putInt("SmeltTime", smeltTime);
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(tag, registries);
        Inventories.readNbt(tag, items, registries);
        this.progress = tag.getInt("Progress");
        this.smeltTime = tag.getInt("SmeltTime");
    }

    public boolean isRunning() {
        return smeltTime > 0;
    }

    public int getProgress() {
        return progress;
    }

    public void finishInstantly() {
        if (this.world.isClient()) return;

        this.smeltTime = targetSmeltTime;
        ConjuringParticleEvents.CONJURER_SUMMON.spawn(world, Vec3d.of(this.pos), null);
    }

    //Inventory Logic
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new SoulfireForgeScreenHandler(syncId, inv, this, properties);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return side == Direction.DOWN ? BOTTOM_SLOT : SIDE_AND_TOP_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return slot != 9 && items.get(slot).isEmpty();
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == 9;
    }

    private void decrementCraftingItems() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = items.get(i);
            if (!ItemOps.emptyAwareDecrement(stack)) items.set(i, ItemStack.EMPTY);
        }
    }

    private void incrementOutput(ItemStack craftingResult) {
        if (items.get(9).isEmpty()) {
            items.set(9, craftingResult);
        } else {
            items.get(9).increment(craftingResult.getCount());
        }
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("conjuring.gui.soulfire_forge");
    }
}
