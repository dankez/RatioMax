package com.torrent.ratiobooster.engine

import com.torrent.ratiobooster.model.TorrentItem
import kotlin.math.max

object DemandCalculator {

    fun calculateDemandScore(
        seeders: Int,
        leechers: Int,
        addedTimestampMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Double {
        val safeSeeders = max(1, seeders)
        val safeLeechers = max(0, leechers)

        val leecherRatio = safeLeechers.toDouble() / safeSeeders.toDouble()
        val leecherVolumeWeight = kotlin.math.ln(safeLeechers + 1.0) * 2.0

        val ageHours = max(0.0, (currentTimeMs - addedTimestampMs).toDouble() / (1000.0 * 60.0 * 60.0))
        val freshnessFactor = kotlin.math.exp(-ageHours / 48.0)

        val rawScore = (leecherRatio * 10.0 + leecherVolumeWeight) * freshnessFactor
        return (rawScore * 100.0).roundToTwoDecimals()
    }

    fun updateTorrentDemand(torrent: TorrentItem, currentTimeMs: Long = System.currentTimeMillis()): TorrentItem {
        val score = calculateDemandScore(
            seeders = torrent.seeders,
            leechers = torrent.leechers,
            addedTimestampMs = torrent.addedTimestamp,
            currentTimeMs = currentTimeMs
        )
        return torrent.copy(demandScore = score)
    }

    private fun Double.roundToTwoDecimals(): Double {
        return (this * 100.0).let { Math.round(it) / 100.0 }
    }
}
