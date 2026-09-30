package com.bettercontent.betterindustrialheat

import com.bettercontent.betterindustrialheat.content.energy.ImpossibleMatterTransducerBlock
import com.bettercontent.betterindustrialheat.content.energy.ImpossibleMatterTransducerBlockEntity
import com.mojang.datafixers.DSL
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

/** AE2-backed content is isolated so HeatSync remains usable in packs and dev runs without AE2. */
object HeatSyncAe2Registries {
    const val MOD_ID = "ae2"

    private val blocks: DeferredRegister<Block> = DeferredRegister.create(ForgeRegistries.BLOCKS, HeatSyncMod.MOD_ID)
    private val items: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, HeatSyncMod.MOD_ID)
    private val blockEntityTypes: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, HeatSyncMod.MOD_ID)

    val IMPOSSIBLE_TRANSDUCER: RegistryObject<Block> =
        blocks.register("impossible_matter_transducer", ::ImpossibleMatterTransducerBlock)

    val IMPOSSIBLE_TRANSDUCER_ITEM: RegistryObject<Item> = items.register("impossible_matter_transducer") {
        BlockItem(IMPOSSIBLE_TRANSDUCER.get(), Item.Properties())
    }

    val IMPOSSIBLE_TRANSDUCER_BLOCK_ENTITY: RegistryObject<BlockEntityType<ImpossibleMatterTransducerBlockEntity>> =
        blockEntityTypes.register("impossible_matter_transducer") {
            BlockEntityType.Builder.of(::ImpossibleMatterTransducerBlockEntity, IMPOSSIBLE_TRANSDUCER.get())
                .build(DSL.remainderType())
        }

    fun register(modBus: IEventBus) {
        blocks.register(modBus)
        items.register(modBus)
        blockEntityTypes.register(modBus)
    }
}
