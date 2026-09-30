package com.bettercontent.betterindustrialheat.mixin.forge;

import com.bettercontent.betterindustrialheat.food.FoodStackMergeService;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemHandlerHelper.class, remap = false)
abstract class ItemHandlerHelperMixin {
    @Inject(method = {"canItemStacksStack", "canItemStacksStackRelaxed"}, at = @At("RETURN"), cancellable = true)
    private static void heatSync$allowThermalFoodMerge(
            ItemStack first,
            ItemStack second,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ() && FoodStackMergeService.canMerge(first, second)) {
            cir.setReturnValue(true);
        }
    }
}
