package com.glisco.conjuring.entities;

import io.wispforest.owo.particles.ClientParticles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class SoulEntityRenderer extends EntityRenderer<SoulEntity> {

    public SoulEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(SoulEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {

        ParticleEffect dust = new DustParticleEffect(new Vector3f(0.5f, 1f, 1f), 0.75f);

        World world = MinecraftClient.getInstance().world;
        boolean allParticles = MinecraftClient.getInstance().options.getParticles().getValue() == ParticlesMode.ALL;

        double lastX = entity.lastRenderX + world.random.nextDouble() * 0.3 - 0.15;
        double lastY = entity.lastRenderY + world.random.nextDouble() * 0.3 - 0.15;
        double lastZ = entity.lastRenderZ + world.random.nextDouble() * 0.3 - 0.15;

        double targetX = entity.getX();
        double targetY = entity.getY();
        double targetZ = entity.getZ();

        Vec3d last = new Vec3d(lastX, lastY, lastZ);
        Vec3d current = new Vec3d(targetX, targetY, targetZ);

        Vec3d direction = current.subtract(last);
        Vec3d increment = direction.multiply(1 / (float) Math.round(direction.length() * 4));

        Vec3d currentRenderPosition = last.add(increment);
        for (int j = 0; j < Math.round(direction.length() * 4); j++) {

            ClientParticles.spawnWithMaxAge(ParticleTypes.SOUL_FIRE_FLAME, currentRenderPosition, world.random.nextInt((10 + entity.age)));
            if (allParticles) ClientParticles.spawn(dust, world, currentRenderPosition, 0.35);

            currentRenderPosition = currentRenderPosition.add(increment);
        }

    }

    @Override
    public Identifier getTexture(SoulEntity entity) {
        return Identifier.of("");
    }
}
