package com.glisco.conjuring.entities;

import com.glisco.conjuring.Conjuring;
import io.wispforest.owo.particles.ClientParticles;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class SoulMagnetEntity extends SoulEntity {

    int ticksInBlock = 0;
    boolean recalled = false;

    public SoulMagnetEntity(World world, LivingEntity owner) {
        super(Conjuring.SOUL_MAGNET, world);
        setOwner(owner);
        maxAge = 100;
    }

    public SoulMagnetEntity(EntityType<SoulMagnetEntity> entityType, World world) {
        super(entityType, world);
        maxAge = 100;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    public void tick() {

        if ((recalled || age > 40) && getOwner() != null) {
            Vec3d ownerVector = getOwner().getPos().add(0, 0.75, 0).subtract(getPos());
            if (ownerVector.length() < 1) this.remove(RemovalReason.KILLED);

            double scalar = ownerVector.length() > 3 ? 0.075 : 0.15d;
            setVelocity(ownerVector.multiply(scalar));
        }

        super.tick();

        if (getWorld().getBlockState(getBlockPos()).isSolidBlock(getWorld(), getBlockPos())) {
            ticksInBlock++;
        } else {
            ticksInBlock = 0;
        }

        int range = Conjuring.CONFIG.tools_config.shovel_magnet_range();
        Box box = new Box(getPos().subtract(range, range, range), getPos().add(range, range, range));

        for (ItemEntity item : getWorld().getEntitiesByType(EntityType.ITEM, box, ItemEntity::isAlive)) {
            Vec3d difference = getPos().subtract(item.getPos()).multiply(0.25d);
            item.setVelocity(difference);

            if (getWorld().isClient && difference.length() > 0.5) {
                ParticleEffect dust = new DustParticleEffect(new Vector3f(0.5f, 1f, 1f), 0.5f);
                ClientParticles.setParticleCount(45);
                ClientParticles.spawnLine(dust, getWorld(), getPos(), item.getPos(), 0);
            }
        }

        for (ExperienceOrbEntity orb : getWorld().getEntitiesByType(EntityType.EXPERIENCE_ORB, box, ExperienceOrbEntity::isAlive)) {
            Vec3d difference = getPos().subtract(orb.getPos()).multiply(0.25d);
            orb.setVelocity(difference);
        }


        if (ticksInBlock > (recalled ? 30 : 8)) {
            if (recalled || age > 40) {
                this.remove(RemovalReason.KILLED);
            } else {
                recall();
                ticksInBlock = 0;
            }
        }
    }

    @Override
    public void setVelocity(Entity user, float pitch, float yaw, float roll, float modifierZ, float modifierXYZ) {
        super.setVelocity(user, pitch, yaw, roll, modifierZ, modifierXYZ);
        this.setVelocity(this.getVelocity().multiply(0.35));
    }

    public void recall() {
        this.recalled = true;
    }
}
