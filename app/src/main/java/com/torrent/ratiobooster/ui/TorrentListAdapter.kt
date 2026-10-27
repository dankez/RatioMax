package com.torrent.ratiobooster.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.torrent.ratiobooster.databinding.ItemTorrentBinding
import com.torrent.ratiobooster.model.TorrentItem
import java.util.Locale

class TorrentListAdapter : ListAdapter<TorrentItem, TorrentListAdapter.TorrentViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorrentViewHolder {
        val binding = ItemTorrentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TorrentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TorrentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TorrentViewHolder(private val binding: ItemTorrentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(torrent: TorrentItem) {
            binding.tvTorrentTitle.text = torrent.title
            binding.tvTorrentState.text = torrent.state.name
            binding.pbTorrentProgress.progress = torrent.progressPercent.toInt()
            binding.tvTorrentPeers.text = "S: ${torrent.seeders} | L: ${torrent.leechers}"
            binding.tvTorrentDemand.text = String.format(Locale.US, "Demand: %.1f", torrent.demandScore)
            binding.tvTorrentSize.text = String.format(Locale.US, "Size: %.2f GB", torrent.sizeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
            binding.tvTorrentRatio.text = String.format(Locale.US, "Ratio: %.2f", torrent.ratio)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TorrentItem>() {
        override fun areItemsTheSame(oldItem: TorrentItem, newItem: TorrentItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TorrentItem, newItem: TorrentItem): Boolean = oldItem == newItem
    }
}
