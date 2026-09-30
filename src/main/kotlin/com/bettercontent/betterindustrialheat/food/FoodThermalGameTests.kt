package com.bettercontent.betterindustrialheat.food

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import com.bettercontent.betterindustrialheat.content.heat.ConstantTemperatureBlockEntity
import com.bettercontent.betterindustrialheat.content.heat.ThermalFireboxBlockEntity
import com.bettercontent.betterindustrialheat.mixin.minecraft.RandomizableContainerBlockEntityAccessor
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.items.IItemHandlerModifiable
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.registries.ForgeRegistries

@GameTestHolder(HeatSyncMod.MOD_ID)
@PrefixGameTestTemplate(false)
class FoodThermalGameTests {
    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun chestNearColdAndHeatSourcesFreezesAndThaws(helper: GameTestHelper) {
        val chestPos = BlockPos(2, 1, 2)
        val sourcePos = chestPos.west()
        helper.setBlock(chestPos, Blocks.CHEST)
        val chest = requireNotNull(helper.getBlockEntity(chestPos) as? ChestBlockEntity)
        val worldChestPos = chest.blockPos
        chest.setItem(0, ItemStack(Items.COOKED_BEEF))

        helper.setBlock(sourcePos, HeatSyncRegistries.CREATIVE_COLD_SOURCE.get())
        val cold = requireNotNull(helper.getBlockEntity(sourcePos) as? ConstantTemperatureBlockEntity)
        ConstantTemperatureBlockEntity.tick(helper.level, cold.blockPos, cold.blockState, cold)
        helper.assertTrue((FoodThermalService.adjacentThermalTarget(helper.level, worldChestPos) ?: 9999.0) <= 0.0, "Cold source was not exposed as a 0 K thermal target")
        FoodThermalService.tickContainer(helper.level, worldChestPos, chest, 0)
        FoodThermalService.tickContainer(helper.level, worldChestPos, chest, 2_000)
        helper.assertTrue(FoodThermalService.isFrozen(chest.getItem(0)), "Food in a chest beside a cold source must freeze")

        helper.setBlock(sourcePos, HeatSyncRegistries.CREATIVE_HEAT_SOURCE.get())
        val heat = requireNotNull(helper.getBlockEntity(sourcePos) as? ConstantTemperatureBlockEntity)
        heat.setHeat(10_000f)
        helper.assertTrue((FoodThermalService.adjacentThermalTarget(helper.level, worldChestPos) ?: 0.0) > 600.0, "Heat source was not exposed as a hot thermal target")
        FoodThermalService.tickContainer(helper.level, worldChestPos, chest, 4_000)
        FoodThermalService.tickContainer(helper.level, worldChestPos, chest, 6_000)

        helper.succeedIf {
            helper.assertTrue(!FoodThermalService.isFrozen(chest.getItem(0)), "Food in a chest beside a heat source must thaw")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun activatedBarrelReconcilesFoodWithoutInventoryScheduler(helper: GameTestHelper) {
        val barrelPos = BlockPos(2, 1, 2)
        helper.setBlock(barrelPos, Blocks.BARREL)
        helper.setBlock(barrelPos.west(), Blocks.PACKED_ICE)
        val barrel = requireNotNull(helper.getBlockEntity(barrelPos) as? BarrelBlockEntity)
        barrel.setItem(0, ItemStack(Items.COOKED_BEEF))

        FoodThermalService.activateInventory(barrel, reconcileNow = true)
        FoodThermalService.tickContainer(helper.level, barrel.blockPos, barrel, 4_000)

        helper.succeedIf {
            helper.assertTrue(FoodThermalService.isActivated(barrel), "A gameplay inventory must retain its thermal activation marker")
            helper.assertTrue(FoodThermalService.isFrozen(barrel.getItem(0)), "Packed ice beside an activated barrel must freeze food")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 40)
    fun untouchedWorldgenStyleInventoryDoesNotActivateFromMutation(helper: GameTestHelper) {
        val pos = BlockPos(2, 1, 2)
        helper.setBlock(pos, Blocks.BARREL)
        val barrel = requireNotNull(helper.getBlockEntity(pos) as? BarrelBlockEntity)
        barrel.setLootTable(net.minecraft.resources.ResourceLocation("minecraft", "chests/simple_dungeon"), 17L)
        helper.assertTrue(
            (barrel as RandomizableContainerBlockEntityAccessor).heatSyncLootTable != null,
            "The generated-loot fixture must start with an unresolved loot table",
        )
        FoodThermalService.onBlockEntityChanged(barrel)
        helper.succeedIf {
            helper.assertTrue(!FoodThermalService.isActivated(barrel), "An untouched inventory must remain thermally dormant")
            helper.assertTrue(
                (barrel as RandomizableContainerBlockEntityAccessor).heatSyncLootTable != null,
                "The inventory update handler must not unpack the generated loot table",
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun machineItemHandlerOutputActivatesThermalTracking(helper: GameTestHelper) {
        val pos = BlockPos(4, 1, 2)
        helper.setBlock(pos, HeatSyncRegistries.THERMAL_FIREBOX.get())
        val firebox = requireNotNull(helper.getBlockEntity(pos) as? ThermalFireboxBlockEntity)
        val inventory = firebox.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve()
            .orElseThrow { IllegalStateException("Thermal firebox item capability was unavailable") }
            as IItemHandlerModifiable

        // setStackInSlot models a completed machine output after its recipe has admitted it.
        inventory.setStackInSlot(0, ItemStack(Items.APPLE))

        helper.succeedIf {
            helper.assertTrue(FoodThermalService.isActivated(firebox), "A machine food output must activate thermal tracking")
            helper.assertTrue(
                inventory.getStackInSlot(0).tag?.contains("better_industrial_heat_food") == true,
                "A machine food output must receive thermal state on admission",
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun frozenFoodPausesSpoilage(helper: GameTestHelper) {
        val frozenApple = ItemStack(Items.APPLE)
        thermalState(frozenApple, temperature = 268.15)
        FoodThermalService.tick(frozenApple, 268.15, 24_000)

        val warmApple = ItemStack(Items.APPLE)
        thermalState(warmApple, temperature = 295.15)
        FoodThermalService.tick(warmApple, 295.15, 23_999)
        helper.assertTrue(FoodThermalService.stage(warmApple) == FoodThermalService.Stage.FRESH, "Ordinary food became harmful before 24,000 active ticks")
        FoodThermalService.tick(warmApple, 295.15, 24_000)

        helper.succeedIf {
            helper.assertTrue(decay(frozenApple) == 0.0, "Frozen food must not accumulate spoilage")
            helper.assertTrue(decay(warmApple) == 1.0, "Ordinary warm food must reach harmful spoilage at 24,000 active ticks")
            helper.assertTrue(FoodThermalService.stage(warmApple) == FoodThermalService.Stage.STALE, "First harmful stage must begin at 24,000 active ticks")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun cookingCarriesOnlyThermalFoodState(helper: GameTestHelper) {
        val raw = ItemStack(Items.BEEF)
        thermalState(raw, temperature = 278.15, decay = 0.6, lastTime = 400)
        val cooked = ItemStack(Items.COOKED_BEEF)
        cooked.orCreateTag.putString("recipe_marker", "kept")

        FoodThermalService.carryCookingState(raw, cooked)

        helper.succeedIf {
            helper.assertTrue(decay(cooked) == 0.6, "Cooking must retain accumulated food age")
            helper.assertTrue(FoodThermalService.temperatureK(cooked) == 278.15, "Cooking must retain physical food temperature")
            helper.assertTrue(cooked.tag?.getString("recipe_marker") == "kept", "Cooking must retain result-owned NBT")
            helper.assertTrue(decay(raw) == 0.6, "Cooking must not mutate its input remainder")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun repeatedUpdatesDoNotRoundAwayElapsedFoodAge(helper: GameTestHelper) {
        val apple = ItemStack(Items.APPLE)
        thermalState(apple, temperature = 295.15)
        for (time in 1L..24_000L) FoodThermalService.tick(apple, 295.15, time)
        helper.succeedIf {
            helper.assertTrue(decay(apple) >= 1.0 - 1.0e-9, "Per-update rounding erased ordinary spoilage")
            helper.assertTrue(FoodThermalService.stage(apple) == FoodThermalService.Stage.STALE, "Repeated updates changed the harmful-stage boundary")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun refrigerationUsesOneTenthActiveSpoilageRate(helper: GameTestHelper) {
        val chilledApple = ItemStack(Items.APPLE)
        thermalState(chilledApple, temperature = 278.15)
        FoodThermalService.tick(chilledApple, 278.15, 24_000)

        helper.succeedIf {
            helper.assertTrue(
                decay(chilledApple) == 0.1,
                "Refrigerated food must retain one tenth, rather than zero, active spoilage rate",
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun lazyRefrigeratorTransitionPersistsItsNewRate(helper: GameTestHelper) {
        val apple = ItemStack(Items.APPLE)
        thermalState(apple, temperature = 295.15)

        // The first update settles the old warm category through tick zero, then persists
        // the refrigerator category for the interval while the container is unloaded.
        FoodThermalService.tick(apple, 278.15, 0)
        FoodThermalService.tick(apple, 278.15, 24_000)

        helper.succeedIf {
            helper.assertTrue(
                decay(apple) == 0.1,
                "A refrigerator transition must persist 0.1× for its following lazy interval",
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun foodTintIntensifiesThroughSpoilageStages(helper: GameTestHelper) {
        val food = ItemStack(Items.APPLE)
        thermalState(food, temperature = 295.15)
        food.tag!!.getCompound("better_industrial_heat_food").putDouble("decay", 0.0)
        val fresh = FoodThermalService.itemTint(food)
        food.tag!!.getCompound("better_industrial_heat_food").putDouble("decay", 1.0)
        val stale = FoodThermalService.itemTint(food)
        food.tag!!.getCompound("better_industrial_heat_food").putDouble("decay", 1.5)
        val spoiled = FoodThermalService.itemTint(food)
        food.tag!!.getCompound("better_industrial_heat_food").putDouble("decay", 2.0)
        val rotten = FoodThermalService.itemTint(food)
        food.tag!!.getCompound("better_industrial_heat_food").putInt("temperature_bucket_c", -1)
        val frozen = FoodThermalService.itemTint(food)

        helper.succeedIf {
            helper.assertTrue(fresh == 0xFFFFFF, "Fresh food must retain its native colour")
            helper.assertTrue(stale == 0xD9C9AA && spoiled == 0x916D43 && rotten == 0x392416, "Spoilage tint must intensify through each stage")
            helper.assertTrue(frozen == 0x9DDCFF, "Frozen tint must override spoilage with an explicit ice-blue tint")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun staleFoodDebuffsLastTenSeconds(helper: GameTestHelper) {
        helper.succeedIf {
            helper.assertTrue(FoodThermalService.debuffDurationTicks(FoodThermalService.Stage.STALE) == 200, "Stale-food debuffs must last 10 seconds")
            helper.assertTrue(FoodThermalService.debuffDurationTicks(FoodThermalService.Stage.SPOILED) == 1200, "More severe food stages retain their one-minute duration")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun weightedMergePreservesOrdinaryIdentityAndNonThermalData(helper: GameTestHelper) {
        val destination = ItemStack(Items.APPLE, 3)
        destination.orCreateTag.putString("better_content_quality", "orchard")
        destination.orCreateTag.put("better_content_details", CompoundTag().also { it.putInt("grade", 2) })
        thermalState(destination, temperature = 273.15, decay = 0.2, lastTime = 100)

        val source = destination.copy().also {
            it.count = 2
            thermalState(it, temperature = 373.15, decay = 0.8, lastTime = 100)
        }
        val sourceBefore = source.copy()
        val destinationNonThermal = withoutThermalState(destination)
        val destinationThermal = destination.tag!!.getCompound("better_industrial_heat_food").copy()
        val output = destination.copy()

        FoodStackMergeService.mergeInto(output, destination, source, movedCount = 1)

        helper.succeedIf {
            helper.assertTrue(
                ForgeRegistries.ITEMS.getKey(output.item).toString() == "minecraft:apple",
                "A weighted merge must retain the ordinary destination item ID",
            )
            helper.assertTrue(
                withoutThermalState(output) == destinationNonThermal,
                "A weighted merge must leave every non-thermal NBT value unchanged",
            )
            helper.assertTrue(
                ItemStack.matches(source, sourceBefore),
                "Computing a partial merge must not mutate the source remainder",
            )
            helper.assertTrue(
                output.tag!!.getCompound("better_industrial_heat_food") != destinationThermal,
                "A weighted merge must recompute only better_industrial_heat_food",
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun foodThreadEpisodeRequiresFrozenThenFreshUseOfSameOrdinaryItem(helper: GameTestHelper) {
        val frozenApple = ItemStack(Items.APPLE)
        thermalState(frozenApple, temperature = 268.15)
        val thawedApple = ItemStack(Items.APPLE)
        thermalState(thawedApple, temperature = 295.15)
        val staleApple = thawedApple.copy().also {
            it.tag!!.getCompound("better_industrial_heat_food").putDouble("decay", 1.0)
        }

        helper.succeedIf {
            helper.assertTrue(FoodThermalEpisodes.isNonNeutral(frozenApple), "Frozen edible use must reveal a non-neutral episode")
            helper.assertTrue(!FoodThermalEpisodes.isAppropriate(frozenApple), "Frozen food is not appropriate to consume")
            helper.assertTrue(FoodThermalEpisodes.isAppropriate(thawedApple), "Thawed fresh food is appropriate to consume")
            helper.assertTrue(!FoodThermalEpisodes.isAppropriate(staleApple), "Stale food is not an appropriate completion")
            helper.assertTrue(
                FoodThermalEpisodes.sameOrdinaryItem("minecraft:apple", thawedApple),
                "Completion must retain the reveal episode's ordinary item identity",
            )
            helper.assertTrue(
                !FoodThermalEpisodes.sameOrdinaryItem("minecraft:carrot", thawedApple),
                "A different ordinary item identity must not complete the episode",
            )
        }
    }

    private fun thermalState(
        stack: ItemStack,
        temperature: Double,
        decay: Double = 0.0,
        lastTime: Long = 0,
    ) {
        val state = stack.orCreateTag.getCompound("better_industrial_heat_food")
        val bucket = kotlin.math.round((temperature - 273.15) / 5.0).toInt()
        state.putInt("version", 3)
        state.putInt("temperature_bucket_c", bucket)
        state.putInt("last_target_bucket_c", bucket)
        state.putBoolean("last_target_appliance", false)
        state.putDouble("preservation_rate", when {
            temperature <= 273.15 -> 0.0
            temperature <= 278.15 -> 0.1
            else -> 1.0
        })
        state.putDouble("decay", decay)
        state.putLong("last_time", lastTime)
        stack.orCreateTag.put("better_industrial_heat_food", state)
    }

    private fun withoutThermalState(stack: ItemStack): CompoundTag? =
        stack.tag?.copy()?.also { it.remove("better_industrial_heat_food") }?.takeUnless { it.isEmpty }

    private fun decay(stack: ItemStack): Double = stack.tag!!.getCompound("better_industrial_heat_food").getDouble("decay")
}
