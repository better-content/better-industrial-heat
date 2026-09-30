package com.bettercontent.betterindustrialheat.mixin.minecraft;

import com.bettercontent.betterindustrialheat.food.FoodStackMergeService;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
abstract class SlotMixin {
    @Unique private static final ThreadLocal<MergeCapture> HEAT_SYNC_CAPTURE = new ThreadLocal<>();

    @Inject(method = "safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void heatSync$captureInsert(ItemStack source, int requestedCount, CallbackInfoReturnable<ItemStack> cir) {
        HEAT_SYNC_CAPTURE.remove();
        ItemStack destination = ((Slot) (Object) this).getItem();
        if (!destination.isEmpty() && FoodStackMergeService.canMerge(destination, source)) {
            HEAT_SYNC_CAPTURE.set(new MergeCapture(destination.copy(), source.copy()));
        }
    }

    @Redirect(
            method = "safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameTags(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z")
    )
    private boolean heatSync$allowThermalFoodMerge(ItemStack destination, ItemStack source) {
        return FoodStackMergeService.canMerge(destination, source);
    }

    @Redirect(
            method = "safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;grow(I)V")
    )
    private void heatSync$averageCommittedInsert(
            ItemStack destination,
            int movedCount,
            ItemStack source,
            int requestedCount
    ) {
        MergeCapture capture = HEAT_SYNC_CAPTURE.get();
        HEAT_SYNC_CAPTURE.remove();
        if (capture != null) {
            FoodStackMergeService.mergeInto(destination, capture.destination(), capture.source(), movedCount);
        }
        destination.grow(movedCount);
    }

    @Inject(method = "safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void heatSync$clearInsertCapture(ItemStack source, int requestedCount, CallbackInfoReturnable<ItemStack> cir) {
        HEAT_SYNC_CAPTURE.remove();
    }

    @Unique private record MergeCapture(ItemStack destination, ItemStack source) {}
}
