package com.glisco.conjuring.entities;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.items.soul_alloy_tools.BlockCrawler;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
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
import net.minecraft.world.World;

public class SoulDiggerEntity extends SoulEntity {

    private static final TrackedData<ItemStack> STACK;
    private static final KeyedEndec<ItemStack> SHOVEL_STACK = MinecraftEndecs.ITEM_STACK.keyed("shovel_stack", ItemStack.EMPTY);

    static {
        STACK = DataTracker.registerData(SoulDiggerEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    }

    public SoulDiggerEntity(World world, LivingEntity owner) {
        super(Conjuring.SOUL_DIGGER, world);
        setOwner(owner);
    }

    public SoulDiggerEntity(EntityType<SoulDiggerEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(STACK, ItemStack.EMPTY);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        tag.put(SerializationContext.attributes(RegistriesAttribute.of(this.getRegistryManager())), SHOVEL_STACK, this.getDataTracker().get(STACK));
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        this.setItem(tag.get(SerializationContext.attributes(RegistriesAttribute.of(this.getRegistryManager())), SHOVEL_STACK));
    }

    public void setItem(ItemStack stack) {
        this.getDataTracker().set(STACK, stack);
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

        if (getWorld().getBlockState(pos).isIn(ConventionalBlockTags.ORES)) {
            BlockCrawler.crawl(getWorld(), pos, getDataTracker().get(STACK), this.getOwner().getUuid(), Conjuring.CONFIG.tools_config.pickaxe_veinmine_max_blocks());
        }

        this.remove(RemovalReason.KILLED);
    }

}
