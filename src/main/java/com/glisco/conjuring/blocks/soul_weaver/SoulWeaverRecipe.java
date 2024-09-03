package com.glisco.conjuring.blocks.soul_weaver;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.blocks.ConjuringBlocks;
import com.glisco.conjuring.util.ListRecipeInput;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class SoulWeaverRecipe implements Recipe<ListRecipeInput> {

    private final DefaultedList<Ingredient> inputs;
    private final ItemStack result;
    public final boolean transferTag;

    @Override
    public boolean isIgnoredInRecipeBook() {
        return true;
    }

    public SoulWeaverRecipe(ItemStack result, DefaultedList<Ingredient> inputs, boolean transferTag) {
        this.result = result;
        this.inputs = inputs;
        this.transferTag = transferTag;
    }

    @Override
    public boolean matches(ListRecipeInput inventory, World world) {
        List<ItemStack> testList = new ArrayList<>();

        for (int i = 0; i < inventory.getSize(); i++) {
            testList.add(inventory.getStackInSlot(i));
        }

        return inputs.stream().allMatch(ingredient -> {

            for (int i = 0; i < testList.size(); i++) {
                if (ingredient.test(testList.get(i))) {
                    testList.remove(i);
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public ItemStack craft(ListRecipeInput inventory, RegistryWrapper.WrapperLookup registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean fits(int var1, int var2) {
        return false;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registries) {
        return result.copy();
    }

    public DefaultedList<Ingredient> getInputs() {
        return inputs;
    }

    @Environment(EnvType.CLIENT)
    public ItemStack createIcon() {
        return new ItemStack(ConjuringBlocks.SOUL_WEAVER);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SoulWeaverRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<SoulWeaverRecipe> {
        private Type() {}

        public static final Type INSTANCE = new Type();
        public static final Identifier ID = Conjuring.id("soul_weaving");
    }
}
