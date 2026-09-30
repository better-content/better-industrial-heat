package com.bettercontent.betterindustrialheat.mixin.minecraft;

import com.bettercontent.betterindustrialheat.food.FoodStackMergeService;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
    @Unique private static final ThreadLocal<MergeCapture> HEAT_SYNC_CAPTURE = new ThreadLocal<>();

    @Inject(method = "areMergable", at = @At("RETURN"), cancellable = true)
    private static void heatSync$allowThermalFoodMerge(
            ItemStack first,
            ItemStack second,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ()
                && first.getCount() + second.getCount() <= second.getMaxStackSize()
                && FoodStackMergeService.canMerge(first, second)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "merge(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private static void heatSync$captureEntityMerge(
            ItemStack destination,
            ItemStack source,
            int limit,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        HEAT_SYNC_CAPTURE.set(new MergeCapture(destination.copy(), source.copy()));
    }

    @Inject(method = "merge(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private static void heatSync$averageMergedEntityStack(
            ItemStack destination,
            ItemStack source,
            int limit,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        MergeCapture capture = HEAT_SYNC_CAPTURE.get();
        HEAT_SYNC_CAPTURE.remove();
        if (capture == null) return;
        int movedCount = cir.getReturnValue().getCount() - capture.destination().getCount();
        FoodStackMergeService.mergeInto(cir.getReturnValue(), capture.destination(), capture.source(), movedCount);
    }

    @Unique private record MergeCapture(ItemStack destination, ItemStack source) {}
}
