package com.bettercontent.betterindustrialheat.mixin.create.client;

import com.bettercontent.betterindustrialheat.compat.ponder.ActiveFanProcessingPonderScene;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps Create's fan-processing storyboard accurate for Better Content's active-burner rule. */
@Mixin(targets = "com.simibubi.create.infrastructure.ponder.scenes.FanScenes", remap = false)
abstract class FanScenesMixin {
    @Inject(method = "processing", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void heatSync$showActiveBurnerScene(
            SceneBuilder builder,
            SceneBuildingUtil util,
            CallbackInfo callback
    ) {
        ActiveFanProcessingPonderScene.processing(builder, util);
        callback.cancel();
    }
}
