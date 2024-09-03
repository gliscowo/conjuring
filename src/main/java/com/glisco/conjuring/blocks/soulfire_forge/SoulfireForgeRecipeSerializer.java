package com.glisco.conjuring.blocks.soulfire_forge;

import com.glisco.conjuring.util.DefaultedListEndec;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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

public class SoulfireForgeRecipeSerializer extends EndecRecipeSerializer<SoulfireForgeRecipe> {

    public static final StructEndec<SoulfireForgeRecipe> ENDEC = SoulfireForgeRecipeModel.ENDEC.xmap(model -> {
        var inputs = DefaultedList.ofSize(9, Ingredient.EMPTY);

        int rowIdx = 0;
        for (var row : model.pattern()) {
            int columnIdx = 0;
            for (char c : row.toCharArray()) {
                if (c == ' ') {
                    inputs.set(rowIdx * 3 + columnIdx, Ingredient.EMPTY);
                } else {
                    var ingredient = model.key().get(c);
                    if (ingredient == null) {
                        throw new JsonParseException("Pattern references symbol '" + c + "' which was not defined in key");
                    }

                    inputs.set(rowIdx * 3 + columnIdx, ingredient);
                }
                columnIdx++;
            }

            rowIdx++;
        }

        return new SoulfireForgeRecipe(model.result(), model.smeltTime(), inputs);
    }, soulfireForgeRecipe -> {throw new UnsupportedOperationException("don't serialize recipes :)");});

    public static final StructEndec<SoulfireForgeRecipe> NETWORK_ENDEC = StructEndecBuilder.of(
            DefaultedListEndec.forSize(CodecUtils.toEndec(Ingredient.ALLOW_EMPTY_CODEC), Ingredient.EMPTY, Ingredient::isEmpty, 9).fieldOf("inputs", recipe -> recipe.inputs),
            MinecraftEndecs.ITEM_STACK.fieldOf("result", recipe -> recipe.result),
            ENDEC.VAR_INT.fieldOf("smeltTime", recipe -> recipe.smeltTime),
            (inputs, result, smeltTime) -> new SoulfireForgeRecipe(result, smeltTime, inputs)
    );

    private SoulfireForgeRecipeSerializer() {
        super(ENDEC, NETWORK_ENDEC);
    }

    public static final SoulfireForgeRecipeSerializer INSTANCE = new SoulfireForgeRecipeSerializer();
    public static final Identifier ID = SoulfireForgeRecipe.Type.ID;
}
