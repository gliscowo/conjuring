package com.glisco.conjuring.items.soul_alloy_tools;

import com.glisco.conjuring.items.ConjuringItems;
import io.wispforest.owo.ops.WorldOps;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class SoulAlloyToolAbilities {

    public static final Predicate<BlockState> NO_TOOL_OVERRIDE = blockState -> false;

    public static void registerCommonEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, playerEntity, blockPos, blockState, blockEntity) -> {
            if (!SoulAlloyToolAbilities.canAoeDig(playerEntity)) return true;

            final var playerStack = playerEntity.getMainHandStack();

            for (BlockPos pos : SoulAlloyToolAbilities.getBlocksToDig(playerEntity, ((SoulAlloyTool) playerStack.getItem()).getAoeToolOverridePredicate())) {
                WorldOps.breakBlockWithItem(world, pos, playerStack, playerEntity);

                playerStack.damage(SoulAlloyTool.getModifierLevel(playerStack, SoulAlloyTool.SoulAlloyModifier.SCOPE) * 2, playerEntity, EquipmentSlot.MAINHAND);
            }
            return true;
        });
    }

    public static boolean canAoeDig(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof SoulAlloyTool
                && SoulAlloyTool.isSecondaryEnabled(player.getMainHandStack())
                && player.getMainHandStack().getOrDefault(SoulAlloyTool.MODIFIERS, SoulAlloyTool.ModifiersComponent.DEFAULT).modifiers().containsKey(SoulAlloyTool.SoulAlloyModifier.SCOPE);
    }

    public static boolean canArmorPierce(PlayerEntity player) {
        return player.getMainHandStack().getItem() == ConjuringItems.SOUL_ALLOY_SWORD
                && SoulAlloyTool.getModifierLevel(player.getMainHandStack(), SoulAlloyTool.SoulAlloyModifier.IGNORANCE) > 0;
    }

    public static boolean canAoeHit(PlayerEntity player) {
        return player.getMainHandStack().getItem() == ConjuringItems.SOUL_ALLOY_SWORD
                && SoulAlloyTool.isSecondaryEnabled(player.getMainHandStack())
                && SoulAlloyTool.getModifierLevel(player.getMainHandStack(), SoulAlloyTool.SoulAlloyModifier.SCOPE) > 0;
    }

    public static List<BlockPos> getBlocksToDig(PlayerEntity player, Predicate<BlockState> toolOverridePredicate) {

        if ((!(player.getMainHandStack().getItem() instanceof SoulAlloyTool tool)) || !tool.canAoeDig())
            return Collections.emptyList();

        List<BlockPos> blocksToDig = new ArrayList<>();

        HitResult target = player.raycast(player.getAbilities().creativeMode ? 5.0F : 4.5F, 0, false);
        if (target.getType() != HitResult.Type.BLOCK) return blocksToDig;

        BlockPos hit = ((BlockHitResult) target).getBlockPos();
        BlockPos origin = hit;
        Direction side = ((BlockHitResult) target).getSide();
        int scopeLevel = player.getMainHandStack().getOrDefault(SoulAlloyTool.MODIFIERS, SoulAlloyTool.ModifiersComponent.DEFAULT).modifiers().getOrDefault(SoulAlloyTool.SoulAlloyModifier.SCOPE, 0);

        final var targetState = player.getWorld().getBlockState(hit);
        if (!toolOverridePredicate.test(targetState) && player.getMainHandStack().getItem().getMiningSpeed(player.getMainHandStack(), targetState) == 1)
            return blocksToDig;

        switch (side.getAxis()) {
            case X -> {
                hit = hit.add(0, -1 * scopeLevel, -1 * scopeLevel);
                for (int i = 0; i < 1 + 2 * scopeLevel; i++) {
                    for (int j = 0; j < 1 + 2 * scopeLevel; j++) {
                        blocksToDig.add(hit.add(0, i, j));
                    }
                }
            }
            case Y -> {
                hit = hit.add(-1 * scopeLevel, 0, -1 * scopeLevel);
                for (int i = 0; i < 1 + 2 * scopeLevel; i++) {
                    for (int j = 0; j < 1 + 2 * scopeLevel; j++) {
                        blocksToDig.add(hit.add(j, 0, i));
                    }
                }
            }
            case Z -> {
                hit = hit.add(-1 * scopeLevel, -1 * scopeLevel, 0);
                for (int i = 0; i < 1 + 2 * scopeLevel; i++) {
                    for (int j = 0; j < 1 + 2 * scopeLevel; j++) {
                        blocksToDig.add(hit.add(j, i, 0));
                    }
                }
            }
        }

        blocksToDig.remove(origin);
        blocksToDig.removeIf(blockPos -> {
            var state = player.getWorld().getBlockState(blockPos);
            if (state.getHardness(player.getWorld(), blockPos) < 0) return true;
            if (toolOverridePredicate.test(state)) return false;

            return player.getMainHandStack().getItem().getMiningSpeed(player.getMainHandStack(), state) <= 1;
        });

        return blocksToDig;
    }

}
