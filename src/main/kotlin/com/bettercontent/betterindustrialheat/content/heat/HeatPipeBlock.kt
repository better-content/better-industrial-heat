package com.bettercontent.betterindustrialheat.content.heat

import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor

class HeatPipeBlock : BaseEntityBlock(
    Properties.of()
        .mapColor(MapColor.METAL)
        .strength(2.0f, 3.0f)
        .sound(SoundType.COPPER)
        .requiresCorrectToolForDrops(),
) {
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = HeatPipeBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? = createTickerHelper(
        blockEntityType,
        HeatSyncRegistries.HEAT_PIPE_BLOCK_ENTITY.get(),
        HeatPipeBlockEntity::tick,
    )
}
