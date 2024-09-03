package com.glisco.conjuring.blocks.conjurer;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.items.ConjuringFocus;
import com.glisco.conjuring.items.ConjuringItems;
import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.item.ItemStack;

import java.util.Optional;

public class ConjurerHelper {

    public static void updateConjurerProperties(ConjurerBlockEntity conjurer) {
        int requiredPlayerRange = 16;
        int spawnCount = 4;
        int maxSpawnDelay = 800;
        int maxNearbyEntities = 6;

        ItemStack focus = conjurer.getStack(0);
        ItemStack hasteCharms = conjurer.getStack(1);
        ItemStack abundanceCharms = conjurer.getStack(2);
        ItemStack scopeCharms = conjurer.getStack(3);
        ItemStack ignoranceCharms = conjurer.getStack(4);

        if (focus.getItem() instanceof ConjuringFocus) {
            MobSpawnerEntry entry = new MobSpawnerEntry(focus.get(ConjuringFocus.ENTITY).copyNbt(), Optional.empty(), Optional.empty());

            conjurer.getLogic().setEnty(entry);
            conjurer.getLogic().setSpawnEntry(conjurer.getWorld(), conjurer.getPos(), entry);
            conjurer.setActive(true);

            conjurer.setRequiresPlayer(focus.getItem() != ConjuringItems.STABILIZED_CONJURING_FOCUS);

        } else {
            conjurer.setActive(false);
            conjurer.setRequiresPlayer(true);
            return;
        }

        if (hasteCharms.getItem() == ConjuringItems.HASTE_CHARM) {
            maxSpawnDelay = Math.max(10, Math.round(800 - hasteCharms.getCount() * Conjuring.CONFIG.conjurer_config.haste_multiplier()));
        }

        if (abundanceCharms.getItem() == ConjuringItems.ABUNDANCE_CHARM) {
            spawnCount = 4 + abundanceCharms.getCount() * Conjuring.CONFIG.conjurer_config.abundance_multiplier();
        }

        if (scopeCharms.getItem() == ConjuringItems.SCOPE_CHARM) {
            requiredPlayerRange = 16 + scopeCharms.getCount() * Conjuring.CONFIG.conjurer_config.scope_multiplier();
        }

        if (ignoranceCharms.getItem() == ConjuringItems.IGNORANCE_CHARM) {
            maxNearbyEntities = 6 + ignoranceCharms.getCount() * Conjuring.CONFIG.conjurer_config.ignorance_multiplier();
        }

        conjurer.getLogic().setRequiredPlayerRange(requiredPlayerRange);
        conjurer.getLogic().setMaxNearbyEntities(maxNearbyEntities);
        conjurer.getLogic().setMaxSpawnDelay(maxSpawnDelay);
        conjurer.getLogic().setMinSpawnDelay(maxSpawnDelay / 4);
        conjurer.getLogic().setSpawnCount(spawnCount);
        conjurer.getLogic().updateSpawns(conjurer.getWorld(), conjurer.getPos());
    }
}
