package com.bettercontent.betterindustrialheat.compat.pneumaticcraft

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.api.HeatBlockEntity
import com.bettercontent.betterindustrialheat.api.HeatStorageThermalBody
import com.bettercontent.betterindustrialheat.api.IThermalBody
import me.desht.pneumaticcraft.api.PNCCapabilities
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.eventbus.api.IEventBus
import java.util.function.BiPredicate
import kotlin.math.roundToInt

/** Exposes Better Industrial Heat block entities as conservative PneumaticCraft thermal bodies. */
object PneumaticHeatBridge {
    const val MOD_ID = "pneumaticcraft"
    private val ID = ResourceLocation.fromNamespaceAndPath(HeatSyncMod.MOD_ID, "pneumatic_heat")

    fun initialize(forgeBus: IEventBus) {
        forgeBus.addGenericListener(BlockEntity::class.java, ::attachCapabilities)
    }

    internal fun attachCapabilities(event: AttachCapabilitiesEvent<BlockEntity>) {
        val heat = event.`object` as? HeatBlockEntity ?: return
        val provider = PneumaticHeatProvider(HeatStorageThermalBody(heat))
        event.addCapability(ID, provider)
        event.addListener(provider::invalidate)
    }

    private class PneumaticHeatProvider(
        heat: IThermalBody,
    ) : ICapabilityProvider {
        private val logic: LazyOptional<IHeatExchangerLogic> = LazyOptional.of {
            HeatSyncPneumaticLogic(heat)
        }

        override fun <T : Any> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> =
            if (cap === PNCCapabilities.HEAT_EXCHANGER_CAPABILITY) logic.cast() else LazyOptional.empty()

        fun invalidate() {
            logic.invalidate()
        }
    }

    internal class HeatSyncPneumaticLogic(
        private val storage: IThermalBody,
    ) : IHeatExchangerLogic {
        private var thermalResistance = 1.0
        private var thermalCapacity = 1.0

        override fun tick() = Unit

        override fun initializeAsHull(
            level: Level,
            pos: BlockPos,
            blocks: BiPredicate<LevelAccessor, BlockPos>,
            vararg validSides: Direction,
        ) = Unit

        override fun initializeAmbientTemperature(level: Level, pos: BlockPos) = Unit

        override fun setTemperature(temperature: Double) {
            storage.setTemperatureKelvin(temperature)
        }

        override fun getTemperature(): Double = storage.temperatureKelvin()

        override fun getTemperatureAsInt(): Int = temperature.roundToInt()

        override fun getAmbientTemperature(): Double = 295.15

        override fun setThermalResistance(resistance: Double) {
            thermalResistance = resistance.coerceAtLeast(0.0)
        }

        override fun getThermalResistance(): Double =
            maxOf(thermalResistance, 1.0 / storage.conductanceHUPerKTick(null).coerceAtLeast(0.0001))

        override fun setThermalCapacity(capacity: Double) {
            thermalCapacity = capacity.coerceAtLeast(0.0001)
        }

        override fun getThermalCapacity(): Double =
            maxOf(thermalCapacity, storage.heatCapacityHUPerK())

        override fun addHeat(amount: Double) {
            if (amount > 0) {
                storage.insertHeatHU(amount)
            } else if (amount < 0) {
                storage.extractHeatHU(-amount)
            }
        }

        override fun isSideConnected(side: Direction): Boolean = storage.canConnect(side)
    }
}
