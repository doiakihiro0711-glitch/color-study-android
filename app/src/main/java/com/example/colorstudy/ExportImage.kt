package com.example.colorstudy

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** 「選んだ画像」と「メモした色の一覧」を横に並べた1枚の画像を作る */
object ExportImage {
    private const val PANEL_W = 780
    private const val SWATCH = 64
    private const val WHEEL = 132
    private const val TOP_HUE = 60f
    private const val BG = 0xFF181818.toInt()

    private class Row(val m: Memo, val num: Int, val note: StaticLayout?, val h: Int)

    fun render(src: Bitmap, memos: List<Memo>, title: String): Bitmap {
        val sc = min(1600f / max(src.width, src.height).toFloat(), 2f)
        val iw = (src.width * sc).roundToInt()
        val ih = (src.height * sc).roundToInt()

        val titleP = TextPaint(Paint.ANTI_ALIAS_FLAG)
        titleP.color = 0xFFCCCCCC.toInt()
        titleP.textSize = 28f
        val hexP = TextPaint(Paint.ANTI_ALIAS_FLAG)
        hexP.color = 0xFFFFFFFF.toInt()
        hexP.textSize = 30f
        hexP.typeface = Typeface.DEFAULT_BOLD
        val subP = TextPaint(Paint.ANTI_ALIAS_FLAG)
        subP.color = 0xFFAAAAAA.toInt()
        subP.textSize = 22f
        val noteP = TextPaint(Paint.ANTI_ALIAS_FLAG)
        noteP.color = 0xFFE0C080.toInt()
        noteP.textSize = 26f
        val textW = PANEL_W - 26 - SWATCH - 18 - WHEEL - 22 - 14

        // 行の高さを決める
        val rows = mutableListOf<Row>()
        var n = 0
        for (m in memos) {
            val num = if (m.fx >= 0f && m.fy >= 0f) { n++; n } else 0
            var layout: StaticLayout? = null
            if (m.note.isNotEmpty()) {
                layout = StaticLayout.Builder.obtain(m.note, 0, m.note.length, noteP, textW)
                    .setMaxLines(3)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
            }
            var h = 16 + 36 + 30 + 16
            if (layout != null) h += layout.height + 6
            rows.add(Row(m, num, layout, max(h, WHEEL + 24)))
        }

        // 縦に積んで、あふれたら次の列へ
        val header = 56
        val targetH = max(ih, 900)
        val colOf = IntArray(rows.size)
        val yOf = IntArray(rows.size)
        var col = 0
        var y = header
        var maxY = header
        for (i in rows.indices) {
            val top = if (col == 0) header else 0
            if (y + rows[i].h > targetH && y > top) {
                col++
                y = 0
            }
            colOf[i] = col
            yOf[i] = y
            y += rows[i].h
            maxY = max(maxY, y)
        }
        val cols = col + 1
        val outW = iw + cols * PANEL_W
        val outH = max(ih, maxY) + 10

        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(BG)

        // 画像
        val dst = RectF(0f, 0f, iw.toFloat(), ih.toFloat())
        val ip = Paint(Paint.FILTER_BITMAP_FLAG)
        c.drawBitmap(src, null, dst, ip)

        // 画像の上の印
        val mr = max(14f, iw / 90f)
        val fillP = Paint(Paint.ANTI_ALIAS_FLAG)
        val ringW = Paint(Paint.ANTI_ALIAS_FLAG)
        ringW.style = Paint.Style.STROKE
        ringW.strokeWidth = 4f
        ringW.color = 0xFFFFFFFF.toInt()
        val ringB = Paint(Paint.ANTI_ALIAS_FLAG)
        ringB.style = Paint.Style.STROKE
        ringB.strokeWidth = 8f
        ringB.color = 0xFF000000.toInt()
        val numFill = TextPaint(Paint.ANTI_ALIAS_FLAG)
        numFill.color = 0xFFFFFFFF.toInt()
        numFill.textSize = max(28f, mr * 1.8f)
        numFill.typeface = Typeface.DEFAULT_BOLD
        val numStroke = TextPaint(numFill)
        numStroke.color = 0xFF000000.toInt()
        numStroke.style = Paint.Style.STROKE
        numStroke.strokeWidth = 7f
        for (r in rows) {
            if (r.num == 0) continue
            val cx = r.m.fx * iw
            val cy = r.m.fy * ih
            fillP.color = 0xFF000000.toInt() or r.m.rgb
            c.drawCircle(cx, cy, mr, ringB)
            c.drawCircle(cx, cy, mr, fillP)
            c.drawCircle(cx, cy, mr, ringW)
            val label = r.num.toString()
            val tx = min(cx + mr + 6f, iw - numFill.measureText(label) - 4f)
            val ty = max(cy - mr, numFill.textSize)
            c.drawText(label, tx, ty, numStroke)
            c.drawText(label, tx, ty, numFill)
        }

        // 右側: タイトルとメモの一覧
        val shown = if (title.isEmpty()) "色メモ" else title
        c.drawText(TextUtils.ellipsize(shown, titleP, (PANEL_W - 40).toFloat(), TextUtils.TruncateAt.END).toString(), iw + 22f, 38f, titleP)

        val cardP = Paint(Paint.ANTI_ALIAS_FLAG)
        cardP.color = 0xFF222222.toInt()
        val swStroke = Paint(Paint.ANTI_ALIAS_FLAG)
        swStroke.style = Paint.Style.STROKE
        swStroke.strokeWidth = 2f
        swStroke.color = 0xFF777777.toInt()
        val badgeP = Paint(Paint.ANTI_ALIAS_FLAG)
        badgeP.color = 0xFF000000.toInt()
        val badgeRing = Paint(Paint.ANTI_ALIAS_FLAG)
        badgeRing.style = Paint.Style.STROKE
        badgeRing.strokeWidth = 2f
        badgeRing.color = 0xFFFFFFFF.toInt()
        val badgeT = TextPaint(Paint.ANTI_ALIAS_FLAG)
        badgeT.color = 0xFFFFFFFF.toInt()
        badgeT.textSize = 20f
        badgeT.typeface = Typeface.DEFAULT_BOLD
        badgeT.textAlign = Paint.Align.CENTER

        val wheelColors = IntArray(7)
        for (k in 0..6) wheelColors[k] = 0xFF000000.toInt() or ColorMath.fromHsv(k * 60f, 1f, 1f)
        val ringShader = SweepGradient(0f, 0f, wheelColors, null)

        for (i in rows.indices) {
            val r = rows[i]
            val x = (iw + colOf[i] * PANEL_W).toFloat()
            val top = yOf[i].toFloat()
            c.drawRoundRect(RectF(x + 10f, top + 5f, x + PANEL_W - 10f, top + r.h - 5f), 16f, 16f, cardP)
            val sx = x + 26f
            val sy = top + (r.h - SWATCH) / 2f
            val sr = RectF(sx, sy, sx + SWATCH, sy + SWATCH)
            fillP.color = 0xFF000000.toInt() or r.m.rgb
            c.drawRoundRect(sr, 12f, 12f, fillP)
            c.drawRoundRect(sr, 12f, 12f, swStroke)
            if (r.num > 0) {
                c.drawCircle(sx + 6f, sy + 6f, 15f, badgeP)
                c.drawCircle(sx + 6f, sy + 6f, 15f, badgeRing)
                c.drawText(r.num.toString(), sx + 6f, sy + 13f, badgeT)
            }
            val tx = sx + SWATCH + 18f
            var ty = top + 16f + 28f
            c.drawText(ColorMath.hex(r.m.rgb) + "  " + ColorMath.describe(r.m.rgb), tx, ty, hexP)
            val hs = ColorMath.hsv(r.m.rgb)
            ty += 32f
            c.drawText(
                "RGB " + ColorMath.r(r.m.rgb) + "," + ColorMath.g(r.m.rgb) + "," + ColorMath.b(r.m.rgb) +
                    "  HSV " + hs[0].roundToInt() + "° " + (hs[1] * 100).roundToInt() + "% " + (hs[2] * 100).roundToInt() + "%",
                tx, ty, subP
            )
            drawWheel(c, x + PANEL_W - 22f - WHEEL / 2f, top + r.h / 2f, WHEEL / 2f, r.m.rgb, ringShader)
            val nl = r.note
            if (nl != null) {
                c.save()
                c.translate(tx, ty + 10f)
                nl.draw(c)
                c.restore()
            }
        }
        return out
    }

