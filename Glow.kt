package com.example.aura

import android.graphics.*
import androidx.core.graphics.ColorUtils

/**
 * Draws the whole widget background as ONE bitmap: transparent edge strip with a soft blurred
 * glow, a crisp rotating gradient ring, and the dark card. Everything stays inside the widget
 * bounds, so Android never has to draw outside the widget.
 */
object Glow {
    const val MARGIN_DP = 18f   // the transparent strip that holds the glow
    const val RADIUS_DP = 36f

    fun render(s: AuraState, wPx: Float, hPx: Float, density: Float): Bitmap {
        val k = minOf(1f, 720f / maxOf(wPx, hPx))        // keep bitmap small (RemoteViews memory limit)
        val bw = (wPx * k).toInt().coerceAtLeast(2)
        val bh = (hPx * k).toInt().coerceAtLeast(2)
        val u = density * k
        val bm = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bm)
        val m = MARGIN_DP * u
        val r = RADIUS_DP * u
        val rect = RectF(m, m, bw - m, bh - m)

        val sweep = SweepGradient(bw / 2f, bh / 2f, intArrayOf(s.c1, s.c2, s.c3, s.c1), null)
        sweep.setLocalMatrix(Matrix().apply { setRotate(s.angle, bw / 2f, bh / 2f) })

        // soft outer glow
        cv.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = sweep; style = Paint.Style.STROKE; strokeWidth = 7f * u
            maskFilter = BlurMaskFilter(11f * u, BlurMaskFilter.Blur.NORMAL)
            alpha = if (s.playing) 215 else 90
        })
        // crisp ring
        cv.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = sweep; style = Paint.Style.STROKE; strokeWidth = 2.5f * u
        })
        // card body
        val inner = RectF(rect).apply { inset(2.5f * u, 2.5f * u) }
        cv.drawRoundRect(inner, r - 2.5f * u, r - 2.5f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.blendARGB(0xFF0C0C10.toInt(), s.c1, 0.14f)
        })
        return bm
    }
}
