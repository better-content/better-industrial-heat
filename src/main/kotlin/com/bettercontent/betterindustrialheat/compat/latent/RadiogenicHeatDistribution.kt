package com.bettercontent.betterindustrialheat.compat.latent

import com.bettercontent.betterindustrialheat.api.IHeatStorage

object RadiogenicHeatDistribution {
    /** Fairly offers one finite emission to each target and returns heat that could not be accepted. */
    fun distribute(emission: Float, targets: List<IHeatStorage>): Float {
        var remaining = emission.coerceAtLeast(0f)
        targets.forEachIndexed { index, target ->
            if (remaining <= 0f) return@forEachIndexed
            val fairShare = remaining / (targets.size - index)
            remaining -= target.addHeat(fairShare, false).coerceIn(0f, fairShare)
        }
        return remaining
    }
}
