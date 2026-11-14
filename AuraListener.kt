package com.example.aura

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import androidx.glance.appwidget.updateAll
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*

class AuraListener : NotificationListenerService() {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val cn by lazy { ComponentName(this, AuraListener::class.java) }
    private var ctrl: MediaController? = null
    private var lyrics: List<Pair<Long, String>> = emptyList()
    private var lyricsKey = ""

    // How often the border rotates / lyrics are checked. Lower = smoother but uses more battery.
    private val TICK_MS = 1000L
    private val DEGREES_PER_TICK = 12f

    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = pushTrack()
        override fun onPlaybackStateChanged(state: PlaybackState?) = pushTrack()
    }
    private val sessions = MediaSessionManager.OnActiveSessionsChangedListener { attach(it) }

    override fun onListenerConnected() {
        val msm = getSystemService(MediaSessionManager::class.java)
        msm.addOnActiveSessionsChangedListener(sessions, cn)
        attach(msm.getActiveSessions(cn))
        scope.launch {
            while (isActive) {
                delay(TICK_MS)
                val playing = ctrl?.playbackState?.state == PlaybackState.STATE_PLAYING
                if (playing && getSystemService(PowerManager::class.java).isInteractive) tick(true)
            }
        }
    }

    private fun attach(list: List<MediaController>?) {
        ctrl?.unregisterCallback(cb)
        ctrl = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        ctrl?.registerCallback(cb)
        pushTrack()
    }

    private fun pushTrack() {
        val c = ctrl ?: return
        val m = c.metadata ?: return
        val title = m.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        val artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val art = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val pal = art?.let { Palette.from(it).generate() }
        val f1 = 0xFF7A5CFF.toInt(); val f2 = 0xFFFF3CAC.toInt(); val f3 = 0xFF22D3EE.toInt()
        AuraStore.writeTrack(
            this, title, artist, c.playbackState?.state == PlaybackState.STATE_PLAYING,
            pal?.getVibrantColor(pal.getDominantColor(f1)) ?: f1,
            pal?.getLightVibrantColor(pal.getMutedColor(f2)) ?: f2,
            pal?.getDarkVibrantColor(f3) ?: f3, art
        )
        val key = "$title|$artist"
        if (key != lyricsKey) {
            lyricsKey = key; lyrics = emptyList()
            val dur = (m.getLong(MediaMetadata.METADATA_KEY_DURATION) / 1000).toInt()
            scope.launch { lyrics = Lyrics.fetch(title, artist, dur); tick(false) }
        }
        scope.launch { tick(false) }
    }

    private suspend fun tick(advance: Boolean) {
        val st = ctrl?.playbackState
        val pos = if (st == null) 0L else
            st.position + if (st.state == PlaybackState.STATE_PLAYING)
                ((SystemClock.elapsedRealtime() - st.lastPositionUpdateTime) * st.playbackSpeed).toLong() else 0L
        var l0 = ""; var l1 = ""; var l2 = ""
        if (lyrics.isEmpty()) l1 = "No synced lyrics for this song" else {
            val i = lyrics.indexOfLast { it.first <= pos }
            l0 = lyrics.getOrNull(i - 1)?.second ?: ""
            l1 = lyrics.getOrNull(i)?.second ?: "♪"
            l2 = lyrics.getOrNull(i + 1)?.second ?: ""
        }
        val ang = (AuraStore.angle(this) + if (advance) DEGREES_PER_TICK else 0f) % 360f
        AuraStore.writeTick(this, ang, l0, l1, l2)
        AuraWidget().updateAll(this)
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
