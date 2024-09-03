package com.glisco.conjuring.util;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.util.collection.DefaultedList;

public class ListRecipeInput implements RecipeInput {

    private final DefaultedList<ItemStack> items;

    public ListRecipeInput(DefaultedList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.items.get(slot);
    }

    @Override
    public int getSize() {
        return this.items.size();
    }
}
