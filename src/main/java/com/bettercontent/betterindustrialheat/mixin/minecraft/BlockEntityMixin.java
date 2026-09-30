package com.bettercontent.betterindustrialheat.mixin.minecraft;

import com.bettercontent.betterindustrialheat.food.FoodThermalService;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
abstract class BlockEntityMixin {
    @Inject(method = "setChanged", at = @At("TAIL"))
    private void heatSync$reconcileFoodAfterChange(CallbackInfo ci) {
        FoodThermalService.onBlockEntityChanged((BlockEntity) (Object) this);
    }
}
