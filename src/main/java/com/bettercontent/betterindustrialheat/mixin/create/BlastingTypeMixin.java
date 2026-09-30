package com.bettercontent.betterindustrialheat.mixin.create;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restricts Create's built-in bulk blasting to air passing through an actively fueled Blaze Burner. */
@Mixin(
        targets = "com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes$BlastingType",
        remap = false
)
abstract class BlastingTypeMixin {
    @Inject(method = "isValidAt", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void heatSync$requireActiveBurner(
            Level level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> callback
    ) {
        BlockState state = level.getBlockState(pos);
        boolean activeBurner = state.is(AllBlocks.BLAZE_BURNER.get())
                && state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)
                && state.getValue(BlazeBurnerBlock.HEAT_LEVEL)
                        .isAtLeast(BlazeBurnerBlock.HeatLevel.FADING);
        callback.setReturnValue(activeBurner);
    }
}
