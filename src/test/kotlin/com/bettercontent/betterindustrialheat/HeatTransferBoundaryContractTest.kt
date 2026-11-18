package com.bettercontent.betterindustrialheat

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/** Source-boundary checks supplement native topology tests; these cannot replace a loaded-chunk fixture. */
class HeatTransferBoundaryContractTest {
    private val root = Path.of("src/main/kotlin/com/bettercontent/betterindustrialheat")

    @Test fun `neighbor capability is never queried before loaded guard`() {
        val source = Files.readString(root.resolve("api/HeatBlockEntity.kt"))
        val loop = source.substringAfter("for (direction in Direction.values())")
        assertTrue(source.contains("level::isLoaded, level::getBlockEntity"))
        val loaded = loop.indexOf("if (!isLoaded(neighborPos)) continue")
        val entity = loop.indexOf("getNeighbor(neighborPos)")
        assertTrue(loaded >= 0 && entity > loaded)
    }

    @Test fun `cold source state is never queried before loaded guard`() {
        val source = Files.readString(root.resolve("PipeThermalSourceResolver.kt"))
        assertTrue(source.contains("level::isLoaded, level::getBlockState"))
        val loaded = source.indexOf("if (!isLoaded(neighborPos)) continue")
        val state = source.indexOf("getState(neighborPos)")
        assertTrue(loaded >= 0 && state > loaded)
    }

    @Test fun `native exchange syncs changed endpoints only`() {
        val source = Files.readString(root.resolve("api/HeatBlockEntity.kt"))
        val exchange = source.substringAfter("fun transferAround(source: HeatBlockEntity)").substringBefore("internal fun settleHeat")
        assertTrue(exchange.contains("if (target.getHeat() != targetHeatBefore) sync(targetEntity)"))
        assertTrue(exchange.contains("if (syncSource && source.getHeat() != initialHeat) sync(sourceEntity)"))
        assertTrue(!exchange.contains("if (moved > 0f)"))
    }

    @Test fun `controller has no neighbor average reader`() {
        val source = Files.readString(root.resolve("HeatSyncPipeThermalController.kt"))
        assertTrue(source.contains("neighborAverage = null"))
        assertTrue(!source.contains("resolveNeighborAverage"))
    }
}
