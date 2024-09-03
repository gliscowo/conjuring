package com.glisco.conjuring.blocks;

import io.wispforest.owo.ops.ItemOps;
import io.wispforest.owo.ops.WorldOps;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlackstonePedestalBlockEntity extends BlockEntity {

    @NotNull
    private ItemStack renderedItem = ItemStack.EMPTY;
    private boolean active = false;
    private BlockPos linkedFunnel = null;

    public BlackstonePedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ConjuringBlocks.Entities.BLACKSTONE_PEDESTAL, pos, state);
    }

    //Data Logic
    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(tag, registries);

        ItemOps.store(registries, this.renderedItem, tag, "Item");

        if (linkedFunnel == null) {
            tag.putIntArray("LinkedFunnel", new int[0]);
        } else {
            tag.putIntArray("LinkedFunnel", new int[]{linkedFunnel.getX(), linkedFunnel.getY(), linkedFunnel.getZ()});
        }

        tag.putBoolean("Active", active);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        WorldOps.updateIfOnServer(world, this.getPos());
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(tag, registries);
        this.renderedItem = ItemOps.get(registries, tag, "Item");

        int[] funnelPos = tag.getIntArray("LinkedFunnel");
        if (funnelPos.length > 0) {
            this.linkedFunnel = new BlockPos(funnelPos[0], funnelPos[1], funnelPos[2]);
        }

        active = tag.getBoolean("Active");
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.markDirty();
    }

    public void setLinkedFunnel(BlockPos linkedFunnel) {
        this.linkedFunnel = linkedFunnel;
        this.markDirty();
    }

    public BlockPos getLinkedFunnel() {
        return linkedFunnel;
    }

    public boolean isLinked() {
        return linkedFunnel != null;
    }

    public void setItem(@NotNull ItemStack item) {
        this.renderedItem = item;
        this.markDirty();
    }

    @NotNull
    public ItemStack getItem() {
        return renderedItem;
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
        return tag;
    }
}
