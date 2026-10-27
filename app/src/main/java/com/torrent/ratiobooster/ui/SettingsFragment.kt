package com.torrent.ratiobooster.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.torrent.ratiobooster.data.AppPreferences
import com.torrent.ratiobooster.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = AppPreferences(requireContext())

        binding.etStorageLimit.setText(prefs.storageLimitGb.toString())
        binding.etRssFeed.setText(prefs.rssFeedUrl)
        binding.etTrackerUid.setText(prefs.trackerUid)
        binding.etTrackerPass.setText(prefs.trackerPass)
        binding.switchWifiOnly.isChecked = prefs.wifiOnly
        binding.switchAutoPrune.isChecked = prefs.autoPruneEnabled

        binding.btnSave.setOnClickListener {
            val limitStr = binding.etStorageLimit.text.toString()
            val limitGb = limitStr.toDoubleOrNull() ?: 10.0

            prefs.storageLimitGb = limitGb
            prefs.rssFeedUrl = binding.etRssFeed.text.toString().trim()
            prefs.trackerUid = binding.etTrackerUid.text.toString().trim()
            prefs.trackerPass = binding.etTrackerPass.text.toString().trim()
            prefs.wifiOnly = binding.switchWifiOnly.isChecked
            prefs.autoPruneEnabled = binding.switchAutoPrune.isChecked

            Toast.makeText(requireContext(), "Settings Saved", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
