package com.bettercontent.betterindustrialheat.content.energy

import com.bettercontent.betterindustrialheat.HeatSyncAe2Registries
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor

class ImpossibleMatterTransducerBlock : BaseEntityBlock(
    Properties.of()
        .mapColor(MapColor.COLOR_PURPLE)
        .strength(8.0f, 1_200.0f)
        .sound(SoundType.NETHERITE_BLOCK)
        .requiresCorrectToolForDrops()
        .noLootTable(),
) {
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = ImpossibleMatterTransducerBlockEntity(pos, state)

    override fun playerDestroy(
        level: Level,
        player: Player,
        pos: BlockPos,
        state: BlockState,
        blockEntity: BlockEntity?,
        tool: ItemStack,
    ) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool)
        popResource(level, pos, ItemStack(HeatSyncAe2Registries.IMPOSSIBLE_TRANSDUCER_ITEM.get()))
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? = createTickerHelper(
        blockEntityType,
        HeatSyncAe2Registries.IMPOSSIBLE_TRANSDUCER_BLOCK_ENTITY.get(),
        ImpossibleMatterTransducerBlockEntity::tick,
    )
}