    /** 小さなカラーサークル(上が黄色の色相リング+彩度・明度の四角)に、その色の位置を印として描く */
    private fun drawWheel(c: Canvas, cx: Float, cy: Float, outerR: Float, rgb: Int, ringShader: Shader) {
        val hs = ColorMath.hsv(rgb)
        val hue = hs[0]
        val ringW = outerR * 0.22f
        val innerR = outerR - ringW - 2f
        val half = innerR * 1.414f / 2f
        val ringP = Paint(Paint.ANTI_ALIAS_FLAG)
        ringP.style = Paint.Style.STROKE
        ringP.strokeWidth = ringW
        ringP.shader = ringShader
        val pure = 0xFF000000.toInt() or ColorMath.fromHsv(hue, 1f, 1f)
        val a = Paint()
        a.shader = LinearGradient(-half, 0f, half, 0f, 0xFFFFFFFF.toInt(), pure, Shader.TileMode.CLAMP)
        val b = Paint()
        b.shader = LinearGradient(0f, -half, 0f, half, 0x00000000, 0xFF000000.toInt(), Shader.TileMode.CLAMP)
        val black = Paint(Paint.ANTI_ALIAS_FLAG)
        black.style = Paint.Style.STROKE
        black.strokeWidth = 5f
        black.color = 0xFF000000.toInt()
        val white = Paint(Paint.ANTI_ALIAS_FLAG)
        white.style = Paint.Style.STROKE
        white.strokeWidth = 2.5f
        white.color = 0xFFFFFFFF.toInt()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        c.save()
        c.translate(cx, cy)
        c.save()
        c.rotate(-90f - TOP_HUE)
        c.drawCircle(0f, 0f, outerR - ringW / 2f, ringP)
        c.restore()
        val rect = RectF(-half, -half, half, half)
        c.drawRect(rect, a)
        c.drawRect(rect, b)
        // 色相の印(無彩色のときは色相が決まらないので付けない)
        if (hs[1] > 0.02f && hs[2] > 0.02f) {
            val ang = Math.toRadians((hue - TOP_HUE - 90f).toDouble())
            val rr = outerR - ringW / 2f
            val hx = (rr * Math.cos(ang)).toFloat()
            val hy = (rr * Math.sin(ang)).toFloat()
            fill.color = pure
            c.drawCircle(hx, hy, ringW * 0.45f, fill)
            c.drawCircle(hx, hy, ringW * 0.45f, black)
            c.drawCircle(hx, hy, ringW * 0.45f, white)
        }
        // 彩度・明度の印
        val sx = -half + hs[1] * half * 2f
        val sy = -half + (1f - hs[2]) * half * 2f
        fill.color = 0xFF000000.toInt() or rgb
        c.drawCircle(sx, sy, 7f, fill)
        c.drawCircle(sx, sy, 7f, black)
        c.drawCircle(sx, sy, 7f, white)
        c.restore()
    }
}
