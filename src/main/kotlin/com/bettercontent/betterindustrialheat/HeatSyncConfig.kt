package com.bettercontent.betterindustrialheat

import com.bettercontent.betterindustrialheat.content.heat.BoilerHeaterSettings
import net.minecraftforge.common.ForgeConfigSpec

object HeatSyncConfig {
    private val builder = ForgeConfigSpec.Builder()

    private val absoluteZeroOffsetValue: ForgeConfigSpec.DoubleValue
    private val csToHeatScaleValue: ForgeConfigSpec.DoubleValue
    private val pipeMinHeatValue: ForgeConfigSpec.DoubleValue
    private val pipeMaxHeatValue: ForgeConfigSpec.DoubleValue

    private val ambientBlendRateValue: ForgeConfigSpec.DoubleValue
    private val pipeLossPerTickValue: ForgeConfigSpec.DoubleValue
    private val networkEqualizationStrengthValue: ForgeConfigSpec.DoubleValue
    private val coldSourcePullRateValue: ForgeConfigSpec.DoubleValue

    private val iceSourceHeatValue: ForgeConfigSpec.DoubleValue
    private val packedIceSourceHeatValue: ForgeConfigSpec.DoubleValue
    private val blueIceSourceHeatValue: ForgeConfigSpec.DoubleValue
    private val taggedColdSourceDefaultHeatValue: ForgeConfigSpec.DoubleValue

    private val pipeBlockTempRangeValue: ForgeConfigSpec.DoubleValue
    private val pipeBlockTempMaxEffectValue: ForgeConfigSpec.DoubleValue

    private val powerGridAmbientHeatValue: ForgeConfigSpec.DoubleValue
    private val powerGridAmbientTemperatureValue: ForgeConfigSpec.DoubleValue
    private val powerGridHeatPerDegreeValue: ForgeConfigSpec.DoubleValue
    private val powerGridMaxHeatValue: ForgeConfigSpec.DoubleValue
    private val fireboxHeatPerTickValue: ForgeConfigSpec.DoubleValue
    private val fireboxTargetHeatValue: ForgeConfigSpec.DoubleValue
    private val pneumaticAmbientHeatValue: ForgeConfigSpec.DoubleValue
    private val pneumaticAmbientTemperatureValue: ForgeConfigSpec.DoubleValue
    private val pneumaticHeatPerKelvinValue: ForgeConfigSpec.DoubleValue
    private val transducerFeCapacityValue: ForgeConfigSpec.IntValue
    private val transducerFePerUnitValue: ForgeConfigSpec.IntValue
    private val transducerUnitsPerTickValue: ForgeConfigSpec.IntValue
    private val transducerMaxHeatValue: ForgeConfigSpec.DoubleValue
    private val boilerHeaterMaxHeatValue: ForgeConfigSpec.DoubleValue
    private val boilerHeaterFirstThresholdValue: ForgeConfigSpec.DoubleValue
    private val boilerHeaterSecondThresholdValue: ForgeConfigSpec.DoubleValue
    private val boilerHeaterThirdThresholdValue: ForgeConfigSpec.DoubleValue
    private val boilerHeaterFirstCostValue: ForgeConfigSpec.DoubleValue
    private val boilerHeaterSecondCostValue: ForgeConfigSpec.DoubleValue
    private val boilerHeaterThirdCostValue: ForgeConfigSpec.DoubleValue

    val SPEC: ForgeConfigSpec

