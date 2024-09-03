package com.glisco.conjuring.items.soul_alloy_tools;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.entities.SoulMagnetEntity;
import io.wispforest.owo.itemgroup.OwoItemSettings;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class SoulAlloyShovel extends ShovelItem implements SoulAlloyTool {

    private static final ComponentType<UUID> CURRENT_PROJECTILE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Conjuring.id("soul_alloy_shovel_projectile"),
            ComponentType.<UUID>builder()
                    .codec(Uuids.INT_STREAM_CODEC)
                    .packetCodec(Uuids.PACKET_CODEC)
                    .build()
    );

    public SoulAlloyShovel() {
        super(SoulAlloyToolMaterial.INSTANCE, new OwoItemSettings().group(Conjuring.CONJURING_GROUP).rarity(Rarity.UNCOMMON));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer().isSneaking()) {
            return super.useOnBlock(context);
        } else {
            return ActionResult.PASS;
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!world.isClient()) {

            ItemStack shovel = user.getStackInHand(hand);

            if (shovel.contains(CURRENT_PROJECTILE) && ((ServerWorld) world).getEntity(shovel.get(CURRENT_PROJECTILE)) != null) {
                ((SoulMagnetEntity) ((ServerWorld) world).getEntity(shovel.get(CURRENT_PROJECTILE))).recall();
                shovel.remove(CURRENT_PROJECTILE);
            } else {
                SoulMagnetEntity magnet = new SoulMagnetEntity(world, user);
                magnet.refreshPositionAndAngles(user.getX(), user.getEyeY(), user.getZ(), 0, 0);
                magnet.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 1.5f, 1);
                world.spawnEntity(magnet);

                shovel.set(CURRENT_PROJECTILE, magnet.getUuid());
            }
        }

        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return SoulAlloyTool.isSecondaryEnabled(stack) || super.isItemBarVisible(stack);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return SoulAlloyTool.isSecondaryEnabled(stack)
                ? 0x00FFFFF
                : super.getItemBarColor(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.addAll(SoulAlloyTool.getTooltip(stack));
    }

    @Override
    public boolean canAoeDig() {
        return true;
    }

}
