package com.bettercontent.betterindustrialheat.content.heat

import com.bettercontent.betterindustrialheat.AmbientHeatSampling
import com.bettercontent.betterindustrialheat.HeatSyncConfig
import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.PipeThermalStepMath
import com.bettercontent.betterindustrialheat.PipeThermalSourceResolver
import com.bettercontent.betterindustrialheat.HeatSyncPipeThermalController
import com.bettercontent.betterindustrialheat.api.HeatBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import kotlin.math.abs

/** Native block entities and the same extract-before-credit transfer used by the four-tick ticker.
 * Each case runs several complete transfer rounds synchronously, before environmental rounds can
 * inject/remove heat; controller/environmental behavior is qualified separately.
 */
@GameTestHolder(HeatSyncMod.MOD_ID)
@PrefixGameTestTemplate(false)
class HeatNetworkConservationGameTests {
    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun pairAndFullReceiver(helper: GameTestHelper) {
        val pipes = place(helper, listOf(BlockPos(0, 1, 1), BlockPos(1, 1, 1)))
        pipes[0].setHeat(pipes[0].maxHeat())
        pipes[1].setHeat(pipes[1].maxHeat())
        val before = pipes.map { it.getHeat() }
        HeatBlockEntity.transferAround(pipes[0])
        helper.assertTrue(pipes.map { it.getHeat() } == before, "Full receiver changed during no-op transfer")
        pipes[0].setHeat(300f)
        pipes[1].setHeat(100f)
        exercise(helper, pipes, 400f)
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun hotHubThreeColdLeaves(helper: GameTestHelper) {
        val pipes = star(helper)
        pipes[0].setHeat(300f)
        pipes.drop(1).forEach { it.setHeat(100f) }
        exercise(helper, pipes, 600f)
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun coldHubThreeHotLeaves(helper: GameTestHelper) {
        val pipes = star(helper)
        pipes[0].setHeat(100f)
        pipes.drop(1).forEach { it.setHeat(300f) }
        exercise(helper, pipes, 1000f)
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun unequalDegreeChain(helper: GameTestHelper) {
        val pipes = place(helper, listOf(BlockPos(0, 1, 0), BlockPos(1, 1, 0), BlockPos(2, 1, 0), BlockPos(2, 1, 1)))
        pipes[0].setHeat(300f)
        pipes.drop(1).forEach { it.setHeat(100f) }
        exercise(helper, pipes, 600f)
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun closedLoop(helper: GameTestHelper) {
        val pipes = place(helper, listOf(BlockPos(0, 1, 0), BlockPos(1, 1, 0), BlockPos(1, 1, 1), BlockPos(0, 1, 1)))
        pipes[0].setHeat(300f)
        pipes.drop(1).forEach { it.setHeat(100f) }
        exercise(helper, pipes, 600f)
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun unequalSurfaceCapacitiesRemainConservative(helper: GameTestHelper) {
        val pipes = place(helper, listOf(BlockPos(0, 1, 1), BlockPos(1, 1, 1)))
        val validated = CompoundTag().also { it.put("better_manufacturing:settings", CompoundTag()) }
        pipes[1].applySurface(1.5, 0.75, validated)
        helper.assertTrue(pipes[1].maxHeat() < pipes[0].maxHeat(), "Surface capacity was not reduced")
        pipes[0].setHeat(300f)
        pipes[1].setHeat(100f)
        exercise(helper, pipes, 400f)
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun boilerTopRejectsHeatButLateralSideAccepts(helper: GameTestHelper) {
        val center = BlockPos(1, 1, 1)
        helper.setBlock(center, com.bettercontent.betterindustrialheat.HeatSyncRegistries.BOILER_HEATER.get())
        val heater = requireNotNull(helper.getBlockEntity(center) as? BoilerHeaterBlockEntity)
        val top = place(helper, listOf(BlockPos(1, 2, 1))).single()
        heater.setHeat(100f)
        top.setHeat(300f)
        HeatBlockEntity.transferAround(top)
        HeatBlockEntity.transferAround(heater) // lower packed peer owns the top edge
        val topBlocked = heater.getHeat() == 100f && top.getHeat() == 300f
        val lateral = place(helper, listOf(BlockPos(2, 1, 1))).single()
        lateral.setHeat(300f)
        // Native peers settle an edge only from the lower packed position.
        HeatBlockEntity.transferAround(heater)
        helper.assertTrue(topBlocked, "Boiler reserved top accepted heat")
        helper.assertTrue(heater.getHeat() > 100f && lateral.getHeat() < 300f, "Lateral boiler input blocked")
        helper.assertTrue(abs(heater.getHeat() + top.getHeat() + lateral.getHeat() - 700f) < 0.02, "Side transfer lost heat")
        helper.succeed()
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun unloadedNeighborAndChangedEndpointsControlExchangeSync(helper: GameTestHelper) {
        val pipes = place(helper, listOf(BlockPos(0, 1, 1), BlockPos(1, 1, 1)))
        val sender = pipes[0]
        val receiver = pipes[1]
        sender.setHeat(300f)
        receiver.setHeat(100f)
        val updates = mutableListOf<BlockEntity>()
        val visited = mutableListOf<BlockPos>()
        val lookup: (BlockPos) -> BlockEntity? = { pos -> visited += pos; helper.level.getBlockEntity(pos) }
        val sync: (BlockEntity) -> Unit = { updates += it }
        HeatBlockEntity.transferAround(sender, { pos -> pos != receiver.blockPos }, lookup, sync)
        helper.assertTrue(receiver.blockPos !in visited && updates.isEmpty(), "Unloaded neighbor was queried or synced")
        helper.assertTrue(sender.getHeat() == 300f && receiver.getHeat() == 100f, "Unloaded neighbor exchanged heat")
        HeatBlockEntity.transferAround(sender, { true }, lookup, sync)
        helper.assertTrue(updates.count { it === sender } == 1 && updates.count { it === receiver } == 1,
            "Transfer did not sync exactly both changed endpoints")
        helper.assertTrue(abs(sender.getHeat() + receiver.getHeat() - 400f) < 0.02, "Loaded exchange lost heat")
        sender.setHeat(200f)
        receiver.setHeat(200f)
        updates.clear()
        HeatBlockEntity.transferAround(sender, { true }, lookup, sync)
        HeatBlockEntity.transferAround(receiver, { true }, lookup, sync)
        helper.succeedIf { helper.assertTrue(updates.isEmpty(), "Equilibrium emitted a heat-only update") }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun unloadedColdSourceIsNotReadAndLoadedSourceStillApplies(helper: GameTestHelper) {
        val origin = helper.absolutePos(BlockPos(1, 1, 1))
        val coldPos = origin.east()
        helper.level.setBlockAndUpdate(coldPos, Blocks.BLUE_ICE.defaultBlockState())
        val visited = mutableListOf<BlockPos>()
        val lookup: (BlockPos) -> net.minecraft.world.level.block.state.BlockState = { pos ->
            visited += pos
            helper.level.getBlockState(pos)
        }
        val skipped = PipeThermalSourceResolver.resolveAdjacentAverageTargetHeat(
            origin, { pos -> pos != coldPos }, lookup,
        )
        helper.assertTrue(coldPos !in visited && skipped == null, "Unloaded cold source was read")
        val loaded = PipeThermalSourceResolver.resolveAdjacentAverageTargetHeat(origin, { true }, lookup)
        helper.succeedIf {
            helper.assertTrue(coldPos in visited && loaded == HeatSyncConfig.blueIceSourceHeat(),
                "Loaded cold source was skipped")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun isolatedFireboxSyncsFuelBurnAndHeatOnlyWhenChanged(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        helper.setBlock(pos, com.bettercontent.betterindustrialheat.HeatSyncRegistries.THERMAL_FIREBOX.get())
        val firebox = requireNotNull(helper.getBlockEntity(pos) as? ThermalFireboxBlockEntity)
        val updates = mutableListOf<BlockEntity>()
        firebox.serverTick { updates += it }
        helper.assertTrue(updates.isEmpty(), "Idle firebox emitted an update")
        firebox.insertFuel(ItemStack(Items.COAL))
        firebox.serverTick { updates += it }
        helper.assertTrue(updates == listOf(firebox), "Burn start did not publish exactly one machine update")
        helper.assertTrue(firebox.updateTag.getInt("BurnTicks") > 0 && firebox.updateTag.getFloat("Heat") > 100f,
            "Fuel/heat missing from published firebox tag")
        updates.clear()
        firebox.serverTick { updates += it }
        helper.succeedIf { helper.assertTrue(updates == listOf(firebox), "Steady burn did not publish its heat/burn change") }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun steadyStrengthBoilerConsumptionStillSyncs(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        helper.setBlock(pos, com.bettercontent.betterindustrialheat.HeatSyncRegistries.BOILER_HEATER.get())
        val heater = requireNotNull(helper.getBlockEntity(pos) as? BoilerHeaterBlockEntity)
        val updates = mutableListOf<BlockEntity>()
        heater.setHeat(300f)
        heater.serverTick(false) { updates += it }
        helper.assertTrue(updates.isEmpty(), "Inactive boiler emitted an update")
        heater.serverTick(true) { updates += it }
        helper.assertTrue(updates == listOf(heater), "Active boiler did not publish state/heat")
        val heatAfterFirst = heater.getHeat()
        updates.clear()
        heater.serverTick(true) { updates += it }
        helper.assertTrue(heater.getHeat() < heatAfterFirst && heater.deliveredStrength() == 2,
            "Boiler did not consume at stable strength")
        helper.assertTrue(updates == listOf(heater) && heater.updateTag.getFloat("Heat") == heater.getHeat(),
            "Steady-strength boiler heat did not publish")
        updates.clear()
        heater.serverTick(false) { updates += it }
        helper.assertTrue(updates == listOf(heater), "Boiler deactivation not published")
        updates.clear()
        heater.serverTick(false) { updates += it }
        helper.succeedIf { helper.assertTrue(updates.isEmpty(), "Idle boiler emitted an update") }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun savedHeatAndReenrollmentKeepTransferWorking(helper: GameTestHelper) {
        val pipes = place(helper, listOf(BlockPos(0, 1, 1), BlockPos(1, 1, 1)))
        pipes[0].setHeat(300f)
        pipes[1].setHeat(100f)
        val saved = pipes[0].saveWithoutMetadata()
        pipes[0].setHeat(100f)
        pipes[0].load(saved)
        val pos = pipes[0].blockPos
        HeatSyncPipeThermalController.unenroll(helper.level, pos)
        pipes[0].onLoad() // mirrors chunk reattachment; no world or registry reset
        HeatBlockEntity.transferAround(pipes[0])
        helper.succeedIf {
            helper.assertTrue(HeatSyncPipeThermalController.isEnrolled(helper.level, pos), "Reattached pipe not scheduled")
            helper.assertTrue(pipes[0].getHeat() < 300f && pipes[1].getHeat() > 100f, "Saved heat could not transfer")
            helper.assertTrue(abs(pipes.sumOf { it.getHeat().toDouble() } - 400.0) < 0.02, "Save/reload duplicated heat")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun environmentalRoundRetainsAmbientColdSourceAndLoss(helper: GameTestHelper) {
        val pipe = place(helper, listOf(BlockPos(1, 1, 1))).single()
        helper.setBlock(BlockPos(2, 1, 1), Blocks.BLUE_ICE)
        pipe.setHeat(300f)
        val expected = PipeThermalStepMath.step(
            pipeHeat = 300.0,
            ambientHeat = AmbientHeatSampling.samplePipeHeat(helper.level, pipe.blockPos),
            neighborAverage = null,
            sourceHeat = HeatSyncConfig.blueIceSourceHeat(),
            ambientBlendRate = 0.1, networkEqualizationStrength = 0.0,
            coldSourcePullRate = 0.2, pipeLossPerTick = 0.5,
            minPipeHeat = HeatSyncConfig.pipeMinHeat(), maxPipeHeat = HeatSyncConfig.pipeMaxHeat(),
        )
        HeatSyncPipeThermalController.applyEnvironmentalRound(helper.level, listOf(pipe), 0.1, 0.2, 0.5)
        helper.succeedIf {
            helper.assertTrue(abs(pipe.getHeat() - expected) < 0.02, "Environmental round lost an input: ${pipe.getHeat()} != $expected")
        }
    }

    private fun star(helper: GameTestHelper) = place(helper, listOf(
        BlockPos(1, 1, 1), BlockPos(0, 1, 1), BlockPos(2, 1, 1), BlockPos(1, 1, 0),
    ))

    private fun place(helper: GameTestHelper, positions: List<BlockPos>) = positions.map { pos ->
        helper.setBlock(pos, com.bettercontent.betterindustrialheat.HeatSyncRegistries.HEAT_PIPE.get())
        requireNotNull(helper.getBlockEntity(pos) as? HeatPipeBlockEntity)
    }

    private fun exercise(helper: GameTestHelper, pipes: List<HeatPipeBlockEntity>, expected: Float) {
        var moved = false
        val first = pipes.map { it.getHeat() }
        repeat(12) { round ->
            // The real transferAround path is called on each pipe every four server ticks.
            pipes.forEach { HeatBlockEntity.transferAround(it) }
            if ((round + 1) % 5 == 0) {
                // The same controller method runs every 20 ticks; zero external terms
                // isolate conservation without changing global config or other GameTests.
                HeatSyncPipeThermalController.applyEnvironmentalRound(helper.level, pipes, 0.0, 0.0, 0.0)
            }
            if (pipes.map { it.getHeat() } != first) moved = true
            val actual = pipes.sumOf { it.getHeat().toDouble() }
            helper.assertTrue(abs(actual - expected) < 0.02, "Native network created/lost heat: $actual != $expected")
            pipes.forEach { pipe ->
                helper.assertTrue(pipe.getHeat() >= 0f && pipe.getHeat() <= pipe.maxHeat(), "Pipe exceeded its bounds")
            }
        }
        helper.succeedIf { helper.assertTrue(moved, "No native neighbor transfer occurred") }
    }
}
