package com.bettercontent.betterindustrialheat.mixin.minecraft;

import com.bettercontent.betterindustrialheat.food.FoodStackMergeService;
import com.bettercontent.betterindustrialheat.food.FoodThermalService;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
abstract class InventoryMixin {
    @Accessor("player")
    abstract Player heatSync$getPlayer();

    @Inject(method = "setChanged", at = @At("TAIL"))
    private void heatSync$reconcileFoodAfterChange(CallbackInfo ci) {
        FoodThermalService.onPlayerInventoryChanged(heatSync$getPlayer());
    }

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
