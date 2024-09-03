package com.glisco.conjuring;

import com.glisco.conjuring.blocks.ConjuringBlocks;
import com.glisco.conjuring.blocks.gem_tinkerer.GemTinkererRecipe;
import com.glisco.conjuring.blocks.gem_tinkerer.GemTinkererRecipeSerializer;
import com.glisco.conjuring.blocks.soul_weaver.SoulWeaverRecipe;
import com.glisco.conjuring.blocks.soul_weaver.SoulWeaverRecipeSerializer;
import com.glisco.conjuring.blocks.soulfire_forge.SoulfireForgeRecipe;
import com.glisco.conjuring.blocks.soulfire_forge.SoulfireForgeRecipeSerializer;
import com.glisco.conjuring.entities.*;
import com.glisco.conjuring.items.ConjuringItems;
import com.glisco.conjuring.items.soul_alloy_tools.BlockCrawler;
import com.glisco.conjuring.items.soul_alloy_tools.ChangeToolModePacket;
import com.glisco.conjuring.items.soul_alloy_tools.SoulAlloyTool;
import com.glisco.conjuring.items.soul_alloy_tools.SoulAlloyToolAbilities;
import com.glisco.conjuring.util.*;
import io.wispforest.owo.Owo;
import io.wispforest.owo.itemgroup.Icon;
import io.wispforest.owo.itemgroup.OwoItemGroup;
import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.ops.LootOps;
import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.InvertedLootCondition;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.item.EnchantmentPredicate;
import net.minecraft.predicate.item.EnchantmentsPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.predicate.item.ItemSubPredicateTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.List;

public class Conjuring implements ModInitializer {

    public static final String MOD_ID = "conjuring";

    public static final com.glisco.conjuring.ConjuringConfig CONFIG = com.glisco.conjuring.ConjuringConfig.createAndLoad();

    public static final OwoItemGroup CONJURING_GROUP = OwoItemGroup
            .builder(Conjuring.id("conjuring"), () -> Icon.of(ConjuringBlocks.CONJURER))
            .build();

    public static final ScreenHandlerType<ConjurerScreenHandler> CONJURER_SCREEN_HANDLER_TYPE = new ScreenHandlerType<>(ConjurerScreenHandler::new, FeatureFlags.DEFAULT_ENABLED_FEATURES);
    public static final ScreenHandlerType<SoulfireForgeScreenHandler> SOULFIRE_FORGE_SCREEN_HANDLER_TYPE = new ScreenHandlerType<>(SoulfireForgeScreenHandler::new, FeatureFlags.DEFAULT_ENABLED_FEATURES);

    public static final EntityType<SoulProjectileEntity> SOUL_PROJECTILE
            = FabricEntityTypeBuilder.<SoulProjectileEntity>create(SpawnGroup.MISC, SoulProjectileEntity::new).dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build();
    public static final EntityType<SoulDiggerEntity> SOUL_DIGGER
            = FabricEntityTypeBuilder.<SoulDiggerEntity>create(SpawnGroup.MISC, SoulDiggerEntity::new).dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build();
    public static final EntityType<SoulHarvesterEntity> SOUL_HARVESTER
            = FabricEntityTypeBuilder.<SoulHarvesterEntity>create(SpawnGroup.MISC, SoulHarvesterEntity::new).dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build();
    public static final EntityType<SoulFellerEntity> SOUL_FELLER
            = FabricEntityTypeBuilder.<SoulFellerEntity>create(SpawnGroup.MISC, SoulFellerEntity::new).dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build();
    public static final EntityType<SoulMagnetEntity> SOUL_MAGNET
            = FabricEntityTypeBuilder.<SoulMagnetEntity>create(SpawnGroup.MISC, SoulMagnetEntity::new).dimensions(EntityDimensions.fixed(0.25f, 0.25f)).build();

    public static final SoundEvent WEEE = SoundEvent.of(Conjuring.id("block.soul_weaver.weee"));

    public static final ExtractionRitualCriterion EXTRACTION_RITUAL_CRITERION = new ExtractionRitualCriterion();
    public static final GemTinkeringCriterion GEM_TINKERING_CRITERION = new GemTinkeringCriterion();

    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(id("channel"));