    init {
        builder.push("mapping")
        absoluteZeroOffsetValue = builder
            .comment("Better Industrial Heat heat value that corresponds to Cold Sweat 0 MC / 0 C.")
            .defineInRange("absolute_zero_offset", 100.0, 0.0, Double.MAX_VALUE)
        csToHeatScaleValue = builder
            .comment("How many Better Industrial Heat heat units correspond to one Cold Sweat MC unit.")
            .defineInRange("cs_to_heat_scale", 40.0, 0.0001, Double.MAX_VALUE)
        pipeMinHeatValue = builder
            .comment("Minimum pipe heat. Zero is absolute zero in the bridge model.")
            .defineInRange("pipe_min_heat", 0.0, 0.0, Double.MAX_VALUE)
        pipeMaxHeatValue = builder
            .comment("Maximum pipe heat.")
            .defineInRange("pipe_max_heat", 400.0, 0.0, Double.MAX_VALUE)
        builder.pop()

        builder.push("thermal_firebox")
        fireboxHeatPerTickValue = builder
            .comment("HeatSync heat released per burning tick. Fuel duration remains the furnace burn duration.")
            .defineInRange("heat_per_tick", 6.0, 0.0, Double.MAX_VALUE)
        fireboxTargetHeatValue = builder
            .comment("Firebox target heat. It stops consuming new fuel at or above this value.")
            .defineInRange("target_heat", 360.0, 0.0, Double.MAX_VALUE)
        builder.pop()

        builder.push("pneumaticcraft")
        pneumaticAmbientHeatValue = builder
            .comment("HeatSync heat corresponding to PneumaticCraft ambient temperature.")
            .defineInRange("ambient_heat", 100.0, 0.0, Double.MAX_VALUE)
        pneumaticAmbientTemperatureValue = builder
            .comment("PneumaticCraft ambient temperature in Kelvin.")
            .defineInRange("ambient_temperature_k", 295.15, 0.0, Double.MAX_VALUE)
        pneumaticHeatPerKelvinValue = builder
            .comment("HeatSync heat units per PneumaticCraft Kelvin.")
            .defineInRange("heat_per_kelvin", 0.25, 0.0001, Double.MAX_VALUE)
        builder.pop()

        builder.push("impossible_transducer")
        transducerFeCapacityValue = builder
            .comment("Containment-only Forge Energy buffer. FE is never converted into AE.")
            .defineInRange("fe_capacity", 100_000, 1, Int.MAX_VALUE)
        transducerFePerUnitValue = builder
            .comment("Containment FE consumed per finite source-owned unbinding unit.")
            .defineInRange("fe_per_unit", 2_000, 1, Int.MAX_VALUE)
        transducerUnitsPerTickValue = builder
            .comment("Fixed maximum unbinding units per transducer per tick; scale by parallel blocks.")
            .defineInRange("units_per_tick", 1, 1, 64)
        transducerMaxHeatValue = builder
            .comment("Heat at which the transducer melts into lava after retaining unbinding heat.")
            .defineInRange("max_heat", 400.0, 1.0, Double.MAX_VALUE)
        builder.pop()

        builder.push("pipe_behavior")
        ambientBlendRateValue = builder
            .comment("How strongly local Cold Sweat ambient pulls pipe heat toward its baseline each update.")
            .defineInRange("ambient_blend_rate", 0.08, 0.0, 1.0)
        pipeLossPerTickValue = builder
            .comment("Passive loss toward Better Industrial Heat neutral per update.")
            .defineInRange("pipe_loss_per_tick", 0.5, 0.0, Double.MAX_VALUE)
        networkEqualizationStrengthValue = builder
            .comment("How strongly connected neighbors pull a pipe toward their average heat.")
            .defineInRange("network_equalization_strength", 0.30, 0.0, 1.0)
        coldSourcePullRateValue = builder
            .comment("How strongly adjacent cold sources pull a pipe toward their source heat.")
            .defineInRange("cold_source_pull_rate", 0.18, 0.0, 1.0)
        builder.pop()

        builder.push("boiler_heater")
        boilerHeaterMaxHeatValue = builder
            .comment("Maximum heat stored by a Boiler Heater.")
            .defineInRange("max_heat", 400.0, 100.0, Double.MAX_VALUE)
        boilerHeaterFirstThresholdValue = builder.defineInRange("strength_1_threshold", 180.0, 0.0, Double.MAX_VALUE)
        boilerHeaterSecondThresholdValue = builder.defineInRange("strength_2_threshold", 260.0, 0.0, Double.MAX_VALUE)
        boilerHeaterThirdThresholdValue = builder.defineInRange("strength_3_threshold", 340.0, 0.0, Double.MAX_VALUE)
        boilerHeaterFirstCostValue = builder.defineInRange("strength_1_cost_per_tick", 1.0, 0.0, Double.MAX_VALUE)
        boilerHeaterSecondCostValue = builder.defineInRange("strength_2_cost_per_tick", 2.0, 0.0, Double.MAX_VALUE)
        boilerHeaterThirdCostValue = builder.defineInRange("strength_3_cost_per_tick", 3.0, 0.0, Double.MAX_VALUE)
        builder.pop()

        builder.push("cold_sources")
        iceSourceHeatValue = builder
            .comment("Target Better Industrial Heat heat for adjacent ice.")
            .defineInRange("ice_source_heat", 80.0, 0.0, Double.MAX_VALUE)
        packedIceSourceHeatValue = builder
            .comment("Target Better Industrial Heat heat for adjacent packed ice.")
            .defineInRange("packed_ice_source_heat", 50.0, 0.0, Double.MAX_VALUE)
        blueIceSourceHeatValue = builder
            .comment("Target Better Industrial Heat heat for adjacent blue ice.")
            .defineInRange("blue_ice_source_heat", 20.0, 0.0, Double.MAX_VALUE)
        taggedColdSourceDefaultHeatValue = builder
            .comment("Target heat for any other block in better_industrial_heat:pipe_cold_sources.")
            .defineInRange("tagged_cold_source_default_heat", 80.0, 0.0, Double.MAX_VALUE)
        builder.pop()

        builder.push("radiation")
        pipeBlockTempRangeValue = builder
            .comment("How far pipe-emitted Cold Sweat block temperature reaches.")
            .defineInRange("pipe_blocktemp_range", 5.0, 0.0, Double.MAX_VALUE)
        pipeBlockTempMaxEffectValue = builder
            .comment("Absolute Cold Sweat MC temperature cap emitted by a pipe.")
            .defineInRange("pipe_blocktemp_max_effect", 0.75, 0.0, Double.MAX_VALUE)
        builder.pop()

        builder.push("power_grid")
        powerGridAmbientHeatValue = builder
            .comment("HeatSync heat value corresponding to Power Grid ambient device temperature.")
            .defineInRange("ambient_heat", 100.0, 0.0, Double.MAX_VALUE)
        powerGridAmbientTemperatureValue = builder
            .comment("Power Grid ambient device temperature in Celsius.")
            .defineInRange("ambient_temperature_c", 22.0, -273.15, Double.MAX_VALUE)
        powerGridHeatPerDegreeValue = builder
            .comment("HeatSync heat units per one Power Grid Celsius degree. Default maps 1200 C near HeatSync 400.")
            .defineInRange("heat_per_degree_c", 0.25466893039049235, 0.0001, Double.MAX_VALUE)
        powerGridMaxHeatValue = builder
            .comment("Maximum HeatSync heat exposed by Power Grid thermal devices.")
            .defineInRange("max_heat", 400.0, 1.0, Double.MAX_VALUE)
        builder.pop()

        SPEC = builder.build()
    }

