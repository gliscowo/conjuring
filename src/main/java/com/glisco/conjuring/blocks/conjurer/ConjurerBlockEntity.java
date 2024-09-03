package com.glisco.conjuring.blocks.conjurer;

import com.glisco.conjuring.blocks.ConjuringBlocks;
import com.glisco.conjuring.util.ConjurerScreenHandler;
import io.wispforest.owo.util.ImplementedInventory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ConjurerBlockEntity extends BlockEntity implements ImplementedInventory, NamedScreenHandlerFactory, SidedInventory {

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(5, ItemStack.EMPTY);

    private final ConjurerLogic logic = new ConjurerLogic() {
        @Override
        public void sendStatus(World world, BlockPos pos, int i) {
            ConjurerBlockEntity.this.world.addSyncedBlockEvent(ConjurerBlockEntity.this.pos, ConjuringBlocks.CONJURER, i, 0);
        }

        public World getWorld() {
            return ConjurerBlockEntity.this.world;
        }

        public BlockPos getPos() {
            return ConjurerBlockEntity.this.pos;
        }
    };

    public ConjurerBlockEntity(BlockPos pos, BlockState state) {
        super(ConjuringBlocks.Entities.CONJURER, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ConjurerBlockEntity conjurer) {
        if (world.isClient) {
            conjurer.getLogic().clientTick(world, pos);
        } else {
            conjurer.getLogic().serverTick((ServerWorld) world, pos);
        }
    }

    public ConjurerLogic getLogic() {
        return logic;
    }

    @Override
    public void markDirty() {
        ConjurerHelper.updateConjurerProperties(this);
        super.markDirty();
    }

    public boolean isActive() {
        return logic.isActive();
    }

    public void setActive(boolean active) {
        this.logic.setActive(active);

        if (this.world == null) return;
        this.world.setBlockState(pos, this.world.getBlockState(pos).with(ConjurerBlock.ACTIVE, active));
    }

    public void setRequiresPlayer(boolean requiresPlayer) {
        this.logic.setRequiresPlayer(requiresPlayer);
    }


    //NBT Logic
    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(tag, registries);
        Inventories.readNbt(tag, items, registries);
        this.logic.readNbt(world, pos, tag);
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(tag, registries);
        Inventories.writeNbt(tag, items, registries);
        this.logic.writeNbt(tag);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        var tag = new NbtCompound();
        this.writeNbt(tag, registries);
        tag.remove("SpawnPotentials");
        tag.remove("Items");
        return tag;
    }

    //Interface Logic
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ConjurerScreenHandler(syncId, playerInventory, this);
    }


    //Inventory Logic
    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    @Override
    public Text getDisplayName() {
        return Text.empty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[0];
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }
}
