package com.torrent.ratiobooster

import com.torrent.ratiobooster.engine.DemandCalculator
import com.torrent.ratiobooster.engine.StorageManager
import com.torrent.ratiobooster.model.TorrentItem
import com.torrent.ratiobooster.model.TorrentState
import com.torrent.ratiobooster.network.NetworkConstraintChecker
import com.torrent.ratiobooster.rss.RssFeedParser
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class CoreLogicTest {

    @Test
    fun testDemandCalculator_higherLeecherToSeederRatioGivesHigherScore() {
        val now = System.currentTimeMillis()
        val highDemandScore = DemandCalculator.calculateDemandScore(seeders = 5, leechers = 200, addedTimestampMs = now, currentTimeMs = now)
        val lowDemandScore = DemandCalculator.calculateDemandScore(seeders = 100, leechers = 5, addedTimestampMs = now, currentTimeMs = now)

        assertTrue("High demand score should be strictly greater than low demand score", highDemandScore > lowDemandScore)
    }

    @Test
    fun testDemandCalculator_ageDecayReducesScore() {
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (48 * 60 * 60 * 1000L)

        val freshScore = DemandCalculator.calculateDemandScore(seeders = 10, leechers = 50, addedTimestampMs = now, currentTimeMs = now)
        val oldScore = DemandCalculator.calculateDemandScore(seeders = 10, leechers = 50, addedTimestampMs = twoDaysAgo, currentTimeMs = now)

        assertTrue("Fresh torrent should have higher demand score than old torrent", freshScore > oldScore)
    }

    @Test
    fun testStorageManager_pruningEvictsLowestDemandTorrents() {
        val manager = StorageManager(storageLimitGb = 1.0)
        val oneGbInBytes = (1024L * 1024L * 1024L)
        val halfGbInBytes = oneGbInBytes / 2

        val lowDemandTorrent = TorrentItem(
            id = "1",
            title = "Old Movie",
            downloadUrl = "http://example.com/1.torrent",
            sizeBytes = halfGbInBytes,
            seeders = 100,
            leechers = 2,
            demandScore = 10.0
        )

        val existingTorrents = listOf(lowDemandTorrent)

        val newHighDemandTorrent = TorrentItem(
            id = "2",
            title = "Popular New Release",
            downloadUrl = "http://example.com/2.torrent",
            sizeBytes = halfGbInBytes + (100 * 1024 * 1024),
            seeders = 2,
            leechers = 300,
            demandScore = 95.0
        )

        val result = manager.evaluatePruning(newHighDemandTorrent, existingTorrents)

        assertTrue("New high demand torrent should be accepted", result.accepted)
        assertEquals(1, result.evictedTorrents.size)
        assertEquals("1", result.evictedTorrents.first().id)
        assertTrue(result.retainedTorrents.isEmpty())
    }

    @Test
    fun testRssFeedParser_parsesItemsCorrectly() {
        val rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>Test Tracker</title>
                    <item>
                        <title>Popular Show S01E01 [S: 20, L: 500]</title>
                        <link>https://sktorrent.eu/torrent/details.php?id=79f8d65677b3e96a56b9b7a633411f5fda3d6219</link>
                        <size>1073741824</size>
                        <seeders>20</seeders>
                        <leechers>500</leechers>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val parser = RssFeedParser()
        val inputStream = ByteArrayInputStream(rssXml.toByteArray(Charsets.UTF_8))
        val items = parser.parse(inputStream)

        assertEquals(1, items.size)
        val item = items.first()
        assertEquals("Popular Show S01E01 [S: 20, L: 500]", item.title)
        assertEquals("https://sktorrent.eu/torrent/download.php?id=79f8d65677b3e96a56b9b7a633411f5fda3d6219", item.downloadUrl)
        assertEquals(1073741824L, item.sizeBytes)
        assertEquals(20, item.seeders)
        assertEquals(500, item.leechers)
        assertEquals(TorrentState.QUEUED, item.state)
    }

    @Test
    fun testRssFeedParser_transformSkTorrentUrl() {
        val parser = RssFeedParser()
        val detailsUrl = "https://sktorrent.eu/torrent/details.php?id=79f8d65677b3e96a56b9b7a633411f5fda3d6219"
        val downloadUrl = parser.transformToDownloadUrl(detailsUrl)
        assertEquals("https://sktorrent.eu/torrent/download.php?id=79f8d65677b3e96a56b9b7a633411f5fda3d6219", downloadUrl)
    }

    @Test
    fun testNetworkConstraintChecker_mockValidation() {
        val mockChecker = object : NetworkConstraintChecker {
            override fun isWifiConnected(): Boolean = false
            override fun canTransferData(wifiOnlyRequired: Boolean): Boolean = !wifiOnlyRequired
        }

        assertFalse("Should not allow transfer when wifiOnly is true and wifi is disconnected", mockChecker.canTransferData(wifiOnlyRequired = true))
        assertTrue("Should allow transfer when wifiOnly is false and internet is available", mockChecker.canTransferData(wifiOnlyRequired = false))
    }
}
