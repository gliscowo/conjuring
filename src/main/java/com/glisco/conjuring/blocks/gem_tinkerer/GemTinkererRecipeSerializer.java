package com.glisco.conjuring.blocks.gem_tinkerer;

import com.google.gson.JsonParseException;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.EndecRecipeSerializer;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public class GemTinkererRecipeSerializer extends EndecRecipeSerializer<GemTinkererRecipe> {

    public static final StructEndec<GemTinkererRecipe> ENDEC = StructEndecBuilder.of(
            MinecraftEndecs.ITEM_STACK.fieldOf("result", o -> o.getResult(null)),
            CodecUtils.toEndec(Ingredient.DISALLOW_EMPTY_CODEC).listOf().validate(ingredients -> {
                if (ingredients.size() > 5) {
                    throw new JsonParseException("Gem tinkerer recipes cannot have more than 5 inputs");
                }
            }).fieldOf("inputs", GemTinkererRecipe::getInputs),
            (stack, ingredients) -> {
                var inputs = DefaultedList.ofSize(5, Ingredient.EMPTY);
                for (int i = 0; i < ingredients.size(); i++) {
                    inputs.set(i, ingredients.get(i));
                }

                return new GemTinkererRecipe(stack, inputs);
            }
    );

    private GemTinkererRecipeSerializer() {
        super(ENDEC);
    }

    public static final GemTinkererRecipeSerializer INSTANCE = new GemTinkererRecipeSerializer();
    public static final Identifier ID = GemTinkererRecipe.Type.ID;
}
