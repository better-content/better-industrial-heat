package com.bettercontent.betterindustrialheat

import com.bettercontent.betterindustrialheat.content.coolant.BundledCoolantDefinitions
import com.bettercontent.betterindustrialheat.content.coolant.CoolantExchangerBlock
import com.bettercontent.betterindustrialheat.content.coolant.CoolantExchangerBlockEntity
import com.bettercontent.betterindustrialheat.content.coolant.LiquidCoolantDefinition
import com.bettercontent.betterindustrialheat.content.heat.ConstantTemperatureBlock
import com.bettercontent.betterindustrialheat.content.heat.ConstantTemperatureBlockEntity
import com.bettercontent.betterindustrialheat.content.heat.BoilerHeaterBlock
import com.bettercontent.betterindustrialheat.content.heat.BoilerHeaterBlockEntity
import com.bettercontent.betterindustrialheat.content.heat.HeatPipeBlock
import com.bettercontent.betterindustrialheat.content.heat.HeatPipeBlockEntity
import com.bettercontent.betterindustrialheat.content.heat.ThermalFireboxBlock
import com.bettercontent.betterindustrialheat.content.heat.ThermalFireboxBlockEntity
import com.bettercontent.betterindustrialheat.food.FoodEffects
import com.bettercontent.betterindustrialheat.food.FoodItems
import com.mojang.datafixers.DSL
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.BucketItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Fluid
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fluids.FluidType
import net.minecraftforge.fluids.ForgeFlowingFluid
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object HeatSyncRegistries {
    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(ForgeRegistries.BLOCKS, HeatSyncMod.MOD_ID)
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, HeatSyncMod.MOD_ID)
    val FLUIDS: DeferredRegister<Fluid> = DeferredRegister.create(ForgeRegistries.FLUIDS, HeatSyncMod.MOD_ID)
    val FLUID_TYPES: DeferredRegister<FluidType> = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, HeatSyncMod.MOD_ID)
    val BLOCK_ENTITY_TYPES: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, HeatSyncMod.MOD_ID)

    private val bundledCoolants = BundledCoolantDefinitions.definitions
    private val hotFluidRegistrations: Map<ResourceLocation, HotFluidRegistration> =
        bundledCoolants.associateBy({ it.hotFluid }, ::registerHotFluid)

    val COOLANT_EXCHANGER: RegistryObject<Block> = BLOCKS.register("coolant_exchanger", ::CoolantExchangerBlock)

    val COOLANT_EXCHANGER_ITEM: RegistryObject<Item> = ITEMS.register("coolant_exchanger") {
        BlockItem(COOLANT_EXCHANGER.get(), Item.Properties())
    }

    val COOLANT_EXCHANGER_BLOCK_ENTITY: RegistryObject<BlockEntityType<CoolantExchangerBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("coolant_exchanger") {
            BlockEntityType.Builder.of(::CoolantExchangerBlockEntity, COOLANT_EXCHANGER.get()).build(DSL.remainderType())
        }

    val HEAT_PIPE: RegistryObject<Block> = BLOCKS.register("heat_pipe", ::HeatPipeBlock)

    val HEAT_PIPE_ITEM: RegistryObject<Item> = ITEMS.register("heat_pipe") {
        BlockItem(HEAT_PIPE.get(), Item.Properties())
    }

    val HEAT_PIPE_BLOCK_ENTITY: RegistryObject<BlockEntityType<HeatPipeBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("heat_pipe") {
            BlockEntityType.Builder.of(::HeatPipeBlockEntity, HEAT_PIPE.get()).build(DSL.remainderType())
        }

    val THERMAL_FIREBOX: RegistryObject<Block> = BLOCKS.register("thermal_firebox", ::ThermalFireboxBlock)

    val THERMAL_FIREBOX_ITEM: RegistryObject<Item> = ITEMS.register("thermal_firebox") {
        BlockItem(THERMAL_FIREBOX.get(), Item.Properties())
    }

    val THERMAL_FIREBOX_BLOCK_ENTITY: RegistryObject<BlockEntityType<ThermalFireboxBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("thermal_firebox") {
            BlockEntityType.Builder.of(::ThermalFireboxBlockEntity, THERMAL_FIREBOX.get()).build(DSL.remainderType())
        }

    val BOILER_HEATER: RegistryObject<Block> = BLOCKS.register("boiler_heater", ::BoilerHeaterBlock)

    val BOILER_HEATER_ITEM: RegistryObject<Item> = ITEMS.register("boiler_heater") {
        BlockItem(BOILER_HEATER.get(), Item.Properties())
    }

    val BOILER_HEATER_BLOCK_ENTITY: RegistryObject<BlockEntityType<BoilerHeaterBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("boiler_heater") {
            BlockEntityType.Builder.of(::BoilerHeaterBlockEntity, BOILER_HEATER.get()).build(DSL.remainderType())
        }

    val CREATIVE_HEAT_SOURCE: RegistryObject<Block> = BLOCKS.register("creative_heat_source") {
        ConstantTemperatureBlock(10_000f, BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK))
    }

    val CREATIVE_HEAT_SOURCE_ITEM: RegistryObject<Item> = ITEMS.register("creative_heat_source") {
        BlockItem(CREATIVE_HEAT_SOURCE.get(), Item.Properties())
    }

    val CREATIVE_COLD_SOURCE: RegistryObject<Block> = BLOCKS.register("creative_cold_source") {
        ConstantTemperatureBlock(0f, BlockBehaviour.Properties.copy(Blocks.PACKED_ICE))
    }

    val CREATIVE_COLD_SOURCE_ITEM: RegistryObject<Item> = ITEMS.register("creative_cold_source") {
        BlockItem(CREATIVE_COLD_SOURCE.get(), Item.Properties())
    }

    val CONSTANT_TEMPERATURE_BLOCK_ENTITY: RegistryObject<BlockEntityType<ConstantTemperatureBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("constant_temperature_source") {
            BlockEntityType.Builder.of(
                ::ConstantTemperatureBlockEntity,
                CREATIVE_HEAT_SOURCE.get(),
                CREATIVE_COLD_SOURCE.get(),
            ).build(DSL.remainderType())
        }

    fun register(modBus: IEventBus) {
        BLOCKS.register(modBus)
        ITEMS.register(modBus)
        FLUIDS.register(modBus)
        FLUID_TYPES.register(modBus)
        BLOCK_ENTITY_TYPES.register(modBus)
        FoodItems.ITEMS.register(modBus)
        FoodEffects.EFFECTS.register(modBus)
    }

    fun hotFluid(hotFluidId: ResourceLocation): ForgeFlowingFluid.Source = hotFluidRegistration(hotFluidId).source.get()

    fun hotBucket(hotFluidId: ResourceLocation): Item = hotFluidRegistration(hotFluidId).bucket.get()

    private fun hotFluidRegistration(hotFluidId: ResourceLocation): HotFluidRegistration {
        return requireNotNull(hotFluidRegistrations[hotFluidId]) { "Unknown hot fluid $hotFluidId" }
    }

    private fun registerHotFluid(definition: LiquidCoolantDefinition): HotFluidRegistration {
        val hotPath = definition.hotFluid.path
        val fluidType = FLUID_TYPES.register(hotPath) {
            object : FluidType(
                FluidType.Properties.create()
                    .temperature(1300)
                    .density(1000)
                    .viscosity(1400)
                    .lightLevel(3),
            ) {
                override fun initializeClient(consumer: java.util.function.Consumer<IClientFluidTypeExtensions>) {
                    consumer.accept(object : IClientFluidTypeExtensions {
                        override fun getStillTexture(): ResourceLocation =
                            ResourceLocation.fromNamespaceAndPath(HeatSyncMod.MOD_ID, "block/${hotPath}_still")

                        override fun getFlowingTexture(): ResourceLocation =
                            ResourceLocation.fromNamespaceAndPath(HeatSyncMod.MOD_ID, "block/${hotPath}_flow")

                        override fun getTintColor(): Int = 0xFFFF_FFFF.toInt()
                    })
                }
            }
        }

        lateinit var source: RegistryObject<ForgeFlowingFluid.Source>
        lateinit var flowing: RegistryObject<ForgeFlowingFluid.Flowing>
        lateinit var block: RegistryObject<LiquidBlock>
        lateinit var bucket: RegistryObject<Item>

        val properties = ForgeFlowingFluid.Properties(
            fluidType,
            { source.get() },
            { flowing.get() },
        )
            .block { block.get() }
            .bucket { bucket.get() }
            .tickRate(10)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2)

        source = FLUIDS.register(hotPath) {
            ForgeFlowingFluid.Source(properties)
        }
        flowing = FLUIDS.register("flowing_$hotPath") {
            ForgeFlowingFluid.Flowing(properties)
        }

        block = BLOCKS.register(hotPath) {
            LiquidBlock({ source.get() }, BlockBehaviour.Properties.copy(Blocks.WATER).noLootTable())
        }

        bucket = ITEMS.register("${hotPath}_bucket") {
            BucketItem({ source.get() }, Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))
        }

        return HotFluidRegistration(
            fluidType = fluidType,
            source = source,
            flowing = flowing,
            block = block,
            bucket = bucket,
        )
    }

    private data class HotFluidRegistration(
        val fluidType: RegistryObject<out FluidType>,
        val source: RegistryObject<ForgeFlowingFluid.Source>,
        val flowing: RegistryObject<ForgeFlowingFluid.Flowing>,
        val block: RegistryObject<LiquidBlock>,
        val bucket: RegistryObject<Item>,
    )
}
