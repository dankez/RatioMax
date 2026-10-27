package com.torrent.ratiobooster.engine

import com.torrent.ratiobooster.model.TorrentItem

class StorageManager(
    var storageLimitGb: Double = 10.0
) {
    val storageLimitBytes: Long
        get() = (storageLimitGb * 1024.0 * 1024.0 * 1024.0).toLong()

    fun calculateUsedStorage(torrents: List<TorrentItem>): Long {
        return torrents.sumOf { it.sizeBytes }
    }

    fun canFit(torrentBytes: Long, currentTorrents: List<TorrentItem>): Boolean {
        return calculateUsedStorage(currentTorrents) + torrentBytes <= storageLimitBytes
    }

    fun evaluatePruning(
        newTorrent: TorrentItem,
        existingTorrents: List<TorrentItem>
    ): PruneResult {
        if (newTorrent.sizeBytes > storageLimitBytes) {
            return PruneResult(
                accepted = false,
                evictedTorrents = emptyList(),
                retainedTorrents = existingTorrents,
                freedBytes = 0L
            )
        }

        val totalUsed = calculateUsedStorage(existingTorrents)
        val neededSpace = (totalUsed + newTorrent.sizeBytes) - storageLimitBytes

        if (neededSpace <= 0) {
            return PruneResult(
                accepted = true,
                evictedTorrents = emptyList(),
                retainedTorrents = existingTorrents,
                freedBytes = 0L
            )
        }

        val evictionCandidates = existingTorrents.sortedWith(
            compareBy<TorrentItem> { it.demandScore }.thenBy { it.addedTimestamp }
        )

        val toEvict = mutableListOf<TorrentItem>()
        var freedBytes = 0L

        for (candidate in evictionCandidates) {
            if (newTorrent.demandScore > candidate.demandScore) {
                toEvict.add(candidate)
                freedBytes += candidate.sizeBytes
                if (freedBytes >= neededSpace) {
                    break
                }
            }
        }

        return if (freedBytes >= neededSpace) {
            val evictedIds = toEvict.map { it.id }.toSet()
            val retained = existingTorrents.filterNot { it.id in evictedIds }
            PruneResult(
                accepted = true,
                evictedTorrents = toEvict,
                retainedTorrents = retained,
                freedBytes = freedBytes
            )
        } else {
            PruneResult(
                accepted = false,
                evictedTorrents = emptyList(),
                retainedTorrents = existingTorrents,
                freedBytes = 0L
            )
        }
    }
}

data class PruneResult(
    val accepted: Boolean,
    val evictedTorrents: List<TorrentItem>,
    val retainedTorrents: List<TorrentItem>,
    val freedBytes: Long
)
