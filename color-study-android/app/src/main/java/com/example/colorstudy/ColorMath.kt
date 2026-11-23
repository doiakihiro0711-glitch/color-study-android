package com.example.colorstudy

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** 色の計算(Androidに依存しない部分) */
object ColorMath {
    fun r(rgb: Int) = (rgb shr 16) and 255
    fun g(rgb: Int) = (rgb shr 8) and 255
    fun b(rgb: Int) = rgb and 255

    fun hex(rgb: Int): String = "#%02X%02X%02X".format(r(rgb), g(rgb), b(rgb))

    /** [h 0..360, s 0..1, v 0..1] */
    fun hsv(rgb: Int): FloatArray {
        val rf = r(rgb) / 255f
        val gf = g(rgb) / 255f
        val bf = b(rgb) / 255f
        val mx = max(rf, max(gf, bf))
        val mn = min(rf, min(gf, bf))
        val d = mx - mn
        var h = 0f
        if (d > 0f) {
            h = when (mx) {
                rf -> ((gf - bf) / d) % 6f
                gf -> (bf - rf) / d + 2f
                else -> (rf - gf) / d + 4f
            } * 60f
            if (h < 0f) h += 360f
        }
        val s = if (mx == 0f) 0f else d / mx
        return floatArrayOf(h, s, mx)
    }

    /** [h 0..360, s 0..1, l 0..1] */
    fun hsl(rgb: Int): FloatArray {
        val rf = r(rgb) / 255f
        val gf = g(rgb) / 255f
        val bf = b(rgb) / 255f
        val mx = max(rf, max(gf, bf))
        val mn = min(rf, min(gf, bf))
        val l = (mx + mn) / 2f
        val d = mx - mn
        val s = if (d == 0f) 0f else d / (1f - abs(2f * l - 1f))
        return floatArrayOf(hsv(rgb)[0], s, l)
    }

    /** HSV(h 0..360, s 0..1, v 0..1) から 0xRRGGBB */
    fun fromHsv(h: Float, s: Float, v: Float): Int {
        val hh = ((h % 360f) + 360f) % 360f
        val c = v * s
        val x = c * (1f - abs((hh / 60f) % 2f - 1f))
        val m = v - c
        val (r1, g1, b1) = when {
            hh < 60f -> Triple(c, x, 0f)
            hh < 120f -> Triple(x, c, 0f)
            hh < 180f -> Triple(0f, c, x)
            hh < 240f -> Triple(0f, x, c)
            hh < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val ri = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val gi = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val bi = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        return (ri shl 16) or (gi shl 8) or bi
    }

    /** ざっくりした色の呼び名(例: 「明るい橙」「くすんだ青」) */
    fun describe(rgb: Int): String {
        val hs = hsv(rgb)
        val h = hs[0]
        val s = hs[1]
        val v = hs[2]
        if (v < 0.12f) return "黒に近い色"
        if (s < 0.08f) {
            return when {
                v > 0.92f -> "白に近い色"
                v > 0.65f -> "明るいグレー"
                v < 0.35f -> "暗いグレー"
                else -> "グレー"
            }
        }
        val name = when {
            h < 15f || h >= 345f -> "赤"
            h < 50f -> if (v < 0.65f) "茶色" else "橙"
            h < 65f -> "黄"
            h < 95f -> "黄緑"
            h < 160f -> "緑"
            h < 185f -> "青緑"
            h < 205f -> "水色"
            h < 250f -> "青"
            h < 280f -> "青紫"
            h < 315f -> "紫"
            else -> "赤紫"
        }
        val tone = when {
            v >= 0.85f && s < 0.35f -> "淡い"
            v >= 0.8f && s >= 0.6f -> "鮮やかな"
            v < 0.4f -> "暗い"
            s < 0.35f -> "くすんだ"
            v >= 0.85f -> "明るい"
            else -> ""
        }
        return tone + name
    }
}
