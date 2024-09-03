package com.glisco.conjuring.util;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.items.ConjuringFocus;
import com.glisco.conjuring.items.ConjuringItems;
import io.wispforest.owo.client.screens.ScreenUtils;
import io.wispforest.owo.client.screens.SlotGenerator;
import io.wispforest.owo.client.screens.ValidatingSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class ConjurerScreenHandler extends ScreenHandler {

    private final Inventory inventory;

    public ConjurerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(5));
    }

    public ConjurerScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(Conjuring.CONJURER_SCREEN_HANDLER_TYPE, syncId);
        this.inventory = inventory;

        this.addSlot(new ValidatingSlot(this.inventory, 0, 77, 41, stack -> stack.getItem() instanceof ConjuringFocus && stack.contains(ConjuringFocus.ENTITY)));

        this.addSlot(new ValidatingSlot(this.inventory, 1, 77, 2, stack -> stack.getItem() == ConjuringItems.HASTE_CHARM));
        this.addSlot(new ValidatingSlot(this.inventory, 2, 117, 41, stack -> stack.getItem() == ConjuringItems.ABUNDANCE_CHARM));
        this.addSlot(new ValidatingSlot(this.inventory, 3, 77, 80, stack -> stack.getItem() == ConjuringItems.SCOPE_CHARM));
        this.addSlot(new ValidatingSlot(this.inventory, 4, 38, 41, stack -> stack.getItem() == ConjuringItems.IGNORANCE_CHARM));

        SlotGenerator.begin(this::addSlot, 8, 110).playerInventory(playerInventory);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        return ScreenUtils.handleSlotTransfer(this, invSlot, this.inventory.size());
    }

}
