package com.example.aura

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Fetches time-synced lyrics (LRC format) from the community service lrclib.net. */
object Lyrics {
    suspend fun fetch(title: String, artist: String, durationSec: Int): List<Pair<Long, String>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://lrclib.net/api/get?track_name=${Uri.encode(title)}&artist_name=${Uri.encode(artist)}" +
                    if (durationSec > 0) "&duration=$durationSec" else ""
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 6000; c.readTimeout = 6000
                c.setRequestProperty("User-Agent", "AuraWidget/1.0")
                if (c.responseCode != 200) return@withContext emptyList()
                parse(JSONObject(c.inputStream.bufferedReader().readText()).optString("syncedLyrics"))
            } catch (e: Exception) { emptyList() }
        }

    fun parse(lrc: String): List<Pair<Long, String>> {
        val re = Regex("""\[(\d+):(\d+(?:\.\d+)?)](.*)""")
        return lrc.lines().mapNotNull { line ->
            re.matchEntire(line.trim())?.let { m ->
                val ms = ((m.groupValues[1].toLong() * 60 + m.groupValues[2].toDouble()) * 1000).toLong()
                ms to m.groupValues[3].trim()
            }
        }.filter { it.second.isNotEmpty() }
    }
}
