package com.bettercontent.betterindustrialheat.mixin.hexerei;

import com.bettercontent.betterindustrialheat.food.FoodThermalService;
import com.bettercontent.betterindustrialheat.food.FoodItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Carries age and temperature from a rack input into its explicitly catalogued output. */
@Mixin(targets = "net.joefoxe.hexerei.data.recipes.DryingRackRecipe", remap = false)
abstract class DryingRackRecipeMixin {
    @Inject(
            method = "matches(Lnet/minecraft/world/SimpleContainer;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void heatSync$rejectStaleRawFood(SimpleContainer container, Level level,
                                              CallbackInfoReturnable<Boolean> callback) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack input = container.getItem(slot);
            if (FoodItems.isDriedFoodSource(input) && !FoodThermalService.canDryAt(input, level.getGameTime())) {
                callback.setReturnValue(false);
                return;
            }
        }
    }

    @Inject(method = "assemble(Lnet/minecraft/world/SimpleContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void heatSync$carryFoodState(SimpleContainer container, RegistryAccess registries,
                                          CallbackInfoReturnable<ItemStack> cir) {
        FoodThermalService.carryDryingState(container.getItem(0), cir.getReturnValue());
    }
}
