package com.glisco.conjuring.blocks;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.items.ConjuringFocus;
import com.glisco.conjuring.items.ConjuringItems;
import com.glisco.conjuring.util.ConjuringParticleEvents;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.wispforest.owo.Owo;
import io.wispforest.owo.blockentity.LinearProcess;
import io.wispforest.owo.blockentity.LinearProcessExecutor;
import io.wispforest.owo.ops.ItemOps;
import io.wispforest.owo.ops.WorldOps;
import io.wispforest.owo.particles.ClientParticles;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@SuppressWarnings("ConstantConditions")
public class SoulFunnelBlockEntity extends BlockEntity implements RitualCore {

    public static final BlockEntityTicker<SoulFunnelBlockEntity> SERVER_TICKER = (world1, pos1, state, blockEntity) -> blockEntity.tickServer();
    public static final BlockEntityTicker<SoulFunnelBlockEntity> CLIENT_TICKER = (world1, pos1, state, blockEntity) -> blockEntity.tickClient();

    public static final LinearProcess<SoulFunnelBlockEntity> PROCESS = new LinearProcess<>(80);

    @NotNull
    private ItemStack item = ItemStack.EMPTY;
    private float itemHeight = 0;
    private int slownessCooldown = 0;

    private UUID ritualEntity = null;
    private float particleOffset = 0;
    private float ritualStability = 0.1f;

    private final List<BlockPos> pedestalPositions;

    private final LinearProcessExecutor<SoulFunnelBlockEntity> ritualExecutor;

    public SoulFunnelBlockEntity(BlockPos pos, BlockState state) {
        super(ConjuringBlocks.Entities.SOUL_FUNNEL, pos, state);
        pedestalPositions = new ArrayList<>();
        this.ritualExecutor = PROCESS.createExecutor(this);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        PROCESS.configureExecutor(ritualExecutor, world.isClient);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        WorldOps.updateIfOnServer(world, this.getPos());
    }

    //Data Logic
    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(tag, lookup);

        ItemOps.store(lookup, this.item, tag, "Item");

        tag.putInt("Cooldown", slownessCooldown);

        if (ritualExecutor.running()) {
            NbtCompound ritual = new NbtCompound();
            ritual.putUuid("Entity", ritualEntity);
            ritual.putFloat("ParticleOffset", particleOffset);
            ritual.putFloat("Stability", ritualStability);
            ritualExecutor.writeState(ritual);
            tag.put("Ritual", ritual);
        }

        savePedestals(tag, pedestalPositions);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        this.item = ItemOps.get(registryLookup, nbt, "Item");

        loadPedestals(nbt, pedestalPositions);

        slownessCooldown = nbt.getInt("Cooldown");

