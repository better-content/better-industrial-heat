package com.bettercontent.betterindustrialheat.mixin.food;

import com.bettercontent.betterindustrialheat.food.RecipeInputGate;
import com.bettercontent.betterindustrialheat.HeatSyncThermalTags;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Prevents dried foods from being rejuvenated by Farmer's Delight's separate cooking-pot recipe path. */
@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.crafting.CookingPotRecipe", remap = false)
abstract class FarmersDelightCookingPotRecipeMixin {
    @Inject(
            method = "matches(Lnet/minecraftforge/items/wrapper/RecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void heatSync$rejectDriedFood(RecipeWrapper wrapper, net.minecraft.world.level.Level level,
                                           CallbackInfoReturnable<Boolean> callback) {
        List<ItemStack> inputs = new ArrayList<>();
        for (int slot = 0; slot < wrapper.getContainerSize(); slot++) inputs.add(wrapper.getItem(slot));
        Predicate<ItemStack> isDriedFood = HeatSyncThermalTags::isDriedFood;
        if (RecipeInputGate.rejects(inputs, isDriedFood)) callback.setReturnValue(false);
    }
}
