package com.bettercontent.betterindustrialheat

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.event.level.ChunkEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import com.bettercontent.betterindustrialheat.api.HeatBlockEntity
import com.bettercontent.betterindustrialheat.content.heat.HeatPipeBlock
import com.bettercontent.betterindustrialheat.content.heat.HeatPipeBlockEntity
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import kotlin.math.abs

object HeatSyncPipeThermalController {
    private const val UPDATE_INTERVAL_TICKS: Long = 20
    private const val MIN_HEAT_DELTA: Double = 0.01
    private val trackedPipePositionsByLevel = mutableMapOf<ResourceKey<Level>, LongOpenHashSet>()

    @SubscribeEvent
    fun onLevelLoad(event: LevelEvent.Load) {
        val level = event.level as? ServerLevel ?: return
        trackedPipePositions(level)
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        val level = event.level as? ServerLevel ?: return
        trackedPipePositionsByLevel.remove(level.dimension())
    }

    @SubscribeEvent
    fun onChunkLoad(event: ChunkEvent.Load) {
        val level = event.level as? ServerLevel ?: return
        val chunk = event.chunk as? LevelChunk ?: return
        indexChunk(level, chunk)
    }

    @SubscribeEvent
    fun onChunkUnload(event: ChunkEvent.Unload) {
        val level = event.level as? ServerLevel ?: return
        val tracked = trackedPipePositionsByLevel[level.dimension()] ?: return
        if (tracked.isEmpty()) {
            return
        }

        val chunkPos = event.chunk.pos
        val iterator = tracked.iterator()
        while (iterator.hasNext()) {
            val packedPos = iterator.nextLong()
            if (SectionPos.blockToSectionCoord(BlockPos.getX(packedPos)) != chunkPos.x) {
                continue
            }
            if (SectionPos.blockToSectionCoord(BlockPos.getZ(packedPos)) != chunkPos.z) {
                continue
            }
            iterator.remove()
        }
    }

    @SubscribeEvent
    fun onBlockPlaced(event: BlockEvent.EntityPlaceEvent) {
        val level = event.level as? ServerLevel ?: return
        if (!isHeatPipeState(event.placedBlock)) {
            return
        }

        enroll(level, event.pos)
    }

    fun enroll(level: ServerLevel, pos: BlockPos) {
        trackedPipePositions(level).add(pos.asLong())
    }

    fun unenroll(level: ServerLevel, pos: BlockPos) {
        trackedPipePositionsByLevel[level.dimension()]?.remove(pos.asLong())
    }

    internal fun isEnrolled(level: ServerLevel, pos: BlockPos): Boolean =
        trackedPipePositionsByLevel[level.dimension()]?.contains(pos.asLong()) == true

    @SubscribeEvent
    fun onLevelTick(event: TickEvent.LevelTickEvent) {
        if (event.phase != TickEvent.Phase.END) {
            return
        }

        val level = event.level as? ServerLevel ?: return
        if ((level.gameTime % UPDATE_INTERVAL_TICKS) != 0L) {
            return
        }

        val tracked = trackedPipePositionsByLevel[level.dimension()] ?: return
        if (tracked.isEmpty()) {
            return
        }

        val pipes = mutableListOf<HeatPipeBlockEntity>()
        val iterator = tracked.iterator()
        while (iterator.hasNext()) {
            val pipePos = BlockPos.of(iterator.nextLong())
            if (!level.isLoaded(pipePos)) {
                iterator.remove()
                continue
            }

            val pipe = level.getBlockEntity(pipePos) as? HeatPipeBlockEntity
            if (pipe == null || pipe.isRemoved) {
                iterator.remove()
                continue
            }

            pipes += pipe
        }
        applyEnvironmentalRound(level, pipes, HeatSyncConfig.ambientBlendRate(),
            HeatSyncConfig.coldSourcePullRate(), HeatSyncConfig.pipeLossPerTick())
    }

    /** Called every 20 ticks in production; rates are explicit for zero-environment native tests. */
    internal fun applyEnvironmentalRound(
        level: ServerLevel, pipes: List<HeatPipeBlockEntity>,
        ambientBlendRate: Double, coldSourcePullRate: Double, pipeLossPerTick: Double,
    ) {
        // Neighbor exchange is settled exclusively by HeatBlockEntity.transferAround every
        // four ticks. This slower round applies only environmental inputs and passive loss.
        val updates = pipes.mapNotNull { pipe ->
            val currentHeat = pipe.getHeat().toDouble()
            val nextHeat = PipeThermalMath.step(
                pipeHeat = currentHeat,
                ambientHeat = AmbientHeatSampling.samplePipeHeat(level, pipe.blockPos),
                neighborAverage = null,
                sourceHeat = resolveSourceHeat(level, pipe.blockPos),
                ambientBlendRate = ambientBlendRate,
                networkEqualizationStrength = 0.0,
                coldSourcePullRate = coldSourcePullRate,
                pipeLossPerTick = pipeLossPerTick,
                minPipeHeat = HeatSyncConfig.pipeMinHeat(),
                maxPipeHeat = HeatSyncConfig.pipeMaxHeat()
            )
            if (abs(nextHeat - currentHeat) < MIN_HEAT_DELTA) null else pipe to nextHeat
        }
        updates.forEach { (pipe, nextHeat) ->
            pipe.setHeat(nextHeat.toFloat())
            HeatBlockEntity.trySync(pipe)
        }
    }

    private fun resolveSourceHeat(level: Level, pos: BlockPos): Double? =
        PipeThermalSourceResolver.resolveAdjacentAverageTargetHeat(level, pos)

    private fun trackedPipePositions(level: ServerLevel): LongOpenHashSet =
        trackedPipePositionsByLevel.getOrPut(level.dimension(), ::LongOpenHashSet)

    private fun indexChunk(level: ServerLevel, chunk: LevelChunk) {
        val tracked = trackedPipePositions(level)
        for ((pos, blockEntity) in chunk.blockEntities) {
            if (blockEntity is HeatPipeBlockEntity && !blockEntity.isRemoved) {
                tracked.add(pos.asLong())
            }
        }
    }

    private fun isHeatPipeState(state: BlockState): Boolean = state.block is HeatPipeBlock
}
