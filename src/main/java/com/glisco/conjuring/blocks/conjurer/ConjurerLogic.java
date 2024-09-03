package com.glisco.conjuring.blocks.conjurer;


import com.glisco.conjuring.util.ConjuringParticleEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.collection.DataPool;
import net.minecraft.util.collection.Weighted;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Function;

/**
 * Modified version of Minecraft's MobSpawnerLogic
 * <p>
 * Used to provide control over the logics properties,
 * to make it usable for a player-modifiable spawner
 */
public abstract class ConjurerLogic {
    public static final String SPAWN_DATA_KEY = "SpawnData";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int field_30951 = 1;
    private int spawnDelay = 20;
    private DataPool<MobSpawnerEntry> spawnPotentials = DataPool.<MobSpawnerEntry>empty();
    @Nullable
    private MobSpawnerEntry spawnEntry;
    private double rotation;
    private double lastRotation;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = 4;
    @Nullable
    private Entity renderedEntity;
    private int maxNearbyEntities = 6;
    private int requiredPlayerRange = 16;
    private int spawnRange = 4;

    // These fields are appended
    private boolean requiresPlayer = true;
    private boolean active = false;

    public void setEntityId(EntityType<?> type, @Nullable World world, Random random, BlockPos pos) {
        this.getSpawnEntry(world, random, pos).getNbt().putString("id", Registries.ENTITY_TYPE.getId(type).toString());
    }

    // This method is made public and tweaked to respect stabilized foci and redstone
    public boolean isPlayerInRange(World world, BlockPos pos) {
        return world.getReceivedRedstonePower(pos) == 0 && (!requiresPlayer || world.isPlayerInRange((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, (double) this.requiredPlayerRange));
    }

    public void clientTick(World world, BlockPos pos) {
        if (!this.isPlayerInRange(world, pos)) {
            this.lastRotation = this.rotation;
        } else if (this.renderedEntity != null) {
            Random random = world.getRandom();
            double d = (double)pos.getX() + random.nextDouble();
            double e = (double)pos.getY() + random.nextDouble();
            double f = (double)pos.getZ() + random.nextDouble();

            //These particles have been changed to reflect the custom spawner version
            world.addParticle(ParticleTypes.ENCHANTED_HIT, d, e, f, 0.0, 0.0, 0.0);
            world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, d, e, f, 0.0, 0.0, 0.0);

            if (this.spawnDelay > 0) {
                this.spawnDelay--;
            }

            this.lastRotation = this.rotation;
            this.rotation = (this.rotation + (double)(1000.0F / ((float)this.spawnDelay + 200.0F))) % 360.0;
        }
    }

