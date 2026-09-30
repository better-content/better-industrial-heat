package com.bettercontent.betterindustrialheat.mixin.create;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Removes Create's ambient, fuel-free boiler heat while preserving actively fueled burners. */
@Mixin(targets = "com.simibubi.create.content.fluids.tank.BoilerHeaters", remap = false)
abstract class BoilerHeatersMixin {
    @Inject(method = "passive", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void heatSync$rejectPassiveHeat(
            Level level,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<Integer> callback
    ) {
        callback.setReturnValue(BoilerHeater.NO_HEAT);
    }

    @Inject(method = "blazeBurner", at = @At("RETURN"), cancellable = true, require = 1, remap = false)
    private static void heatSync$requireActiveBurner(
            Level level,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<Integer> callback
    ) {
        BlazeBurnerBlock.HeatLevel heatLevel = state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
        if (!heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)) {
            callback.setReturnValue(BoilerHeater.NO_HEAT);
        }
    }
}