        if (nbt.contains("Ritual")) {
            NbtCompound ritual = nbt.getCompound("Ritual");
            ritualEntity = ritual.getUuid("Entity");
            particleOffset = ritual.getFloat("ParticleOffset");
            ritualStability = ritual.getFloat("Stability");
            ritualExecutor.readState(ritual);
        } else {
            ritualEntity = null;
            particleOffset = 0;
            ritualStability = 0.1f;
            ritualExecutor.readState(new NbtCompound());
        }
    }

    //Tick Logic
    public void tickClient() {
        itemHeight = itemHeight >= 100 ? 0 : itemHeight + 1;
        if (slownessCooldown > 0) slownessCooldown--;

        ritualExecutor.tick();
    }

    public void tickServer() {
        ritualExecutor.tick();

        if (slownessCooldown > 0) slownessCooldown--;

        if (slownessCooldown == 0 && this.getItem() != null) {
            if (world.getOtherEntities(null, new Box(pos)).isEmpty()) return;

            Entity e = world.getOtherEntities(null, new Box(pos)).get(0);
            if (e instanceof PlayerEntity || e instanceof EnderDragonEntity || e instanceof WitherEntity || !(e instanceof LivingEntity) || e.getCommandTags().contains("affected")) {
                return;
            }

            ((LivingEntity) e).addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 15 * 20, 20));
            slownessCooldown = 30 * 20;
            this.markDirty();
        }
    }

    //Actual Logic
    public void setItem(@NotNull ItemStack item) {
        this.item = item.copy();
        this.markDirty();
    }

    @NotNull
    public ItemStack getItem() {
        return item.copy();
    }

    public boolean tryStartRitual(PlayerEntity player) {

        if (item.isEmpty()) return false;

        if (world.getOtherEntities(player, Box.enclosing(pos, pos.add(1, 3, 1))).isEmpty()) return false;
        Entity e = world.getOtherEntities(player, Box.enclosing(pos, pos.add(1, 3, 1))).get(0);

        if (e instanceof ItemEntity item && item.getStack().isOf(ConjuringItems.DISTILLED_SPIRIT) && this.item != null) {
            if (world.isClient) return true;

            final MobEntity newEntity = (MobEntity) EntityType.loadEntityWithPassengers(this.item.getOrDefault(ConjuringFocus.ENTITY, NbtComponent.DEFAULT).getNbt(), world, Function.identity());
            if (newEntity == null) return false;

            final var health = newEntity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            health.setBaseValue(health.getBaseValue() * 1.5);
            newEntity.setHealth(newEntity.getMaxHealth());

            final var attackDamage = newEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if (attackDamage != null) attackDamage.setBaseValue(attackDamage.getBaseValue() * 1.5);

            newEntity.updatePosition(pos.getX(), pos.getY() + 1, pos.getZ());
            world.spawnEntity(newEntity);

            ConjuringParticleEvents.EXTRACTION_RITUAL_FINISHED.spawn(world, Vec3d.of(pos.add(0, 1, 0)), false);
            world.playSound(null, pos, SoundEvents.ENTITY_WITHER_HURT, SoundCategory.BLOCKS, 1, 0);

            ItemScatterer.spawn(world, pos.getX(), pos.getY() + 0.75, pos.getZ(), new ItemStack(this.item.getItem()));
            this.item = ItemStack.EMPTY;

            item.discard();
            markDirty();
            return true;
        }

        if (!(e instanceof MobEntity) || Conjuring.CONFIG.conjurer_config.conjurer_blacklist().contains(Registries.ENTITY_TYPE.getId(e.getType()).toString())) {
            return false;
        }

        if (item.contains(ConjuringFocus.ENTITY)) return false;

        if (!world.isClient()) {
            ritualExecutor.begin();
            this.ritualEntity = e.getUuid();
            this.markDirty();

            Conjuring.EXTRACTION_RITUAL_CRITERION.trigger((ServerPlayerEntity) player);
        }

        return true;
    }

    public boolean isRitualRunning() {
        return ritualExecutor.running();
    }

    public float getItemHeight() {
        return (float) Math.sin(2 * Math.PI * itemHeight / 100) / 25f;
    }

    public boolean onCooldown() {
        return slownessCooldown > 0;
    }

    private void disablePedestals() {
        for (BlockPos pos : pedestalPositions) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (!(blockEntity instanceof BlackstonePedestalBlockEntity)) continue;

            ((BlackstonePedestalBlockEntity) blockEntity).setActive(false);
            ((BlackstonePedestalBlockEntity) blockEntity).setItem(ItemStack.EMPTY);
        }
    }

    public boolean linkPedestal(BlockPos pedestal) {
        if (pedestalPositions.size() >= 4) return false;

        if (!pedestalPositions.contains(pedestal)) pedestalPositions.add(pedestal);
        if (!world.isClient) {
            ConjuringParticleEvents.LINK_SOUL_FUNNEL.spawn(world, Vec3d.of(pos), pedestal.subtract(pos));
        }

        this.markDirty();
        return true;
    }

    public boolean removePedestal(BlockPos pedestal, boolean pedestalActive) {
        boolean returnValue = pedestalPositions.remove(pedestal);
        this.markDirty();

        BlockPos offset = BlockPos.ofFloored(Vec3d.of(pedestal.subtract(pos)).normalize());
        ConjuringParticleEvents.PEDESTAL_REMOVED.spawn(world, Vec3d.of(pos), Direction.fromVector(offset.getX(), offset.getY(), offset.getZ()));

        if (pedestalActive) {
            ritualExecutor.cancel();
            this.markDirty();
        }

        return returnValue;
    }

    public List<BlockPos> getPedestalPositions() {
        return pedestalPositions;
    }

    public void calculateStability() {
        if (world.getBiome(pos).getKey().orElse(null) == BiomeKeys.SOUL_SAND_VALLEY) {ritualStability += .1f;}

        var drops = extractDrops(world.getServer().getReloadableRegistries().getLootTable(((MobEntity) ((ServerWorld) world).getEntity(ritualEntity)).getLootTable()));

        for (var pedestalPos : pedestalPositions) {
            if (!(world.getBlockEntity(pedestalPos) instanceof BlackstonePedestalBlockEntity pedestal)) continue;

            if (pedestal.getItem().isEmpty()) continue;
            if (!drops.contains(pedestal.getItem().getItem())) continue;

            ritualStability += 0.2f;
            pedestal.setActive(true);
        }
        this.markDirty();
    }

    public void onBroken() {
        if (!item.isEmpty()) ItemScatterer.spawn(world, pos.getX(), pos.getY() + 1.25, pos.getZ(), item);

        for (BlockPos pos : pedestalPositions) {
            if (!(world.getBlockEntity(pos) instanceof BlackstonePedestalBlockEntity)) continue;
            ((BlackstonePedestalBlockEntity) world.getBlockEntity(pos)).setLinkedFunnel(null);
        }

        ritualExecutor.cancel();
    }

    private List<Item> extractDrops(LootTable table) {
        var tableObject = (JsonObject) LootTable.CODEC.encodeStart(this.world.getRegistryManager().getOps(JsonOps.INSTANCE), table).result().get();
        var extractedDrops = new ArrayList<Item>();

        try {
            for (JsonElement poolElement : tableObject.get("pools").getAsJsonArray()) {
                JsonArray entries = poolElement.getAsJsonObject().get("entries").getAsJsonArray();

                for (JsonElement entryElement : entries) {
                    JsonObject entryObject = entryElement.getAsJsonObject();
                    if (!"minecraft:item".equals(JsonHelper.getString(entryObject, "type"))) continue;

                    extractedDrops.add(Registries.ITEM.get(Identifier.of(JsonHelper.getString(entryObject, "name"))));
                }
            }

            return extractedDrops;
        } catch (Exception e) {
            if (!Owo.DEBUG) return new ArrayList<>();
            throw new RuntimeException("Unable to parse loot table", e);
        }
    }

    private boolean verifyTargetedEntity() {
        if (world.isClient) return true;
        MobEntity targetEntity = (MobEntity) ((ServerWorld) world).getEntity(ritualEntity);
        return targetEntity != null && !targetEntity.isDead();
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        var tag = new NbtCompound();
        this.writeNbt(tag, registryLookup);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    // Main Tick Logic
    static {
        PROCESS.runConditionally(executor -> executor.getTarget().verifyTargetedEntity());

        PROCESS.addClientEvent(1, (executor, funnel) -> {
            funnel.world.playSound(funnel.getPos().getX(), funnel.getPos().getY(), funnel.getPos().getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1, 1, false);
        });

        PROCESS.addClientStep(20, 60, (executor, funnel) -> {
            final var world = funnel.getWorld();

            for (BlockPos pos : funnel.pedestalPositions) {
                if (!(world.getBlockEntity(pos) instanceof BlackstonePedestalBlockEntity pedestal) || !pedestal.isActive()) {
                    continue;
                }

                BlockPos particleOrigin = pos.add(0, 1, 0);
                BlockPos particleVelocity = pos.subtract(funnel.pos);

                ParticleEffect particle = new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(pos));
                ClientParticles.setParticleCount(4);
                ClientParticles.spawnWithOffsetFromBlock(particle, world, particleOrigin, new Vec3d(0.5, 0.25, 0.5), 0.1);

                ClientParticles.setVelocity(new Vec3d(particleVelocity.getX() * -0.05, funnel.particleOffset * 0.075, particleVelocity.getZ() * -0.05));
                ClientParticles.spawnWithOffsetFromBlock(ParticleTypes.SOUL, world, particleOrigin, new Vec3d(0.5, 0.3, 0.5), 0.1);
            }

            ClientParticles.setParticleCount(5);
            ClientParticles.setVelocity(new Vec3d(0, -0.5, 0));
            ClientParticles.spawnWithOffsetFromBlock(ParticleTypes.SOUL_FIRE_FLAME, world, funnel.pos, new Vec3d(0.5, 1.75 + funnel.particleOffset, 0.5), 0.1);
        });

        PROCESS.addServerEvent(1, (executor, funnel) -> {
            var targetEntity = (MobEntity) ((ServerWorld) funnel.world).getEntity(funnel.ritualEntity);

            funnel.particleOffset = targetEntity.getHeight() / 2;
            funnel.markDirty();

            targetEntity.teleport(funnel.pos.getX() + 0.5f, targetEntity.getY(), funnel.pos.getZ() + 0.5f, false);
            targetEntity.setVelocity(0, 0.075f, 0);
            targetEntity.setNoGravity(true);
            funnel.calculateStability();
        });

        PROCESS.addServerEvent(20, (executor, funnel) -> {
            var targetEntity = (MobEntity) ((ServerWorld) funnel.world).getEntity(funnel.ritualEntity);

            targetEntity.setVelocity(0, 0, 0);
            targetEntity.setAiDisabled(true);

            final Vec3d entityTargetPos = Vec3d.of(funnel.pos).add(0.5, 1.85, 0.5);
            targetEntity.setPos(entityTargetPos.x, entityTargetPos.y, entityTargetPos.z);
        });

        PROCESS.addServerStep(20, 60, (executor, funnel) -> {
            if (executor.getProcessTick() % 10 != 0) return;
            ((ServerWorld) funnel.world).getEntity(funnel.ritualEntity).damage(funnel.world.getDamageSources().outOfWorld(), 0.01f);
        });

        PROCESS.whenFinishedServer((executor, funnel) -> {
            final var world = funnel.world;
            final var pos = funnel.pos;

            var targetEntity = (MobEntity) ((ServerWorld) world).getEntity(funnel.ritualEntity);

            boolean success = targetEntity.getWorld().random.nextDouble() < funnel.ritualStability;

            ConjuringParticleEvents.EXTRACTION_RITUAL_FINISHED.spawn(world, Vec3d.of(pos.add(0, 2, 0)), success);
            world.playSound(null, pos, success ? SoundEvents.ITEM_TOTEM_USE : SoundEvents.ENTITY_WITHER_HURT, SoundCategory.BLOCKS, 1, 0);
            world.setBlockState(pos, world.getBlockState(pos).with(SoulFunnelBlock.FILLED, false));

            ItemStack drop = success ? ConjuringFocus.writeData(funnel.item, targetEntity.getType()) : funnel.item;
            ItemScatterer.spawn(world, pos.getX(), pos.getY() + 1.25, pos.getZ(), drop);

            funnel.disablePedestals();
            targetEntity.kill();

            funnel.item = ItemStack.EMPTY;
            funnel.ritualEntity = null;
            funnel.ritualStability = 0.1f;

            funnel.markDirty();
        });

        PROCESS.onCancelledServer((executor, funnel) -> {
            final var world = funnel.world;
            final var funnelPos = funnel.pos;

            ConjuringParticleEvents.EXTRACTION_RITUAL_FINISHED.spawn(world, Vec3d.of(funnelPos.add(0, 2, 0)), false);
            world.playSound(null, funnelPos, SoundEvents.ENTITY_WITHER_HURT, SoundCategory.BLOCKS, 1, 0);

            funnel.disablePedestals();

            var targetEntity = (MobEntity) ((ServerWorld) world).getEntity(funnel.ritualEntity);
            if (targetEntity != null) targetEntity.kill();

            funnel.ritualEntity = null;
            funnel.markDirty();
        });

        PROCESS.finish();
    }
}
