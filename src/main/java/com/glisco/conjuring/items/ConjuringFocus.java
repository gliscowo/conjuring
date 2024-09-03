package com.glisco.conjuring.items;

import com.glisco.conjuring.Conjuring;
import com.mojang.serialization.Codec;
import io.wispforest.owo.itemgroup.OwoItemSettings;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.Util;

import java.util.List;

public class ConjuringFocus extends Item {

    public static final ComponentType<NbtComponent> ENTITY = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Conjuring.id("conjuring_focus_entity"),
            ComponentType.<NbtComponent>builder()
                    .codec(NbtComponent.CODEC)
                    .packetCodec(NbtComponent.PACKET_CODEC)
                    .build()
    );

    private final boolean hasGlint;

    public ConjuringFocus(boolean hasGlint) {
        super(new OwoItemSettings().group(Conjuring.CONJURING_GROUP).maxCount(1).rarity(Rarity.UNCOMMON));
        this.hasGlint = hasGlint;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (!stack.contains(ENTITY)) return;

        tooltip.add(Text.translatable(Util.createTranslationKey(
                "entity",
                stack.get(ENTITY).get(Codec.STRING.fieldOf("id")).result().map(Identifier::tryParse).orElse(null)
        )).formatted(Formatting.GRAY));
    }

    public static ItemStack writeData(ItemStack focus, EntityType<?> entityType) {
        var entityTag = new NbtCompound();
        entityTag.putString("id", Registries.ENTITY_TYPE.getId(entityType).toString());

        focus.set(ENTITY, NbtComponent.of(entityTag));
        return focus;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return this.hasGlint;
    }
}
