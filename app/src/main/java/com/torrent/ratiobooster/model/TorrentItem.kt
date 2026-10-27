package com.torrent.ratiobooster.model

enum class TorrentState {
    QUEUED,
    DOWNLOADING,
    SEEDING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class TorrentItem(
    val id: String,
    val title: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val seeders: Int,
    val leechers: Int,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val uploadedBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val state: TorrentState = TorrentState.QUEUED,
    val demandScore: Double = 0.0
) {
    val ratio: Double
        get() = if (downloadedBytes > 0) uploadedBytes.toDouble() / downloadedBytes.toDouble() else if (uploadedBytes > 0) 100.0 else 0.0

    val progressPercent: Float
        get() = if (sizeBytes > 0) ((downloadedBytes.toDouble() / sizeBytes.toDouble()) * 100).toFloat().coerceIn(0f, 100f) else 0f
}
