package com.glisco.conjuring.mixin;

import com.glisco.conjuring.blocks.ConjuringBlocks;
import com.glisco.conjuring.blocks.soulfire_forge.SoulfireForgeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.block.dispenser.DispenserBehavior$17"})
public class DispenserBehaviorMixin {

    @Inject(method = "dispenseSilently(Lnet/minecraft/util/math/BlockPointer;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    public void checkFroge(BlockPointer pointer, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        Direction direction = pointer.world().getBlockState(pointer.pos()).get(DispenserBlock.FACING);
        BlockState state = pointer.world().getBlockState(pointer.pos().offset(direction));
        if (!state.getBlock().equals(ConjuringBlocks.SOULFIRE_FORGE)) return;

        if (state.get(SoulfireForgeBlock.BURNING)) return;

        pointer.world().setBlockState(pointer.pos().offset(direction), state.with(SoulfireForgeBlock.BURNING, true));
        stack.damage(1, pointer.world(), null, item -> {});

        cir.setReturnValue(stack.getDamage() > stack.getMaxDamage() ? ItemStack.EMPTY : stack);
    }
}
