package com.glisco.conjuring.blocks.soulfire_forge;

import com.glisco.conjuring.mixin.RawShapedRecipeDataAccessor;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Map;

public record SoulfireForgeRecipeModel(Map<Character, Ingredient> key, List<String> pattern, int smeltTime,
                                       ItemStack result) {

    public static final Endec<List<String>> PATTERN_ENDEC = Endec.STRING.listOf().xmap(rows -> {
        if (rows.size() != 3) {
            throw new JsonParseException("Invalid pattern: must have three rows");
        } else {
            for (var row : rows) {
                if (row.length() != 3) {
                    throw new JsonParseException("Invalid pattern: each row must be three characters");
                }
            }

            return rows;
        }
    }, strings -> strings);

    public static final StructEndec<SoulfireForgeRecipeModel> ENDEC = StructEndecBuilder.of(
            CodecUtils.toEndec(Codecs.strictUnboundedMap(RawShapedRecipeDataAccessor.conjuring$keyEntryCodec(), Ingredient.DISALLOW_EMPTY_CODEC)).fieldOf("key", SoulfireForgeRecipeModel::key),
            PATTERN_ENDEC.fieldOf("pattern", SoulfireForgeRecipeModel::pattern),
            Endec.INT.fieldOf("smeltTime", SoulfireForgeRecipeModel::smeltTime),
            MinecraftEndecs.ITEM_STACK.fieldOf("result", SoulfireForgeRecipeModel::result),
            SoulfireForgeRecipeModel::new
    );
}