    @Override
    public void onInitialize() {

        FieldRegistrationHandler.register(ConjuringBlocks.class, MOD_ID, false);
        FieldRegistrationHandler.register(ConjuringBlocks.Entities.class, MOD_ID, false);

        FieldRegistrationHandler.register(ConjuringItems.class, MOD_ID, false);

        Registry.register(Registries.RECIPE_SERIALIZER, SoulfireForgeRecipeSerializer.ID, SoulfireForgeRecipeSerializer.INSTANCE);
        Registry.register(Registries.RECIPE_TYPE, SoulfireForgeRecipe.Type.ID, SoulfireForgeRecipe.Type.INSTANCE);

        Registry.register(Registries.RECIPE_SERIALIZER, SoulWeaverRecipeSerializer.ID, SoulWeaverRecipeSerializer.INSTANCE);
        Registry.register(Registries.RECIPE_TYPE, SoulWeaverRecipe.Type.ID, SoulWeaverRecipe.Type.INSTANCE);

        Registry.register(Registries.RECIPE_SERIALIZER, GemTinkererRecipeSerializer.ID, GemTinkererRecipeSerializer.INSTANCE);
        Registry.register(Registries.RECIPE_TYPE, GemTinkererRecipe.Type.ID, GemTinkererRecipe.Type.INSTANCE);

        Registry.register(Registries.SCREEN_HANDLER, Conjuring.id("conjurer"), CONJURER_SCREEN_HANDLER_TYPE);
        Registry.register(Registries.SCREEN_HANDLER, Conjuring.id("soulfire_forge"), SOULFIRE_FORGE_SCREEN_HANDLER_TYPE);

        Registry.register(Registries.ENTITY_TYPE, Conjuring.id("soul_projectile"), SOUL_PROJECTILE);
        Registry.register(Registries.ENTITY_TYPE, Conjuring.id("soul_feller"), SOUL_FELLER);
        Registry.register(Registries.ENTITY_TYPE, Conjuring.id("soul_digger"), SOUL_DIGGER);
        Registry.register(Registries.ENTITY_TYPE, Conjuring.id("soul_magnet"), SOUL_MAGNET);
        Registry.register(Registries.ENTITY_TYPE, Conjuring.id("soul_harvester"), SOUL_HARVESTER);

        CONJURING_GROUP.initialize();

        ServerTickEvents.END_WORLD_TICK.register(BlockCrawler::tick);
        CHANNEL.registerServerbound(ChangeToolModePacket.class, (message, access) -> {
            var player = access.player();

            if (!player.isSneaking()) return;
            if (!(player.getMainHandStack().getItem() instanceof SoulAlloyTool)) return;
            SoulAlloyTool.toggleEnabledState(player.getMainHandStack());
            player.playSound(SoulAlloyTool.isSecondaryEnabled(player.getMainHandStack()) ? SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN : SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), 1, 1.5f + player.getWorld().random.nextFloat() * 0.5f);
        });

        Registry.register(Registries.SOUND_EVENT, Conjuring.id("block.soul_weaver.weee"), WEEE);

        final var spawnerLootTableId = Identifier.of("blocks/spawner");
        LootTableEvents.MODIFY.register((key, tableBuilder, source) -> {
            if (!spawnerLootTableId.equals(key.getValue())) return;


            var server = Owo.currentServer();
            if (server == null) return;

            var wrapper = server.getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);

            var itemEntry = ItemEntry.builder(ConjuringItems.CONJURATION_ESSENCE);
            if (CONFIG.conjurer_config.fortuneEnabled()) {
                itemEntry.apply(ApplyBonusLootFunction.oreDrops(wrapper.getOrThrow(Enchantments.FORTUNE)));
            }

            if (CONFIG.conjurer_config.respectSilkTouch()) {
                itemEntry.conditionally(InvertedLootCondition.builder(
                        MatchToolLootCondition.builder(ItemPredicate.Builder.create().subPredicate(
                                ItemSubPredicateTypes.ENCHANTMENTS, EnchantmentsPredicate.enchantments(
                                        List.of(new EnchantmentPredicate(wrapper.getOrThrow(Enchantments.SILK_TOUCH), NumberRange.IntRange.atLeast(1)))
                                )
                        )))
                );
            }

            tableBuilder.pool(LootPool.builder().with(itemEntry).build());
        });


        LootOps.injectItem(ConjuringItems.CONJURATION_ESSENCE, .35f, LootTables.SIMPLE_DUNGEON_CHEST.getValue());
        LootOps.injectItem(ConjuringItems.CONJURATION_ESSENCE, .175f, LootTables.BASTION_TREASURE_CHEST.getValue());
        LootOps.injectItem(ConjuringItems.CONJURATION_ESSENCE, .2f, LootTables.DESERT_PYRAMID_CHEST.getValue(), LootTables.STRONGHOLD_CORRIDOR_CHEST.getValue());
        LootOps.injectItem(ConjuringItems.CONJURATION_ESSENCE, .05f, LootTables.STRONGHOLD_LIBRARY_CHEST.getValue());

        Criteria.register("conjuring:extraction_ritual", EXTRACTION_RITUAL_CRITERION);
        Criteria.register("conjuring:gem_tinkering", GEM_TINKERING_CRITERION);

        SoulAlloyToolAbilities.registerCommonEvents();

        ConjuringParticleEvents.register();

        if (!Owo.DEBUG) return;
        CommandRegistrationCallback.EVENT.register(CreateConjuringFocusCommand::register);
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
