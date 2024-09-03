package com.glisco.conjuring.blocks.soul_weaver;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.EndecRecipeSerializer;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;

public class SoulWeaverRecipeSerializer extends EndecRecipeSerializer<SoulWeaverRecipe> {

    public static final StructEndec<SoulWeaverRecipe> ENDEC = StructEndecBuilder.of(
            MinecraftEndecs.ITEM_STACK.fieldOf("result", s -> s.getResult(null)),
            CodecUtils.toEndec(Ingredient.DISALLOW_EMPTY_CODEC).listOf().validate(ingredients -> {
                if (ingredients.size() > 5) {
                    throw new JsonParseException("Gem tinkerer recipes cannot have more than 5 inputs");
                }
            }).fieldOf("inputs", SoulWeaverRecipe::getInputs),
            Endec.BOOLEAN.fieldOf("transferTag", soulWeaverRecipe -> soulWeaverRecipe.transferTag),
            (stack, ingredients, transferTag) -> {
                var inputs = DefaultedList.ofSize(5, Ingredient.EMPTY);
                for (int i = 0; i < ingredients.size(); i++) {
                    inputs.set(i, ingredients.get(i));
                }

                return new SoulWeaverRecipe(stack, inputs, transferTag);
            }
    );

    private SoulWeaverRecipeSerializer() {
        super(ENDEC);
    }

    public static final SoulWeaverRecipeSerializer INSTANCE = new SoulWeaverRecipeSerializer();
    public static final Identifier ID = SoulWeaverRecipe.Type.ID;
}