    public void serverTick(ServerWorld world, BlockPos pos) {
        // Check if active
        if (this.isPlayerInRange(world, pos) && active) {
            if (this.spawnDelay == -1) {
                this.updateSpawns(world, pos);
            }

            if (this.spawnDelay > 0) {
                this.spawnDelay--;
            } else {
                boolean bl = false;
                Random random = world.getRandom();
                MobSpawnerEntry mobSpawnerEntry = this.getSpawnEntry(world, random, pos);

                for (int i = 0; i < this.spawnCount; i++) {
                    NbtCompound nbtCompound = mobSpawnerEntry.getNbt();
                    Optional<EntityType<?>> optional = EntityType.fromNbt(nbtCompound);
                    if (optional.isEmpty()) {
                        this.updateSpawns(world, pos);
                        return;
                    }

                    NbtList nbtList = nbtCompound.getList("Pos", NbtElement.DOUBLE_TYPE);
                    int j = nbtList.size();
                    double d = j >= 1 ? nbtList.getDouble(0) : (double)pos.getX() + (random.nextDouble() - random.nextDouble()) * (double)this.spawnRange + 0.5;
                    double e = j >= 2 ? nbtList.getDouble(1) : (double)(pos.getY() + random.nextInt(3) - 1);
                    double f = j >= 3 ? nbtList.getDouble(2) : (double)pos.getZ() + (random.nextDouble() - random.nextDouble()) * (double)this.spawnRange + 0.5;
                    if (world.isSpaceEmpty(((EntityType)optional.get()).getSpawnBox(d, e, f))) {
                        BlockPos blockPos = BlockPos.ofFloored(d, e, f);
                        if (mobSpawnerEntry.getCustomSpawnRules().isPresent()) {
                            if (!((EntityType)optional.get()).getSpawnGroup().isPeaceful() && world.getDifficulty() == Difficulty.PEACEFUL) {
                                continue;
                            }

                            MobSpawnerEntry.CustomSpawnRules customSpawnRules = (MobSpawnerEntry.CustomSpawnRules)mobSpawnerEntry.getCustomSpawnRules().get();
                            if (!customSpawnRules.canSpawn(blockPos, world)) {
                                continue;
                            }
                        } else if (!SpawnRestriction.canSpawn((EntityType)optional.get(), world, SpawnReason.SPAWNER, blockPos, world.getRandom())) {
                            continue;
                        }

                        Entity entity = EntityType.loadEntityWithPassengers(nbtCompound, world, entityx -> {
                            entityx.refreshPositionAndAngles(d, e, f, entityx.getYaw(), entityx.getPitch());
                            return entityx;
                        });
                        if (entity == null) {
                            this.updateSpawns(world, pos);
                            return;
                        }

                        int k = world.getEntitiesByType(
                                        TypeFilter.equals(entity.getClass()),
                                        new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1))
                                                .expand((double)this.spawnRange),
                                        EntityPredicates.EXCEPT_SPECTATOR
                                )
                                .size();
                        if (k >= this.maxNearbyEntities) {
                            this.updateSpawns(world, pos);
                            return;
                        }

                        entity.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), random.nextFloat() * 360.0F, 0.0F);
                        if (entity instanceof MobEntity mobEntity) {
                            if (mobSpawnerEntry.getCustomSpawnRules().isEmpty() && !mobEntity.canSpawn(world, SpawnReason.SPAWNER) || !mobEntity.canSpawn(world)) {
                                continue;
                            }

                            boolean bl2 = mobSpawnerEntry.getNbt().getSize() == 1 && mobSpawnerEntry.getNbt().contains("id", NbtElement.STRING_TYPE);
                            if (bl2) {
                                ((MobEntity)entity).initialize(world, world.getLocalDifficulty(entity.getBlockPos()), SpawnReason.SPAWNER, null);
                            }

                            mobSpawnerEntry.getEquipment().ifPresent(mobEntity::setEquipmentFromTable);
                        }

                        if (!world.spawnNewEntityAndPassengers(entity)) {
                            this.updateSpawns(world, pos);
                            return;
                        }

                        world.syncWorldEvent(WorldEvents.SPAWNER_SPAWNS_MOB, pos, 0);
                        world.emitGameEvent(entity, GameEvent.ENTITY_PLACE, blockPos);
                        //This worldEvent instead emits a conjuring particle event
                        ConjuringParticleEvents.CONJURER_SUMMON.spawn(world, Vec3d.of(pos), null);
                        if (entity instanceof MobEntity) {
                            ((MobEntity)entity).playSpawnEffects();
                        }

                        bl = true;
                    }
                }

                if (bl) {
                    this.updateSpawns(world, pos);
                }
            }
        }
    }

    public void updateSpawns(World world, BlockPos pos) {
        Random random = world.random;
        if (this.maxSpawnDelay <= this.minSpawnDelay) {
            this.spawnDelay = this.minSpawnDelay;
        } else {
            this.spawnDelay = this.minSpawnDelay + random.nextInt(this.maxSpawnDelay - this.minSpawnDelay);
        }

        this.spawnPotentials.getOrEmpty(random).ifPresent(spawnPotential -> this.setSpawnEntry(world, pos, (MobSpawnerEntry)spawnPotential.data()));
        this.sendStatus(world, pos, 1);
    }

    public void readNbt(@Nullable World world, BlockPos pos, NbtCompound nbt) {
        this.spawnDelay = nbt.getShort("Delay");
        boolean bl = nbt.contains("SpawnData", NbtElement.COMPOUND_TYPE);
        if (bl) {
            MobSpawnerEntry mobSpawnerEntry = (MobSpawnerEntry)MobSpawnerEntry.CODEC
                    .parse(NbtOps.INSTANCE, nbt.getCompound("SpawnData"))
                    .resultOrPartial(string -> LOGGER.warn("Invalid SpawnData: {}", string))
                    .orElseGet(MobSpawnerEntry::new);
            this.setSpawnEntry(world, pos, mobSpawnerEntry);
        }

        boolean bl2 = nbt.contains("SpawnPotentials", NbtElement.LIST_TYPE);
        if (bl2) {
            NbtList nbtList = nbt.getList("SpawnPotentials", NbtElement.COMPOUND_TYPE);
            this.spawnPotentials = (DataPool<MobSpawnerEntry>)MobSpawnerEntry.DATA_POOL_CODEC
                    .parse(NbtOps.INSTANCE, nbtList)
                    .resultOrPartial(error -> LOGGER.warn("Invalid SpawnPotentials list: {}", error))
                    .orElseGet(() -> DataPool.<MobSpawnerEntry>empty());
        } else {
            this.spawnPotentials = DataPool.of(this.spawnEntry != null ? this.spawnEntry : new MobSpawnerEntry());
        }

        if (nbt.contains("MinSpawnDelay", NbtElement.NUMBER_TYPE)) {
            this.minSpawnDelay = nbt.getShort("MinSpawnDelay");
            this.maxSpawnDelay = nbt.getShort("MaxSpawnDelay");
            this.spawnCount = nbt.getShort("SpawnCount");
        }

        if (nbt.contains("MaxNearbyEntities", NbtElement.NUMBER_TYPE)) {
            this.maxNearbyEntities = nbt.getShort("MaxNearbyEntities");
            this.requiredPlayerRange = nbt.getShort("RequiredPlayerRange");
        }

        if (nbt.contains("SpawnRange", NbtElement.NUMBER_TYPE)) {
            this.spawnRange = nbt.getShort("SpawnRange");
        }

        // Read custom values
        if (nbt.contains("RequiresPlayer")) {
            this.requiresPlayer = nbt.getBoolean("RequiresPlayer");
        }

        if (nbt.contains("Active")) {
            this.active = nbt.getBoolean("Active");
        }

        this.renderedEntity = null;
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putShort("Delay", (short)this.spawnDelay);
        nbt.putShort("MinSpawnDelay", (short)this.minSpawnDelay);
        nbt.putShort("MaxSpawnDelay", (short)this.maxSpawnDelay);
        nbt.putShort("SpawnCount", (short)this.spawnCount);
        nbt.putShort("MaxNearbyEntities", (short)this.maxNearbyEntities);
        nbt.putShort("RequiredPlayerRange", (short)this.requiredPlayerRange);
        nbt.putShort("SpawnRange", (short)this.spawnRange);
        if (this.spawnEntry != null) {
            nbt.put(
                    "SpawnData",
                    MobSpawnerEntry.CODEC.encodeStart(NbtOps.INSTANCE, this.spawnEntry).getOrThrow(string -> new IllegalStateException("Invalid SpawnData: " + string))
            );
        }

        nbt.put("SpawnPotentials", MobSpawnerEntry.DATA_POOL_CODEC.encodeStart(NbtOps.INSTANCE, this.spawnPotentials).getOrThrow());

        // Write custom values
        nbt.putBoolean("RequiresPlayer", requiresPlayer);
        nbt.putBoolean("Active", active);

        return nbt;
    }

    @Nullable
    public Entity getRenderedEntity(World world, BlockPos pos) {
        if (this.renderedEntity == null) {
            NbtCompound nbtCompound = this.getSpawnEntry(world, world.getRandom(), pos).getNbt();
            if (!nbtCompound.contains("id", NbtElement.STRING_TYPE)) {
                return null;
            }

            this.renderedEntity = EntityType.loadEntityWithPassengers(nbtCompound, world, Function.identity());
            if (nbtCompound.getSize() == 1 && this.renderedEntity instanceof MobEntity) {
            }
        }

        return this.renderedEntity;
    }

    public boolean handleStatus(World world, int status) {
        if (status == 1) {
            if (world.isClient) {
                this.spawnDelay = this.minSpawnDelay;
            }

            return true;
        } else {
            return false;
        }
    }

    protected void setSpawnEntry(@Nullable World world, BlockPos pos, MobSpawnerEntry spawnEntry) {
        this.spawnEntry = spawnEntry;
    }

    private MobSpawnerEntry getSpawnEntry(@Nullable World world, Random random, BlockPos pos) {
        if (this.spawnEntry != null) {
            return this.spawnEntry;
        } else {
            this.setSpawnEntry(world, pos, (MobSpawnerEntry)this.spawnPotentials.getOrEmpty(random).map(Weighted.Present::data).orElseGet(MobSpawnerEntry::new));
            return this.spawnEntry;
        }
    }

    public abstract void sendStatus(World world, BlockPos pos, int status);

    public double getRotation() {
        return this.rotation;
    }

    public double getLastRotation() {
        return this.lastRotation;
    }

    //Custom methods to control the logics properties
    public void setRequiredPlayerRange(int requiredPlayerRange) {
        this.requiredPlayerRange = requiredPlayerRange;
    }

    public void setMinSpawnDelay(int minSpawnDelay) {
        this.minSpawnDelay = minSpawnDelay;
    }

    public void setMaxSpawnDelay(int maxSpawnDelay) {
        this.maxSpawnDelay = maxSpawnDelay;
    }

    public void setSpawnCount(int spawnCount) {
        this.spawnCount = spawnCount;
    }

    public void setMaxNearbyEntities(int maxNearbyEntities) {
        this.maxNearbyEntities = maxNearbyEntities;
    }

    public void setEnty(MobSpawnerEntry entry) {
        this.spawnPotentials = DataPool.of(entry);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setRequiresPlayer(boolean requiresPlayer) {
        this.requiresPlayer = requiresPlayer;
    }

    public boolean isActive() {
        return active;
    }
}

