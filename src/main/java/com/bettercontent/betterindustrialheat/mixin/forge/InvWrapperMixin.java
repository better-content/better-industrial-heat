package com.bettercontent.betterindustrialheat.mixin.forge;

import com.bettercontent.betterindustrialheat.food.FoodStackMergeService;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InvWrapper.class, remap = false)
abstract class InvWrapperMixin {
    @Unique private static final ThreadLocal<MergeCapture> HEAT_SYNC_CAPTURE = new ThreadLocal<>();

    @Shadow public abstract Container getInv();

    @Inject(method = "insertItem", at = @At("HEAD"))
    private void heatSync$captureInsert(int slot, ItemStack source, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        HEAT_SYNC_CAPTURE.remove();
        ItemStack destination = getInv().getItem(slot);
        if (!simulate && !destination.isEmpty() && FoodStackMergeService.canMerge(destination, source)) {
            HEAT_SYNC_CAPTURE.set(new MergeCapture(slot, destination.copy(), source.copy()));
        }
    }

    @Inject(method = "insertItem", at = @At("RETURN"))
    private void heatSync$averageCommittedInsert(int slot, ItemStack source, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        MergeCapture capture = HEAT_SYNC_CAPTURE.get();
        HEAT_SYNC_CAPTURE.remove();
        if (capture == null || capture.slot() != slot) return;
        int movedCount = capture.source().getCount() - cir.getReturnValue().getCount();
        FoodStackMergeService.mergeInto(getInv().getItem(slot), capture.destination(), capture.source(), movedCount);
    }

    @Unique private record MergeCapture(int slot, ItemStack destination, ItemStack source) {}
}
