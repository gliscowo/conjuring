package com.glisco.conjuring.client;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.blocks.ConjuringBlocks;
import com.glisco.conjuring.blocks.soulfire_forge.SoulfireForgeRecipe;
import com.glisco.conjuring.client.ber.*;
import com.glisco.conjuring.client.ui.ConjurerScreen;
import com.glisco.conjuring.client.ui.SoulfireForgeScreen;
import com.glisco.conjuring.entities.SoulEntityRenderer;
import com.glisco.conjuring.items.ConjuringItems;
import com.glisco.conjuring.items.soul_alloy_tools.ChangeToolModePacket;
import com.glisco.conjuring.items.soul_alloy_tools.SoulAlloyTool;
import com.glisco.conjuring.items.soul_alloy_tools.SoulAlloyToolAbilities;
import com.glisco.conjuring.mixin.WorldRendererInvoker;
import io.wispforest.lavender.client.LavenderBookScreen;
import io.wispforest.lavender.md.ItemListComponent;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.parsing.UIModelLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeGridAligner;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

@Environment(EnvType.CLIENT)
public class ConjuringClient implements ClientModInitializer {

    public static final KeyBinding TOGGLE_TOOL_MODE = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.conjuring.toggle_tool_mode", GLFW.GLFW_KEY_LEFT_ALT, "category.conjuring"));

    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(ConjuringBlocks.Entities.CONJURER, ConjurerBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ConjuringBlocks.Entities.BLACKSTONE_PEDESTAL, BlackstonePedestalBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ConjuringBlocks.Entities.SOUL_FUNNEL, SoulFunnelBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ConjuringBlocks.Entities.SOUL_WEAVER, SoulWeaverBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(ConjuringBlocks.Entities.GEM_TINKERER, GemTinkererBlockEntityRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ConjuringBlocks.CONJURER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ConjuringBlocks.SOULFIRE_FORGE, RenderLayer.getCutout());

        EntityRendererRegistry.register(Conjuring.SOUL_PROJECTILE, SoulEntityRenderer::new);
        EntityRendererRegistry.register(Conjuring.SOUL_DIGGER, SoulEntityRenderer::new);
        EntityRendererRegistry.register(Conjuring.SOUL_FELLER, SoulEntityRenderer::new);
        EntityRendererRegistry.register(Conjuring.SOUL_MAGNET, SoulEntityRenderer::new);
        EntityRendererRegistry.register(Conjuring.SOUL_HARVESTER, SoulEntityRenderer::new);

        ModelPredicateProviderRegistry.register(ConjuringItems.CONJURING_FOCUS, Conjuring.id("has_soul"), (stack, world, entity, seed) -> stack.getOrCreateNbt().contains("Entity") ? 1f : 0f);
        ModelPredicateProviderRegistry.register(ConjuringItems.STABILIZED_CONJURING_FOCUS, Conjuring.id("has_soul"), (stack, world, entity, seed) -> stack.getOrCreateNbt().contains("Entity") ? 1f : 0f);
        ModelPredicateProviderRegistry.register(ConjuringItems.ENCHIRIDION, Conjuring.id("is_sandwich"), (stack, world, entity, seed) -> stack.getOrCreateNbt().getBoolean("Sandwich") ? 1f : 0f);
        ModelPredicateProviderRegistry.register(ConjuringItems.PIZZA, Conjuring.id("is_brinsa"), (stack, world, entity, seed) -> stack.getOrCreateNbt().contains("Brinsa") ? 1f : 0f);

        HandledScreens.register(Conjuring.CONJURER_SCREEN_HANDLER_TYPE, ConjurerScreen::new);
        HandledScreens.register(Conjuring.SOULFIRE_FORGE_SCREEN_HANDLER_TYPE, SoulfireForgeScreen::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_TOOL_MODE.wasPressed()) {
                client.getNetworkHandler().sendPacket(ChangeToolModePacket.create());
            }
        });

        WorldRenderEvents.BLOCK_OUTLINE.register((worldRenderContext, blockOutlineContext) -> {
            final MinecraftClient client = MinecraftClient.getInstance();
            if (!SoulAlloyToolAbilities.canAoeDig(client.player)) {
                return true;
            }

            BlockState blockState;
            for (BlockPos pos : SoulAlloyToolAbilities.getBlocksToDig(client.player, ((SoulAlloyTool) client.player.getMainHandStack().getItem()).getAoeToolOverridePredicate())) {

                blockState = client.world.getBlockState(pos);
                if (!blockState.isAir()) {
                    WorldRendererInvoker.conjuring_drawCuboidShapeOutline(worldRenderContext.matrixStack(), blockOutlineContext.vertexConsumer(), blockState.getOutlineShape(client.world, pos, ShapeContext.of(blockOutlineContext.entity())), (double) pos.getX() - blockOutlineContext.cameraX(), (double) pos.getY() - blockOutlineContext.cameraY(), (double) pos.getZ() - blockOutlineContext.cameraZ(), 0.0F, 0.0F, 0.0F, 0.4F);
                }
            }
            return true;
        });

        LavenderBookScreen.registerRecipePreviewBuilder(Conjuring.id("enchiridion"), SoulfireForgeRecipe.Type.INSTANCE, (componentSource, recipeEntry) -> {
            var recipe = recipeEntry.value();
            var recipeComponent = componentSource.template(
                    UIModelLoader.get(Conjuring.id("enchiridion_components")),
                    ParentComponent.class,
                    "soulfire-forge-recipe",
                    Map.of("duration", (recipe.getSmeltTime() / 20) + "s")
            );

            var inputGrid = recipeComponent.childById(ParentComponent.class, "input-grid");
            ((RecipeGridAligner<Ingredient>) (inputs, slot, amount, gridX, gridY) -> {
                if (!(inputGrid.children().get(slot) instanceof ItemListComponent itemList)) return;
                itemList.ingredient(inputs.next());
            }).alignRecipeToGrid(3, 3, 9, recipeEntry, recipe.getIngredients().iterator(), 0);

            recipeComponent.childById(ItemComponent.class, "output").stack(recipe.getResult(MinecraftClient.getInstance().world.getRegistryManager()));
            return recipeComponent;
        });

    }

}
