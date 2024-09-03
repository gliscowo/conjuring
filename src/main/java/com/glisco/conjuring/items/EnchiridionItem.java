package com.glisco.conjuring.items;

import com.glisco.conjuring.Conjuring;
import com.mojang.serialization.Codec;
import io.wispforest.lavender.book.LavenderBookItem;
import io.wispforest.owo.itemgroup.OwoItemSettings;
import net.minecraft.block.Blocks;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class EnchiridionItem extends LavenderBookItem {

    public static final ComponentType<Boolean> SANDWICH = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Conjuring.id("enchiridion_sandwich"),
            ComponentType.<Boolean>builder()
                    .codec(Codec.BOOL)
                    .packetCodec(PacketCodecs.BOOL)
                    .build()
    );

    public EnchiridionItem() {
        super(new OwoItemSettings().maxCount(1).group(Conjuring.CONJURING_GROUP).component(SANDWICH, false), Conjuring.id("enchiridion"));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getPlayer().isSneaking()) return ActionResult.PASS;
        if (!context.getWorld().getBlockState(context.getBlockPos()).isOf(Blocks.SNOW_BLOCK)) return ActionResult.PASS;

        context.getStack().apply(SANDWICH, false, sandwich -> !sandwich);
        return ActionResult.SUCCESS;
    }

    @Override
    public Text getName(ItemStack stack) {
        return stack.getOrDefault(SANDWICH, false) ? Text.literal("Ice Cream Sandwich") : super.getName(stack);
    }

}
