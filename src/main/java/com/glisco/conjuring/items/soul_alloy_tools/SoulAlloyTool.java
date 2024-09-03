package com.glisco.conjuring.items.soul_alloy_tools;

import com.glisco.conjuring.Conjuring;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import io.wispforest.endec.Endec;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

public interface SoulAlloyTool {

    ComponentType<ModifiersComponent> MODIFIERS = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Conjuring.id("soul_alloy_tool_modifiers"),
            ComponentType.<ModifiersComponent>builder()
                    .codec(CodecUtils.toCodec(ModifiersComponent.ENDEC))
                    .packetCodec(CodecUtils.toPacketCodec(ModifiersComponent.ENDEC))
                    .build()
    );

    ComponentType<Boolean> SECONDARY_ENABLED = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Conjuring.id("soul_alloy_tool_secondary"),
            ComponentType.<Boolean>builder()
                    .codec(Codec.BOOL)
                    .packetCodec(PacketCodecs.BOOL)
                    .build()
    );

    default boolean canAoeDig() {
        return false;
    }

    default Predicate<BlockState> getAoeToolOverridePredicate() {
        return SoulAlloyToolAbilities.NO_TOOL_OVERRIDE;
    }

    static void toggleEnabledState(ItemStack stack) {
        stack.apply(SECONDARY_ENABLED, false, enabled -> !enabled);
    }

    static boolean isSecondaryEnabled(ItemStack stack) {
        return stack.getOrDefault(SECONDARY_ENABLED, false);
    }

    static void addModifier(ItemStack stack, SoulAlloyModifier modifier) {
        var modifiers = stack.getOrDefault(MODIFIERS, ModifiersComponent.DEFAULT).modifiers;

        int level = modifiers.getOrDefault(modifier, 0);
        level++;

        stack.set(MODIFIERS, new ModifiersComponent(ImmutableMap.<SoulAlloyModifier, Integer>builder().putAll(modifiers).put(modifier, level).buildKeepingLast()));
    }

    static boolean canAddModifier(ItemStack stack, SoulAlloyModifier modifier) {
        var modifiers = stack.getOrDefault(MODIFIERS, ModifiersComponent.DEFAULT).modifiers;

        if (modifiers.size() >= 2 && getModifierLevel(stack, modifier) == 0) return false;

        if (getModifierLevel(stack, modifier) >= 3) return false;

        return modifiers.values().stream().mapToInt(Integer::intValue).sum() < 5;
    }

    static boolean canAddModifiers(ItemStack stack, List<SoulAlloyModifier> modifiers) {

        var modifierMap = new HashMap<>(stack.getOrDefault(MODIFIERS, ModifiersComponent.DEFAULT).modifiers);

        for (SoulAlloyModifier modifier : modifiers) {
            if (!modifierMap.containsKey(modifier)) {
                modifierMap.put(modifier, 1);
            } else {
                modifierMap.put(modifier, modifierMap.get(modifier) + 1);
            }
        }

        for (var entry : modifierMap.entrySet()) {
            if (entry.getValue() < 3) continue;
            if (entry.getValue() > 3) return false;
            if (modifierMap.entrySet().stream().anyMatch(currentEntry -> currentEntry != entry && currentEntry.getValue() > 2))
                return false;
        }

        return modifierMap.size() <= 2;

    }

    static List<Text> getTooltip(ItemStack stack) {
        if (!stack.contains(MODIFIERS)) return List.of();

        var tooltip = new ArrayList<Text>();
        var modifiers = stack.get(MODIFIERS).modifiers;

        modifiers.forEach((modifier, level) -> {
            var levelString = "●".repeat(Math.max(0, level));
            tooltip.add(Text.translatable(modifier.translation_key).styled(style -> style.withColor(modifier.textColor)).append(": ").append(TextOps.withFormatting(levelString, Formatting.GRAY)));
        });

        if (!tooltip.isEmpty() && stack.hasEnchantments()) {
            tooltip.add(Text.empty());
        }

        return tooltip;
    }

    static int getModifierLevel(ItemStack stack, SoulAlloyModifier modifier) {
        return stack.getOrDefault(MODIFIERS, ModifiersComponent.DEFAULT).modifiers.getOrDefault(modifier, 0);
    }

    static int getModifierLevel(ComponentMap components, SoulAlloyModifier modifier) {
        return components.getOrDefault(MODIFIERS, ModifiersComponent.DEFAULT).modifiers.getOrDefault(modifier, 0);
    }

    enum SoulAlloyModifier {

        HASTE(0x007a18, "modifier.conjuring.haste"),
        ABUNDANCE(0xa80f01, "modifier.conjuring.abundance"),
        SCOPE(0x4d8184, "modifier.conjuring.scope"),
        IGNORANCE(0x123f89, "modifier.conjuring.ignorance");

        public final int textColor;
        public final String translation_key;

        SoulAlloyModifier(int textColor, String translation_key) {
            this.textColor = textColor;
            this.translation_key = translation_key;
        }
    }

    record ModifiersComponent(ImmutableMap<SoulAlloyModifier, Integer> modifiers) {
        public static final ModifiersComponent DEFAULT = new ModifiersComponent(ImmutableMap.of());

        public static final Endec<ModifiersComponent> ENDEC = Endec.map(SoulAlloyModifier::name, SoulAlloyModifier::valueOf, Endec.INT)
                .xmap(map -> new ModifiersComponent(ImmutableMap.copyOf(map)), ModifiersComponent::modifiers);
    }

}
