package com.torrent.ratiobooster.engine

import com.torrent.ratiobooster.data.AppPreferences
import com.torrent.ratiobooster.model.TorrentItem
import com.torrent.ratiobooster.model.TorrentState
import com.torrent.ratiobooster.network.NetworkConstraintChecker
import com.torrent.ratiobooster.rss.RssFeedParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream

class TorrentEngineManager(
    private val preferences: AppPreferences,
    private val networkConstraintChecker: NetworkConstraintChecker,
    private val rssFeedParser: RssFeedParser = RssFeedParser()
) {
    private val storageManager = StorageManager(preferences.storageLimitGb)

    private val _managedTorrents = MutableStateFlow<List<TorrentItem>>(preferences.getSavedTorrents())
    val managedTorrents: StateFlow<List<TorrentItem>> = _managedTorrents.asStateFlow()

    fun updateStorageLimit(limitGb: Double) {
        preferences.storageLimitGb = limitGb
        storageManager.storageLimitGb = limitGb
    }

    fun getManagedTorrentsList(): List<TorrentItem> = _managedTorrents.value

    fun addOrUpdateTorrent(torrent: TorrentItem) {
        val currentList = _managedTorrents.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == torrent.id }
        if (index >= 0) {
            currentList[index] = torrent
        } else {
            currentList.add(torrent)
        }
        _managedTorrents.value = currentList
        preferences.saveTorrents(currentList)
    }

    fun removeTorrent(id: String) {
        val updated = _managedTorrents.value.filterNot { it.id == id }
        _managedTorrents.value = updated
        preferences.saveTorrents(updated)
    }

    fun processIncomingCandidates(feedCandidates: List<TorrentItem>): SyncSummary {
        val wifiOnly = preferences.wifiOnly
        val isWifiOk = networkConstraintChecker.canTransferData(wifiOnly)

        if (!isWifiOk) {
            val pausedList = _managedTorrents.value.map { torrent ->
                if (torrent.state == TorrentState.DOWNLOADING || torrent.state == TorrentState.SEEDING) {
                    torrent.copy(state = TorrentState.PAUSED)
                } else torrent
            }
            _managedTorrents.value = pausedList
            preferences.saveTorrents(pausedList)
            return SyncSummary(
                wifiOk = false,
                addedCount = 0,
                prunedCount = 0,
                activeCount = pausedList.count { it.state == TorrentState.SEEDING || it.state == TorrentState.DOWNLOADING }
            )
        }

        val scoredCandidates = feedCandidates.map { DemandCalculator.updateTorrentDemand(it) }
            .sortedByDescending { it.demandScore }

        var added = 0
        var pruned = 0

        val currentList = _managedTorrents.value.toMutableList()

        for (candidate in scoredCandidates) {
            if (currentList.any { it.id == candidate.id }) continue

            val pruneResult = storageManager.evaluatePruning(candidate, currentList)
            if (pruneResult.accepted) {
                if (pruneResult.evictedTorrents.isNotEmpty()) {
                    val evictedIds = pruneResult.evictedTorrents.map { it.id }.toSet()
                    currentList.removeAll { it.id in evictedIds }
                    pruned += pruneResult.evictedTorrents.size
                }

                val activeCandidate = candidate.copy(
                    state = TorrentState.DOWNLOADING
                )
                currentList.add(activeCandidate)
                added++
            }
        }

        val updatedList = currentList.map { item ->
            var updated = item
            if (updated.state == TorrentState.DOWNLOADING) {
                val newDownloaded = (updated.downloadedBytes + (updated.sizeBytes / 10)).coerceAtMost(updated.sizeBytes)
                val isCompleted = newDownloaded >= updated.sizeBytes
                updated = updated.copy(
                    downloadedBytes = newDownloaded,
                    state = if (isCompleted) TorrentState.SEEDING else TorrentState.DOWNLOADING
                )
            }
            if (updated.state == TorrentState.SEEDING) {
                val seedIncrement = (updated.sizeBytes * 0.05 * (updated.leechers.toDouble() / maxOf(1, updated.seeders))).toLong()
                updated = updated.copy(
                    uploadedBytes = updated.uploadedBytes + seedIncrement
                )
            }
            DemandCalculator.updateTorrentDemand(updated)
        }

        _managedTorrents.value = updatedList
        preferences.saveTorrents(updatedList)

        return SyncSummary(
            wifiOk = true,
            addedCount = added,
            prunedCount = pruned,
            activeCount = updatedList.count { it.state == TorrentState.SEEDING || it.state == TorrentState.DOWNLOADING }
        )
    }

    fun syncFromRssInputStream(inputStream: InputStream): SyncSummary {
        val candidates = rssFeedParser.parse(inputStream)
        return processIncomingCandidates(candidates)
    }
}

data class SyncSummary(
    val wifiOk: Boolean,
    val addedCount: Int,
    val prunedCount: Int,
    val activeCount: Int
)
