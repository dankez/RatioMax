package com.torrent.ratiobooster.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.torrent.ratiobooster.model.TorrentItem

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ratio_booster_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var storageLimitGb: Double
        get() = prefs.getFloat("storage_limit_gb", 10.0f).toDouble()
        set(value) { prefs.edit().putFloat("storage_limit_gb", value.toFloat()).apply() }

    var wifiOnly: Boolean
        get() = prefs.getBoolean("wifi_only", true)
        set(value) { prefs.edit().putBoolean("wifi_only", value).apply() }

    var rssFeedUrl: String
        get() = prefs.getString("rss_feed_url", "") ?: ""
        set(value) { prefs.edit().putString("rss_feed_url", value).apply() }

    var autoPruneEnabled: Boolean
        get() = prefs.getBoolean("auto_prune_enabled", true)
        set(value) { prefs.edit().putBoolean("auto_prune_enabled", value).apply() }

    fun getSavedTorrents(): List<TorrentItem> {
        val json = prefs.getString("saved_torrents", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TorrentItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTorrents(torrents: List<TorrentItem>) {
        val json = gson.toJson(torrents)
        prefs.edit().putString("saved_torrents", json).apply()
    }
}
