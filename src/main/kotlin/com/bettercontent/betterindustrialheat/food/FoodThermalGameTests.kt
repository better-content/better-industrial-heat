package com.bettercontent.betterindustrialheat.food

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import com.bettercontent.betterindustrialheat.content.heat.ConstantTemperatureBlockEntity
import com.bettercontent.betterindustrialheat.content.heat.ThermalFireboxBlockEntity
import com.bettercontent.betterindustrialheat.mixin.minecraft.RandomizableContainerBlockEntityAccessor
import com.mojang.authlib.GameProfile
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.IItemHandlerModifiable
import net.minecraftforge.items.ItemStackHandler
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

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
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
        foodState(frozenApple, decay = 0.0, lastTime = 0, rate = 0.0)
        FoodThermalService.tick(frozenApple, FoodThermalService.Storage.FROZEN, 24_000)

        val warmApple = ItemStack(Items.APPLE)
        foodState(warmApple, decay = 0.0, lastTime = 0, rate = 1.0)
        FoodThermalService.tick(warmApple, FoodThermalService.Storage.AMBIENT, 23_999)
        helper.assertTrue(FoodThermalService.stage(warmApple) == FoodThermalService.Stage.FRESH, "Ordinary food became harmful before 24,000 active ticks")
        FoodThermalService.tick(warmApple, FoodThermalService.Storage.AMBIENT, 24_000)

        helper.succeedIf {
            helper.assertTrue(decay(frozenApple) == 0.0, "Frozen food must not accumulate spoilage")
            helper.assertTrue(decay(warmApple) == 1.0, "Ordinary warm food must reach harmful spoilage at 24,000 active ticks")
            helper.assertTrue(FoodThermalService.stage(warmApple) == FoodThermalService.Stage.STALE, "First harmful stage must begin at 24,000 active ticks")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun longGapsAdvanceAtMostOneStagePerSettle(helper: GameTestHelper) {
        val apple = ItemStack(Items.APPLE)
        foodState(apple, decay = 0.0, lastTime = 0)
        FoodThermalService.tick(apple, FoodThermalService.Storage.AMBIENT, 240_000)
        val afterFirstGap = decay(apple)
        FoodThermalService.tick(apple, FoodThermalService.Storage.AMBIENT, 480_000)

        helper.succeedIf {
            helper.assertTrue(afterFirstGap == 1.0, "A long gap must settle to exactly one stage, not jump to ruined")
            helper.assertTrue(
                decay(apple) == 1.5,
                "A further settle must advance exactly one more stage and drop the uncharged backlog",
            )
            helper.assertTrue(FoodThermalService.stage(apple) == FoodThermalService.Stage.SPOILED, "Stage must follow the capped decay")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun offlineCarriedFoodDoesNotAgeAcrossSessions(helper: GameTestHelper) {
        val owner = java.util.UUID.randomUUID()
        val player = FakePlayerFactory.get(helper.level, GameProfile(owner, "food-owner"))
        val bags = CarriedBagCapability()
        bags.register()
        try {
            val logoutTime = 500_000L
            val pocket = ItemStack(Items.APPLE)
            foodState(pocket, decay = 0.3, lastTime = logoutTime)
            player.inventory.items[0] = pocket

            val ender = ItemStack(Items.APPLE)
            foodState(ender, decay = 0.4, lastTime = logoutTime)
            player.enderChestInventory.setItem(0, ender)

            val insideBag = ItemStack(Items.APPLE)
            foodState(insideBag, decay = 0.5, lastTime = logoutTime)
            bags.handler.setStackInSlot(0, insideBag)
            val bag = ItemStack(Items.CHEST)
            bag.orCreateTag.putBoolean("carried_bag", true)
            player.inventory.items[1] = bag

            // Logout settles the final online interval and rebases; login rebases again.
            FoodThermalService.settleCarriedFood(player, logoutTime)
            FoodThermalService.rebaseCarriedFood(player, logoutTime)
            val loginTime = 5_000_000L
            FoodThermalService.rebaseCarriedFood(player, loginTime)
            FoodThermalService.settleCarriedFood(player, loginTime)

            helper.succeedIf {
                helper.assertTrue(decay(pocket) == 0.3, "Offline pocket food must not age across sessions")
                helper.assertTrue(decay(ender) == 0.4, "Offline ender-chest food must not age across sessions")
                helper.assertTrue(decay(insideBag) == 0.5, "Offline food inside a carried bag must not age across sessions")
                helper.assertTrue(
                    lastTime(insideBag) == loginTime,
                    "The carried-food walk must reach bag item handlers and rebase their timestamps",
                )
            }
        } finally {
            bags.unregister()
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun cookingCarriesOnlyThermalFoodState(helper: GameTestHelper) {
        val raw = ItemStack(Items.BEEF)
        foodState(raw, decay = 0.6, lastTime = 400)
        val cooked = ItemStack(Items.COOKED_BEEF)
        cooked.orCreateTag.putString("recipe_marker", "kept")

        FoodThermalService.carryCookingState(raw, cooked)

        helper.succeedIf {
            helper.assertTrue(decay(cooked) == 0.6, "Cooking must retain accumulated food age")
            helper.assertTrue(cooked.tag?.getString("recipe_marker") == "kept", "Cooking must retain result-owned NBT")
            helper.assertTrue(decay(raw) == 0.6, "Cooking must not mutate its input remainder")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun repeatedUpdatesDoNotRoundAwayElapsedFoodAge(helper: GameTestHelper) {
        val apple = ItemStack(Items.APPLE)
        foodState(apple, decay = 0.0, lastTime = 0)
        for (time in 1L..24_000L) FoodThermalService.tick(apple, FoodThermalService.Storage.AMBIENT, time)
        helper.succeedIf {
            helper.assertTrue(decay(apple) >= 1.0 - 1.0e-9, "Per-update rounding erased ordinary spoilage")
            helper.assertTrue(FoodThermalService.stage(apple) == FoodThermalService.Stage.STALE, "Repeated updates changed the harmful-stage boundary")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun coldStorageUsesOneTenthActiveSpoilageRate(helper: GameTestHelper) {
        val chilledApple = ItemStack(Items.APPLE)
        FoodThermalService.tick(chilledApple, FoodThermalService.Storage.COLD, 0)
        FoodThermalService.tick(chilledApple, FoodThermalService.Storage.COLD, 24_000)

        helper.succeedIf {
            helper.assertTrue(
                decay(chilledApple) == 0.1,
                "Cold-stored food must retain one tenth, rather than zero, active spoilage rate",
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun lateColdStorageCannotEraseWarmTime(helper: GameTestHelper) {
        val apple = ItemStack(Items.APPLE)
        FoodThermalService.tick(apple, FoodThermalService.Storage.AMBIENT, 0)
        FoodThermalService.tick(apple, FoodThermalService.Storage.COLD, 24_000)

        helper.succeedIf {
            helper.assertTrue(
                decay(apple) == 1.0,
                "Elapsed warm time must settle at the rate it was spent under, not the new cold rate",
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun foodTintIntensifiesThroughSpoilageStages(helper: GameTestHelper) {
        val food = ItemStack(Items.APPLE)
        foodState(food, decay = 0.0)
        val fresh = FoodThermalService.itemTint(food)
        foodState(food, decay = 1.0)
        val stale = FoodThermalService.itemTint(food)
        foodState(food, decay = 1.5)
        val spoiled = FoodThermalService.itemTint(food)
        foodState(food, decay = 2.0)
        val rotten = FoodThermalService.itemTint(food)
        foodState(food, decay = 2.0, rate = 0.0)
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
        foodState(destination, decay = 0.2, lastTime = 100)

        val source = destination.copy().also {
            it.count = 2
            foodState(it, decay = 0.8, lastTime = 100)
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
                kotlin.math.abs(decay(output) - 0.35) < 1.0e-9,
                "A weighted merge must fold decay by destination count and moved count",
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
        foodState(frozenApple, decay = 0.0, rate = 0.0)
        val thawedApple = ItemStack(Items.APPLE)
        foodState(thawedApple, decay = 0.0, rate = 1.0)
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

    class CarriedBagCapability {
        val handler: ItemStackHandler = ItemStackHandler(2)

        fun register() {
            current = this
            if (!listenerRegistered) {
                listenerRegistered = true
                MinecraftForge.EVENT_BUS.addGenericListener(ItemStack::class.java, ::attach)
            }
        }

        fun unregister() {
            current = null
        }

        private class BagProvider(handler: ItemStackHandler) : ICapabilityProvider {
            private val bag = LazyOptional.of<IItemHandler> { handler }
            override fun <T : Any> getCapability(capability: Capability<T>, side: Direction?): LazyOptional<T> =
                if (capability === ForgeCapabilities.ITEM_HANDLER) bag.cast() else LazyOptional.empty()
        }

        companion object {
            private var current: CarriedBagCapability? = null
            private var listenerRegistered = false

            /** Adopts the one fixture stack the offline test marks as a carried bag. */
            private fun attach(event: AttachCapabilitiesEvent<ItemStack>) {
                val fixture = current ?: return
                if (event.`object`.tag?.getBoolean("carried_bag") != true) return
                event.addCapability(ResourceLocation(HeatSyncMod.MOD_ID, "test_bag"), BagProvider(fixture.handler))
            }
        }
    }

    private fun foodState(
        stack: ItemStack,
        decay: Double = 0.0,
        lastTime: Long = 0,
        rate: Double = 1.0,
        warmSince: Long = 0,
    ) {
        stack.orCreateTag.put("better_industrial_heat_food", CompoundTag().also {
            it.putInt("version", 5)
            it.putDouble("decay", decay)
            it.putLong("last_time", lastTime)
            it.putDouble("rate", rate)
            it.putLong("warm_since", warmSince)
        })
    }

    private fun withoutThermalState(stack: ItemStack): CompoundTag? =
        stack.tag?.copy()?.also { it.remove("better_industrial_heat_food") }?.takeUnless { it.isEmpty }

    private fun decay(stack: ItemStack): Double = stack.tag!!.getCompound("better_industrial_heat_food").getDouble("decay")

    private fun lastTime(stack: ItemStack): Long = stack.tag!!.getCompound("better_industrial_heat_food").getLong("last_time")
}
