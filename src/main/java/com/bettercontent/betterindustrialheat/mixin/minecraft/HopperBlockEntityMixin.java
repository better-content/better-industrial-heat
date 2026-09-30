package com.bettercontent.betterindustrialheat.mixin.minecraft;

import com.bettercontent.betterindustrialheat.food.FoodStackMergeService;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
abstract class HopperBlockEntityMixin {
    @Unique private static final ThreadLocal<MergeCapture> HEAT_SYNC_CAPTURE = new ThreadLocal<>();

    @Inject(method = "canMergeItems", at = @At("RETURN"), cancellable = true)
    private static void heatSync$allowThermalFoodMerge(ItemStack destination, ItemStack source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && destination.getCount() < destination.getMaxStackSize()
                && FoodStackMergeService.canMerge(destination, source)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tryMoveInItem", at = @At("HEAD"))
    private static void heatSync$captureHopperInsert(
            Container sourceContainer,
            Container destinationContainer,
            ItemStack source,
            int slot,
            Direction direction,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        HEAT_SYNC_CAPTURE.remove();
        ItemStack destination = destinationContainer.getItem(slot);
        if (!destination.isEmpty() && FoodStackMergeService.canMerge(destination, source)) {
            HEAT_SYNC_CAPTURE.set(new MergeCapture(destination.copy(), source.copy()));
        }
    }

    @Redirect(method = "tryMoveInItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;grow(I)V"))
    private static void heatSync$averageCommittedHopperInsert(
            ItemStack destination,
            int movedCount,
            Container sourceContainer,
            Container destinationContainer,
            ItemStack source,
            int slot,
            Direction direction
    ) {
        MergeCapture capture = HEAT_SYNC_CAPTURE.get();
        HEAT_SYNC_CAPTURE.remove();
        if (capture != null) {
            FoodStackMergeService.mergeInto(destination, capture.destination(), capture.source(), movedCount);
        }
        destination.grow(movedCount);
    }

    @Inject(method = "tryMoveInItem", at = @At("RETURN"))
    private static void heatSync$clearHopperCapture(
            Container sourceContainer,
            Container destinationContainer,
            ItemStack source,
            int slot,
            Direction direction,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        HEAT_SYNC_CAPTURE.remove();
    }

    @Unique private record MergeCapture(ItemStack destination, ItemStack source) {}
}
