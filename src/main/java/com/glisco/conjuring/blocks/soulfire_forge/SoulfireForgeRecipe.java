package com.glisco.conjuring.blocks.soulfire_forge;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.blocks.ConjuringBlocks;
import com.glisco.conjuring.util.ListRecipeInput;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class SoulfireForgeRecipe implements Recipe<SoulfireForgeRecipe.Input> {

    final DefaultedList<Ingredient> inputs;
    final ItemStack result;
    final int smeltTime;

    @Override
    public boolean isIgnoredInRecipeBook() {
        return true;
    }

    public SoulfireForgeRecipe(ItemStack result, int smeltTime, DefaultedList<Ingredient> inputs) {
        this.result = result;
        this.smeltTime = smeltTime;
        this.inputs = inputs;
    }

    @Override
    public boolean matches(Input inventory, World world) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int index = r * 3 + c;
                if (!inputs.get(index).test(inventory.getStackInSlot(index))) return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack craft(Input inventory, RegistryWrapper.WrapperLookup registries) {
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

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        return this.inputs;
    }

    @Environment(EnvType.CLIENT)
    public ItemStack createIcon() {
        return new ItemStack(ConjuringBlocks.SOULFIRE_FORGE);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SoulfireForgeRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public int getSmeltTime() {
        return smeltTime;
    }

    public static class Type implements RecipeType<SoulfireForgeRecipe> {
        private Type() {}

        public static final Type INSTANCE = new Type();

        public static final Identifier ID = Conjuring.id("soulfire_forge");
    }

    public static class Input implements RecipeInput {
        private final SoulfireForgeBlockEntity forge;
        public Input(SoulfireForgeBlockEntity forge) {
            this.forge = forge;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return this.forge.getStack(slot);
        }

        @Override
        public int getSize() {
            return this.forge.size();
        }
    }
}
