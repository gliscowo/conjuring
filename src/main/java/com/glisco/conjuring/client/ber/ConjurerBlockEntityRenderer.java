package com.glisco.conjuring.client.ber;

import com.glisco.conjuring.blocks.conjurer.ConjurerBlockEntity;
import com.glisco.conjuring.blocks.conjurer.ConjurerLogic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class ConjurerBlockEntityRenderer implements BlockEntityRenderer<ConjurerBlockEntity> {

    public ConjurerBlockEntityRenderer(BlockEntityRendererFactory.Context context) {}

    public void render(ConjurerBlockEntity conjurerBlockEntity, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
        if (!conjurerBlockEntity.isActive()) return;

        matrixStack.push();

        matrixStack.translate(0.5D, 0.0D, 0.5D);
        ConjurerLogic mobSpawnerLogic = conjurerBlockEntity.getLogic();
        Entity entity = mobSpawnerLogic.getRenderedEntity(conjurerBlockEntity.getWorld(), conjurerBlockEntity.getPos());
        if (entity != null) {
            float g = 0.53125F;
            float h = Math.max(entity.getWidth(), entity.getHeight());
            if ((double) h > 1.0D) {
                g /= h;
            }

            if (!conjurerBlockEntity.getLogic().isPlayerInRange(conjurerBlockEntity.getWorld(), conjurerBlockEntity.getPos())) tickDelta = 0;

            matrixStack.translate(0.0D, 0.4000000059604645D, 0.0D);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) MathHelper.lerp(tickDelta, mobSpawnerLogic.getLastRotation(), mobSpawnerLogic.getRotation()) * 10.0F));
            matrixStack.translate(0.0D, -0.20000000298023224D, 0.0D);
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F));
            matrixStack.scale(g, g, g);
            MinecraftClient.getInstance().getEntityRenderDispatcher().render(entity, 0.0D, 0.0D, 0.0D, 0.0F, tickDelta, matrixStack, vertexConsumerProvider, i);
        }

        matrixStack.pop();
    }
}
