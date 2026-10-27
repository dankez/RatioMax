package com.torrent.ratiobooster.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.torrent.ratiobooster.data.AppPreferences
import com.torrent.ratiobooster.databinding.FragmentDashboardBinding
import com.torrent.ratiobooster.engine.TorrentEngineManager
import com.torrent.ratiobooster.network.AndroidNetworkConstraintChecker
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUi()
    }

    fun updateUi() {
        val context = context ?: return
        val prefs = AppPreferences(context)
        val networkChecker = AndroidNetworkConstraintChecker(context)
        val engineManager = TorrentEngineManager(prefs, networkChecker)

        val torrents = engineManager.getManagedTorrentsList()

        val totalUploaded = torrents.sumOf { it.uploadedBytes }
        val totalDownloaded = torrents.sumOf { it.downloadedBytes }
        val totalSize = torrents.sumOf { it.sizeBytes }

        val globalRatio = if (totalDownloaded > 0) {
            totalUploaded.toDouble() / totalDownloaded.toDouble()
        } else if (totalUploaded > 0) {
            100.0
        } else {
            0.0
        }

        binding.tvGlobalRatio.text = String.format(Locale.US, "%.2f", globalRatio)
        binding.tvUploaded.text = "Uploaded: ${formatMb(totalUploaded)}"
        binding.tvDownloaded.text = "Downloaded: ${formatMb(totalDownloaded)}"

        val usedGb = totalSize.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val limitGb = prefs.storageLimitGb
        val progress = if (limitGb > 0) ((usedGb / limitGb) * 100).toInt().coerceIn(0, 100) else 0

        binding.pbStorage.progress = progress
        binding.tvStorageStatus.text = String.format(Locale.US, "%.2f GB / %.1f GB Used", usedGb, limitGb)

        val wifiConnected = networkChecker.isWifiConnected()
        binding.tvWifiStatus.text = if (wifiConnected) {
            "Wi-Fi Status: Connected (Active)"
        } else {
            "Wi-Fi Status: Not Connected (Transfers Paused)"
        }
    }

    private fun formatMb(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
