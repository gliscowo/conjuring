package com.glisco.conjuring.items.soul_alloy_tools;

import com.glisco.conjuring.items.ConjuringItems;
import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

public class SoulAlloyToolMaterial implements ToolMaterial {

    public static final SoulAlloyToolMaterial INSTANCE = new SoulAlloyToolMaterial();

    private SoulAlloyToolMaterial() {}

    @Override
    public int getDurability() {
        return 2500;
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return 10.0f;
    }

    @Override
    public float getAttackDamage() {
        return 5.0f;
    }

    @Override
    public TagKey<Block> getInverseTag() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantability() {
        return 18;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(ConjuringItems.SOUL_ALLOY);
    }
}
