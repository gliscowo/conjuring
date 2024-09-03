package com.glisco.conjuring.entities;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.items.ConjuringItems;
import com.glisco.conjuring.util.ConjuringParticleEvents;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;

public class SoulProjectileEntity extends SoulEntity {

    private static final RegistryKey<DamageType> DAMAGE_TYPE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Conjuring.id("soul_projectile"));

    private static final KeyedEndec<Float> BASE_DAMAGE = Endec.FLOAT.keyed("base_damage", 0f);
    private static final KeyedEndec<ItemStack> SWORD_STACK = MinecraftEndecs.ITEM_STACK.keyed("sword_stack", ItemStack.EMPTY);

    private float baseDamage;
    private ItemStack swordStack = ItemStack.EMPTY;
    private static final HashMap<SoulProjectileEntity, Entity> TARGET_ENTITIES = new HashMap<>();
    private final TargetPredicate UNIQUE_CLOSEST;

    public SoulProjectileEntity(World world, LivingEntity owner) {
        super(Conjuring.SOUL_PROJECTILE, world);
        setOwner(owner);
        UNIQUE_CLOSEST = TargetPredicate.createAttackable().setBaseMaxDistance(8).setPredicate(livingEntity -> livingEntity.isAlive() && (!TARGET_ENTITIES.containsValue(livingEntity) || TARGET_ENTITIES.get(this) == livingEntity));
    }

    public SoulProjectileEntity(EntityType<SoulProjectileEntity> entityType, World world) {
        super(entityType, world);
        UNIQUE_CLOSEST = TargetPredicate.createAttackable().setBaseMaxDistance(8).setPredicate(livingEntity -> livingEntity.isAlive() && (!TARGET_ENTITIES.containsValue(livingEntity) || TARGET_ENTITIES.get(this) == livingEntity));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        tag.put(BASE_DAMAGE, this.baseDamage);
        tag.put(SerializationContext.attributes(RegistriesAttribute.of(this.getWorld().getRegistryManager())), SWORD_STACK, this.swordStack);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        this.baseDamage = tag.get(BASE_DAMAGE);
        this.swordStack = tag.get(SerializationContext.attributes(RegistriesAttribute.of(this.getWorld().getRegistryManager())), SWORD_STACK);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (this.getWorld().isClient) return;

        if (!(entityHitResult.getEntity() instanceof LivingEntity target) || entityHitResult.getEntity() instanceof EnderDragonEntity || entityHitResult.getEntity() instanceof WitherEntity || entityHitResult.getEntity() instanceof PlayerEntity)
            return;
        this.remove(RemovalReason.KILLED);

        var source = createDamageSource();
        var damage = EnchantmentHelper.getDamage(((ServerWorld) this.getWorld()), this.swordStack, target, source, this.baseDamage);

        target.damage(createDamageSource(), damage);

        if (!target.isAlive() && damage == 1.5f) {
            target.dropItem(ConjuringItems.CONJURATION_ESSENCE);
            ConjuringParticleEvents.EXTRACTION_RITUAL_FINISHED.spawn(getWorld(), Vec3d.of(entityHitResult.getEntity().getBlockPos()), true);

            if (!target.getWorld().isClient) {
                BlockPos pos = target.getBlockPos();
                World world = target.getWorld();

                world.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 0.5f, 0);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        TARGET_ENTITIES.remove(this);
    }

    @Override
    public void tick() {
        Entity closest = getWorld().getClosestEntity(MobEntity.class, UNIQUE_CLOSEST, null, getX(), getY(), getZ(), getBoundingBox().expand(3, 2, 3));
        if (closest == null && TARGET_ENTITIES.containsKey(this)) closest = TARGET_ENTITIES.get(this);
        if (closest != null) {
            Vec3d targetVector = closest.getPos().add(0, closest.getHeight() * 0.5, 0).subtract(getPos());
            setVelocity(targetVector.multiply(0.25f));
            TARGET_ENTITIES.put(this, closest);
        }

        super.tick();
    }

    public void setBaseDamage(float baseDamage) {
        this.baseDamage = baseDamage;
    }

    public void setSwordStack(ItemStack swordStack) {
        this.swordStack = swordStack;
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        this.remove(RemovalReason.KILLED);
    }

    public DamageSource createDamageSource() {
        return new DamageSource(this.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).getEntry(DAMAGE_TYPE).get(), this.getOwner());
    }

}
