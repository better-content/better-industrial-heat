package com.bettercontent.betterindustrialheat.mixin.minecraft;

import com.bettercontent.betterindustrialheat.food.FoodThermalService;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps furnace, smoker, campfire, and blasting recipe results from rejuvenating food. */
@Mixin(AbstractCookingRecipe.class)
abstract class AbstractCookingRecipeMixin {
    @Inject(method = "matches", at = @At("RETURN"), cancellable = true)
    private void heatSync$rejectDriedFood(Container container, net.minecraft.world.level.Level level,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (container.getContainerSize() > 0 && com.bettercontent.betterindustrialheat.HeatSyncThermalTags.isDriedFood(container.getItem(0))) {
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void heatSync$carryFoodState(
            Container container,
            RegistryAccess registries,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        FoodThermalService.carryCookingState(container.getItem(0), cir.getReturnValue());
    }
}
