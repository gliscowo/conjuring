package com.glisco.conjuring.items;

import com.glisco.conjuring.Conjuring;
import com.mojang.serialization.Codec;
import io.wispforest.owo.itemgroup.OwoItemSettings;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class PizzaItem extends Item {

    public static final ComponentType<Boolean> BRINSA = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Conjuring.id("pizza_is_brinsa"),
            ComponentType.<Boolean>builder()
                    .codec(Codec.BOOL)
                    .packetCodec(PacketCodecs.BOOL)
                    .build()
    );

    public PizzaItem() {
        super(new OwoItemSettings().component(DataComponentTypes.FOOD, new FoodComponent.Builder().nutrition(20).saturationModifier(.75f).build()));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getStackInHand(hand).getOrDefault(BRINSA, false)) {
            user.getStackInHand(hand).set(BRINSA, true);
            return TypedActionResult.success(user.getStackInHand(hand));
        } else {
            return super.use(world, user, hand);
        }
    }
}
