package com.torrent.ratiobooster

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.torrent.ratiobooster.databinding.ActivityMainBinding
import com.torrent.ratiobooster.ui.DashboardFragment
import com.torrent.ratiobooster.ui.SettingsFragment
import com.torrent.ratiobooster.ui.TorrentListFragment
import com.torrent.ratiobooster.worker.RatioBoosterWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val dashboardFragment = DashboardFragment()
    private val torrentListFragment = TorrentListFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scheduleBackgroundWorker()

        if (savedInstanceState == null) {
            replaceFragment(dashboardFragment)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    replaceFragment(dashboardFragment)
                    dashboardFragment.updateUi()
                    true
                }
                R.id.nav_torrents -> {
                    replaceFragment(torrentListFragment)
                    torrentListFragment.updateList()
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun scheduleBackgroundWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<RatioBoosterWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "RatioBoosterWorkerTask",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
