package com.torrent.ratiobooster.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.torrent.ratiobooster.data.AppPreferences
import com.torrent.ratiobooster.engine.TorrentEngineManager
import com.torrent.ratiobooster.network.AndroidNetworkConstraintChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class RatioBoosterWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val preferences = AppPreferences(applicationContext)
        val networkChecker = AndroidNetworkConstraintChecker(applicationContext)
        val engineManager = TorrentEngineManager(preferences, networkChecker)

        val feedUrl = preferences.rssFeedUrl
        if (feedUrl.isBlank()) {
            return@withContext Result.success()
        }

        try {
            val url = URL(feedUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.inputStream.use { inputStream ->
                engineManager.syncFromRssInputStream(inputStream)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
