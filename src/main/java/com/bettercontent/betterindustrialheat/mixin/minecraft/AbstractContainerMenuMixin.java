package com.bettercontent.betterindustrialheat.mixin.minecraft;

import com.bettercontent.betterindustrialheat.food.FoodStackMergeService;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuMixin {
    @Unique private static final ThreadLocal<MergeCapture> HEAT_SYNC_CAPTURE = new ThreadLocal<>();

    @Inject(method = "moveItemStackTo", at = @At("HEAD"))
    private void heatSync$clearQuickMoveCapture(ItemStack source, int startIndex, int endIndex, boolean reverseDirection, CallbackInfoReturnable<Boolean> cir) {
        HEAT_SYNC_CAPTURE.remove();
    }

    @Redirect(
            method = "moveItemStackTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameTags(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z")
    )
    private boolean heatSync$allowThermalFoodMerge(ItemStack source, ItemStack destination) {
        boolean compatible = FoodStackMergeService.canMerge(source, destination);
        if (compatible) {
            HEAT_SYNC_CAPTURE.set(new MergeCapture(destination.copy(), source.copy()));
        }
        return compatible;
    }

    @Redirect(method = "moveItemStackTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setCount(I)V", ordinal = 1))
    private void heatSync$averageCompleteQuickMove(ItemStack destination, int newCount, ItemStack source, int startIndex, int endIndex, boolean reverseDirection) {
        averageAndSet(destination, newCount, source);
    }

    @Redirect(method = "moveItemStackTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setCount(I)V", ordinal = 2))
    private void heatSync$averagePartialQuickMove(ItemStack destination, int newCount, ItemStack source, int startIndex, int endIndex, boolean reverseDirection) {
        averageAndSet(destination, newCount, source);
    }

    private static void averageAndSet(ItemStack destination, int newCount, ItemStack source) {
        int movedCount = newCount - destination.getCount();
        MergeCapture capture = HEAT_SYNC_CAPTURE.get();
        HEAT_SYNC_CAPTURE.remove();
        if (capture != null) {
            FoodStackMergeService.mergeInto(destination, capture.destination(), capture.source(), movedCount);
        }
        destination.setCount(newCount);
    }

    @Inject(method = "moveItemStackTo", at = @At("RETURN"))
    private void heatSync$clearQuickMoveCaptureOnReturn(ItemStack source, int startIndex, int endIndex, boolean reverseDirection, CallbackInfoReturnable<Boolean> cir) {
        HEAT_SYNC_CAPTURE.remove();
    }

    @Unique private record MergeCapture(ItemStack destination, ItemStack source) {}
}
