package com.bettercontent.betterindustrialheat.content.heat

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import com.bettercontent.betterindustrialheat.api.HeatCapabilities
import com.simibubi.create.api.boiler.BoilerHeater
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate

@GameTestHolder(HeatSyncMod.MOD_ID)
@PrefixGameTestTemplate(false)
class BoilerHeaterGameTests {
    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun exposesHeatInputExceptOnReservedTop(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        val heater = placeHeater(helper, pos)
        val side = heater.getCapability(HeatCapabilities.HEAT, Direction.NORTH).resolve()
            .orElseThrow { IllegalStateException("Boiler Heater side heat capability was unavailable") }

        side.addHeat(80f, false)

        helper.succeedIf {
            helper.assertTrue(heater.getHeat() == 180f, "Expected side heat input to reach 180, found ${heater.getHeat()}")
            helper.assertTrue(
                !heater.getCapability(HeatCapabilities.HEAT, Direction.UP).isPresent,
                "Top heat capability must be reserved for the Create boiler",
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun persistsHeatAndPresentationState(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        val heater = placeHeater(helper, pos)
        val input = CompoundTag().also {
            it.putFloat("Heat", 345f)
            it.putInt("LastDeliveredStrength", 3)
            it.putBoolean("Active", true)
        }
        heater.load(input)
        val saved = heater.saveWithoutMetadata()

        helper.succeedIf {
            helper.assertTrue(saved.getFloat("Heat") == 345f, "Stored heat did not persist")
            helper.assertTrue(saved.getInt("LastDeliveredStrength") == 3, "Delivered strength did not persist")
            helper.assertTrue(saved.getBoolean("Active"), "Active presentation state did not persist")
            helper.assertTrue(heater.comparatorOutput() == 12, "Expected comparator output 12")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun isRegisteredWithCreateBoilers(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        placeHeater(helper, pos)
        helper.succeedIf {
            helper.assertTrue(
                BoilerHeater.REGISTRY.get(HeatSyncRegistries.BOILER_HEATER.get()) != null,
                "Boiler Heater was not registered in Create's supported registry",
            )
            helper.assertTrue(
                BoilerHeater.findHeat(helper.level, helper.absolutePos(pos), helper.getBlockState(pos)) ==
                    BoilerHeater.NO_HEAT.toFloat(),
                "An inactive Boiler Heater must report NO_HEAT, not Create passive heat",
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun consumesOnlyForAnActiveCreateBoilerRequest(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        val heater = placeHeater(helper, pos)
        heater.setHeat(400f)
        helper.setBlock(pos.above(), AllBlocks.FLUID_TANK.get())
        val tank = requireNotNull(helper.getBlockEntity(pos.above()) as? FluidTankBlockEntity)
        tank.boiler.attachedEngines = 1

        BoilerHeaterBlockEntity.tick(helper.level, helper.absolutePos(pos), helper.getBlockState(pos), heater)

        helper.succeedIf {
            helper.assertTrue(heater.getHeat() == 397f, "Strength-three request should consume exactly three heat")
            helper.assertTrue(heater.deliveredStrength() == 3, "Expected delivered strength three")
            helper.assertTrue(
                BoilerHeater.findHeat(helper.level, helper.absolutePos(pos), helper.getBlockState(pos)) == 3f,
                "Create callback did not advertise the successfully delivered strength",
            )
        }
    }

    private fun placeHeater(helper: GameTestHelper, pos: BlockPos): BoilerHeaterBlockEntity {
        helper.setBlock(pos, HeatSyncRegistries.BOILER_HEATER.get())
        return requireNotNull(helper.getBlockEntity(pos) as? BoilerHeaterBlockEntity)
    }
}
