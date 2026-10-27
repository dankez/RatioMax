package com.torrent.ratiobooster.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.torrent.ratiobooster.data.AppPreferences
import com.torrent.ratiobooster.databinding.FragmentTorrentListBinding
import com.torrent.ratiobooster.engine.TorrentEngineManager
import com.torrent.ratiobooster.network.AndroidNetworkConstraintChecker

class TorrentListFragment : Fragment() {

    private var _binding: FragmentTorrentListBinding? = null
    private val binding get() = _binding!!
    private val adapter = TorrentListAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTorrentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTorrents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTorrents.adapter = adapter
        updateList()
    }

    fun updateList() {
        val context = context ?: return
        val prefs = AppPreferences(context)
        val networkChecker = AndroidNetworkConstraintChecker(context)
        val engineManager = TorrentEngineManager(prefs, networkChecker)

        val torrents = engineManager.getManagedTorrentsList()
        adapter.submitList(torrents)

        if (torrents.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvTorrents.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvTorrents.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
