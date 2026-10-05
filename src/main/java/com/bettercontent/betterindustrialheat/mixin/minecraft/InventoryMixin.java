package com.bettercontent.betterindustrialheat.mixin.minecraft;

import com.bettercontent.betterindustrialheat.food.FoodStackMergeService;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.entity.player.Inventory;

@Mixin(Inventory.class)
abstract class InventoryMixin {
    @Redirect(
            method = "hasRemainingSpaceForItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameTags(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z")
    )
    private boolean heatSync$allowThermalFoodMerge(ItemStack destination, ItemStack source) {
        return FoodStackMergeService.canMerge(destination, source);
    }

    @Redirect(
            method = "addResource(ILnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;grow(I)V")
    )
    private void heatSync$averageCommittedInventoryInsert(
            ItemStack destination,
            int movedCount,
            int slot,
            ItemStack source
    ) {
        FoodStackMergeService.mergeInto(destination, destination.copy(), source.copy(), movedCount);
        destination.grow(movedCount);
    }
}
