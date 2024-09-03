package com.glisco.conjuring.entities;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.items.soul_alloy_tools.BlockCrawler;
import com.glisco.conjuring.items.soul_alloy_tools.SoulAlloyScythe;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class SoulHarvesterEntity extends SoulEntity {

    private int maxBlocks = 8;
    private static final TrackedData<ItemStack> STACK;
    private static final KeyedEndec<ItemStack> SCYTHE_STACK = MinecraftEndecs.ITEM_STACK.keyed("scythe_stack", ItemStack.EMPTY);

    static {
        STACK = DataTracker.registerData(SoulHarvesterEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    }

    public SoulHarvesterEntity(World world, LivingEntity owner) {
        super(Conjuring.SOUL_HARVESTER, world);
        setOwner(owner);
    }

    public SoulHarvesterEntity(EntityType<SoulHarvesterEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(STACK, ItemStack.EMPTY);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        tag.put(SerializationContext.attributes(RegistriesAttribute.of(this.getRegistryManager())), SCYTHE_STACK, this.getDataTracker().get(STACK));
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        this.setItem(tag.get(SerializationContext.attributes(RegistriesAttribute.of(this.getRegistryManager())), SCYTHE_STACK));
    }

    public void setItem(ItemStack stack) {
        this.getDataTracker().set(STACK, stack);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().getBlockState(getBlockPos()).getBlock() instanceof CropBlock) {
            this.onBlockHit(new BlockHitResult(getPos(), Direction.UP, getBlockPos(), true));
        }
    }

    @Override
    public void setVelocity(Entity user, float pitch, float yaw, float roll, float modifierZ, float modifierXYZ) {
        super.setVelocity(user, pitch, yaw, roll, modifierZ, modifierXYZ);
        this.setVelocity(this.getVelocity().multiply(0.65f));
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (this.getOwner() == null) return;

        BlockPos pos = blockHitResult.getBlockPos();

        var state = getWorld().getBlockState(pos);
        var predicate = SoulAlloyScythe.getCrawlPredicate(getDataTracker().get(STACK));

        if (state.getBlock() instanceof CropBlock && predicate.apply(state.getBlock(), state)) {
            BlockCrawler.crawl(getWorld(), pos, getDataTracker().get(STACK), this.getOwner().getUuid(), maxBlocks, predicate);
        } else {
            state = getWorld().getBlockState(pos.up());
            if (state.getBlock() instanceof CropBlock && predicate.apply(state.getBlock(), state)) {
                BlockCrawler.crawl(getWorld(), pos.up(), getDataTracker().get(STACK), this.getOwner().getUuid(), maxBlocks, predicate);
            }
        }

        this.remove(RemovalReason.KILLED);
    }

    public void setMaxBlocks(int maxBlocks) {
        this.maxBlocks = maxBlocks;
    }

}