    fun absoluteZeroOffset(): Double = absoluteZeroOffsetValue.get()

    fun csToHeatScale(): Double = csToHeatScaleValue.get()

    fun pipeMinHeat(): Double = pipeMinHeatValue.get()

    fun pipeMaxHeat(): Double = maxOf(pipeMaxHeatValue.get(), pipeMinHeat())

    fun ambientBlendRate(): Double = ambientBlendRateValue.get()

    fun pipeLossPerTick(): Double = pipeLossPerTickValue.get()

    fun networkEqualizationStrength(): Double = networkEqualizationStrengthValue.get()

    fun coldSourcePullRate(): Double = coldSourcePullRateValue.get()

    fun iceSourceHeat(): Double = iceSourceHeatValue.get()

    fun packedIceSourceHeat(): Double = packedIceSourceHeatValue.get()

    fun blueIceSourceHeat(): Double = blueIceSourceHeatValue.get()

    fun taggedColdSourceDefaultHeat(): Double = taggedColdSourceDefaultHeatValue.get()

    fun pipeBlockTempRange(): Double = pipeBlockTempRangeValue.get()

    fun pipeBlockTempMaxEffect(): Double = pipeBlockTempMaxEffectValue.get()

    fun powerGridAmbientHeat(): Double = powerGridAmbientHeatValue.get()

    fun powerGridAmbientTemperature(): Double = powerGridAmbientTemperatureValue.get()

    fun powerGridHeatPerDegree(): Double = powerGridHeatPerDegreeValue.get()

    fun powerGridMaxHeat(): Double = powerGridMaxHeatValue.get()

    fun fireboxHeatPerTick(): Float = fireboxHeatPerTickValue.get().toFloat()

    fun fireboxTargetHeat(): Float = fireboxTargetHeatValue.get().toFloat()

    fun pneumaticAmbientHeat(): Double = pneumaticAmbientHeatValue.get()

    fun pneumaticAmbientTemperature(): Double = pneumaticAmbientTemperatureValue.get()

    fun pneumaticHeatPerKelvin(): Double = pneumaticHeatPerKelvinValue.get()

    fun transducerFeCapacity(): Int = transducerFeCapacityValue.get()

    fun transducerFePerUnit(): Int = transducerFePerUnitValue.get()

    fun transducerUnitsPerTick(): Long = transducerUnitsPerTickValue.get().toLong()

    fun transducerMaxHeat(): Float = transducerMaxHeatValue.get().toFloat()

    fun boilerHeaterMaxHeat(): Float = boilerHeaterMaxHeatValue.get().toFloat()

    fun boilerHeaterSettings(): BoilerHeaterSettings = BoilerHeaterSettings(
        firstThreshold = boilerHeaterFirstThresholdValue.get().toFloat(),
        secondThreshold = boilerHeaterSecondThresholdValue.get().toFloat(),
        thirdThreshold = boilerHeaterThirdThresholdValue.get().toFloat(),
        firstCost = boilerHeaterFirstCostValue.get().toFloat(),
        secondCost = boilerHeaterSecondCostValue.get().toFloat(),
        thirdCost = boilerHeaterThirdCostValue.get().toFloat(),
    )
}
