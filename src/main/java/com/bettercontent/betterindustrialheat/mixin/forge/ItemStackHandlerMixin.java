package com.bettercontent.betterindustrialheat.mixin.forge;

import com.bettercontent.betterindustrialheat.food.FoodStackMergeService;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemStackHandler.class, remap = false)
abstract class ItemStackHandlerMixin {
    @Redirect(method = "insertItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;grow(I)V", remap = true))
    private void heatSync$averageCommittedInsert(ItemStack destination, int movedCount, int slot, ItemStack source, boolean simulate) {
        // Simulation must be observational: mutating thermal NBT here makes a
        // machine's admission probe consume age before any item is moved.
        if (simulate) {
            destination.grow(movedCount);
            return;
        }
        FoodStackMergeService.mergeInto(destination, destination.copy(), source.copy(), movedCount);
        destination.grow(movedCount);
    }
}
