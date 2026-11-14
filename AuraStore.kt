package com.example.aura

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

data class AuraState(
    val title: String, val artist: String, val playing: Boolean,
    val c1: Int, val c2: Int, val c3: Int, val art: Bitmap?,
    val angle: Float, val lyricsMode: Boolean, val l0: String, val l1: String, val l2: String
)

object AuraStore {
    private fun p(c: Context) = c.getSharedPreferences("aura", Context.MODE_PRIVATE)

    fun writeTrack(c: Context, title: String, artist: String, playing: Boolean,
                   c1: Int, c2: Int, c3: Int, art: Bitmap?) {
        art?.let {
            val small = Bitmap.createScaledBitmap(it, 512, 512, true)
            File(c.filesDir, "art.png").outputStream().use { o -> small.compress(Bitmap.CompressFormat.PNG, 100, o) }
        }
        p(c).edit().putString("t", title).putString("a", artist).putBoolean("p", playing)
            .putInt("c1", c1).putInt("c2", c2).putInt("c3", c3).apply()
    }

    fun angle(c: Context) = p(c).getFloat("ang", 0f)

    fun writeTick(c: Context, angle: Float, l0: String, l1: String, l2: String) {
        p(c).edit().putFloat("ang", angle).putString("l0", l0).putString("l1", l1).putString("l2", l2).apply()
    }

    fun toggleLyrics(c: Context) {
        p(c).edit().putBoolean("ly", !p(c).getBoolean("ly", false)).apply()
    }

    fun read(c: Context): AuraState {
        val s = p(c)
        val f = File(c.filesDir, "art.png")
        return AuraState(
            s.getString("t", "Nothing playing")!!, s.getString("a", "Start a song")!!, s.getBoolean("p", false),
            s.getInt("c1", 0xFF7A5CFF.toInt()), s.getInt("c2", 0xFFFF3CAC.toInt()), s.getInt("c3", 0xFF22D3EE.toInt()),
            if (f.exists()) BitmapFactory.decodeFile(f.path) else null,
            s.getFloat("ang", 0f), s.getBoolean("ly", false),
            s.getString("l0", "")!!, s.getString("l1", "")!!, s.getString("l2", "")!!
        )
    }
}
