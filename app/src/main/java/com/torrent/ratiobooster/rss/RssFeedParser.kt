package com.torrent.ratiobooster.rss

import com.torrent.ratiobooster.model.TorrentItem
import com.torrent.ratiobooster.model.TorrentState
import java.io.InputStream
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory

class RssFeedParser {

    fun parse(inputStream: InputStream): List<TorrentItem> {
        val torrents = mutableListOf<TorrentItem>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        dbFactory.isNamespaceAware = true
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(inputStream)
        doc.documentElement.normalize()

        val itemList = doc.getElementsByTagName("item")
        val entryList = doc.getElementsByTagName("entry")

        val nodes = if (itemList.length > 0) itemList else entryList

        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                val element = node as org.w3c.dom.Element

                val title = element.getElementsByTagName("title").item(0)?.textContent

                var link: String? = null
                val linkNodes = element.getElementsByTagName("link")
                if (linkNodes.length > 0) {
                    val linkEl = linkNodes.item(0) as org.w3c.dom.Element
                    link = linkEl.getAttribute("href").ifBlank { linkEl.textContent }
                }

                var enclosureUrl: String? = null
                var sizeBytes: Long? = null
                val enclosureNodes = element.getElementsByTagName("enclosure")
                if (enclosureNodes.length > 0) {
                    val encEl = enclosureNodes.item(0) as org.w3c.dom.Element
                    enclosureUrl = encEl.getAttribute("url")
                    sizeBytes = encEl.getAttribute("length").toLongOrNull()
                }

                val seedersStr = element.getElementsByTagName("seeders").item(0)?.textContent
                val leechersStr = element.getElementsByTagName("leechers").item(0)?.textContent
                val sizeStr = element.getElementsByTagName("size").item(0)?.textContent

                val parsedSeeders = seedersStr?.toIntOrNull() ?: 1
                val parsedLeechers = leechersStr?.toIntOrNull() ?: 0
                val parsedSize = sizeBytes ?: sizeStr?.toLongOrNull() ?: (1024L * 1024L * 500L)

                val downloadUrl = enclosureUrl ?: link

                if (!title.isNullOrBlank() && !downloadUrl.isNullOrBlank()) {
                    val extractedSeeders = parseNumberPattern(title, listOf("seeders", "seeds", "s:")) ?: parsedSeeders
                    val extractedLeechers = parseNumberPattern(title, listOf("leechers", "leeches", "l:")) ?: parsedLeechers

                    torrents.add(
                        TorrentItem(
                            id = UUID.nameUUIDFromBytes(downloadUrl.toByteArray()).toString(),
                            title = title,
                            downloadUrl = downloadUrl,
                            sizeBytes = parsedSize,
                            seeders = extractedSeeders,
                            leechers = extractedLeechers,
                            addedTimestamp = System.currentTimeMillis(),
                            state = TorrentState.QUEUED
                        )
                    )
                }
            }
        }

        return torrents
    }

    private fun parseNumberPattern(text: String, keywords: List<String>): Int? {
        val lowerText = text.lowercase()
        for (keyword in keywords) {
            val idx = lowerText.indexOf(keyword)
            if (idx != -1) {
                val sub = lowerText.substring(idx + keyword.length).trim()
                val match = Regex("""^:?\s*(\d+)""").find(sub)
                if (match != null) {
                    return match.groupValues[1].toIntOrNull()
                }
            }
        }
        return null
    }
}
