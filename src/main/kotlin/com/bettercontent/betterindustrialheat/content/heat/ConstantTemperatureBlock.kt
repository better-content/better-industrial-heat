package com.bettercontent.betterindustrialheat.content.heat

import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

class ConstantTemperatureBlock(
    val setpoint: Float,
    properties: BlockBehaviour.Properties,
) : BaseEntityBlock(properties) {
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ConstantTemperatureBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? {
        return createTickerHelper(
            blockEntityType,
            com.bettercontent.betterindustrialheat.HeatSyncRegistries.CONSTANT_TEMPERATURE_BLOCK_ENTITY.get(),
            ConstantTemperatureBlockEntity::tick,
        )
    }

    override fun isSignalSource(state: BlockState): Boolean = false

    override fun getShadeBrightness(state: BlockState, level: BlockGetter, pos: BlockPos): Float = 1.0f
}
