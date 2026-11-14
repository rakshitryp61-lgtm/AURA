package com.example.aura

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider

val CMD = ActionParameters.Key<String>("cmd")

class AuraWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact   // we need the real size to draw the glow bitmap
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = AuraStore.read(context)
        provideContent { AuraCard(s) }
    }
}

class AuraReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = AuraWidget()
}

@Composable
private fun AuraCard(s: AuraState) {
    val d = LocalContext.current.resources.displayMetrics.density
    val size = LocalSize.current
    val bg = Glow.render(s, size.width.value * d, size.height.value * d, d)
    val white = ColorProvider(Color.White)
    val toggle = GlanceModifier.clickable(actionRunCallback<MediaAction>(actionParametersOf(CMD to "ly")))

    Box(GlanceModifier.fillMaxSize().background(ImageProvider(bg))) {
        // padding = transparent glow strip (18dp) + inner padding (14dp)
        Column(GlanceModifier.fillMaxSize().padding(32.dp)) {
            if (!s.lyricsMode) {
                if (s.art != null) Image(ImageProvider(s.art), "Album cover (tap for lyrics)",
                    GlanceModifier.fillMaxWidth().defaultWeight().cornerRadius(24.dp).then(toggle), ContentScale.Crop)
                else Spacer(GlanceModifier.fillMaxWidth().defaultWeight())
                Text(s.title, GlanceModifier.padding(top = 10.dp), maxLines = 1,
                    style = TextStyle(color = white, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                Text(s.artist, maxLines = 1,
                    style = TextStyle(color = ColorProvider(Color(0xFFA9A5B3)), fontSize = 13.sp))
            } else {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (s.art != null) Image(ImageProvider(s.art), "Show cover",
                        GlanceModifier.size(48.dp).cornerRadius(12.dp).then(toggle), ContentScale.Crop)
                    Column(GlanceModifier.padding(start = 12.dp)) {
                        Text(s.title, maxLines = 1,
                            style = TextStyle(color = white, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                        Text(s.artist, maxLines = 1,
                            style = TextStyle(color = ColorProvider(Color(0xFFA9A5B3)), fontSize = 12.sp))
                    }
                }
                Column(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.l0, maxLines = 1, style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.32f)), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    Text(s.l1, GlanceModifier.padding(vertical = 8.dp), maxLines = 3, style = TextStyle(
                        color = white, fontSize = 21.sp, fontWeight = FontWeight.Bold))
                    Text(s.l2, maxLines = 1, style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.32f)), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                }
            }
            Row(GlanceModifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                Btn(android.R.drawable.ic_media_previous, "prev", "Previous")
                Spacer(GlanceModifier.width(18.dp))
                Box(GlanceModifier.size(52.dp).background(ColorProvider(Color(s.c1))).cornerRadius(26.dp)
                    .clickable(actionRunCallback<MediaAction>(actionParametersOf(CMD to "pp"))),
                    contentAlignment = Alignment.Center) {
                    Image(ImageProvider(if (s.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play),
                        "Play or pause", GlanceModifier.size(28.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(Color(0xFF0B0B0E))))
                }
                Spacer(GlanceModifier.width(18.dp))
                Btn(android.R.drawable.ic_media_next, "next", "Next")
            }
        }
    }
}

@Composable
private fun Btn(icon: Int, cmd: String, desc: String) {
    Image(ImageProvider(icon), desc, GlanceModifier.size(40.dp).padding(6.dp)
        .clickable(actionRunCallback<MediaAction>(actionParametersOf(CMD to cmd))))
}

class MediaAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val cmd = parameters[CMD]
        if (cmd == "ly") { AuraStore.toggleLyrics(context); AuraWidget().updateAll(context); return }
        val msm = context.getSystemService(MediaSessionManager::class.java)
        val c = msm.getActiveSessions(ComponentName(context, AuraListener::class.java)).firstOrNull() ?: return
        when (cmd) {
            "prev" -> c.transportControls.skipToPrevious()
            "next" -> c.transportControls.skipToNext()
            "pp" -> if (c.playbackState?.state == PlaybackState.STATE_PLAYING)
                c.transportControls.pause() else c.transportControls.play()
        }
    }
}
