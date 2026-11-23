package com.example.colorstudy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/** クリスタ風のカラーサークル。外側が色相のリング、内側が彩度×明度の四角 */
class ColorWheelView(ctx: Context) : View(ctx) {
    /** 触って色を変えたときに呼ばれる(setColor では呼ばれない) */
    var onChange: ((Int) -> Unit)? = null

    private var hue = 0f
    private var sat = 0f
    private var vl = 1f
    private var region = 0 // 1:リング 2:四角
    private val d = ctx.resources.displayMetrics.density

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boxA = Paint()
    private val boxB = Paint()
    private val black = Paint(Paint.ANTI_ALIAS_FLAG)
    private val white = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    private var cx = 0f
    private var cy = 0f
    private var outerR = 0f
    private var ringW = 0f
    private var boxL = 0f
    private var boxT = 0f
    private var boxS = 0f

    init {
        ringPaint.style = Paint.Style.STROKE
        black.style = Paint.Style.STROKE
        black.strokeWidth = 4f * d
        black.color = 0xFF000000.toInt()
        white.style = Paint.Style.STROKE
        white.strokeWidth = 2f * d
        white.color = 0xFFFFFFFF.toInt()
        fill.style = Paint.Style.FILL
    }

    /** スポイトした色を反映する。無彩色のときは色相をそのまま残す */
    fun setColor(rgb: Int) {
        val hs = ColorMath.hsv(rgb)
        if (hs[1] > 0.001f && hs[2] > 0.001f) hue = hs[0]
        sat = hs[1]
        vl = hs[2]
        rebuildBox()
        invalidate()
    }

    private fun currentRgb(): Int = ColorMath.fromHsv(hue, sat, vl)

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        cx = w / 2f
        cy = h / 2f
        outerR = min(w, h) / 2f - 2f * d
        ringW = outerR * 0.22f
        val innerR = outerR - ringW - 3f * d
        boxS = innerR * 1.414f
        boxL = cx - boxS / 2f
        boxT = cy - boxS / 2f
        val colors = IntArray(7)
        for (i in 0..6) colors[i] = 0xFF000000.toInt() or ColorMath.fromHsv(i * 60f, 1f, 1f)
        ringPaint.shader = SweepGradient(cx, cy, colors, null)
        ringPaint.strokeWidth = ringW
        rebuildBox()
    }

    private fun rebuildBox() {
        if (boxS <= 0f) return
        val pure = 0xFF000000.toInt() or ColorMath.fromHsv(hue, 1f, 1f)
        boxA.shader = LinearGradient(boxL, 0f, boxL + boxS, 0f, 0xFFFFFFFF.toInt(), pure, Shader.TileMode.CLAMP)
        boxB.shader = LinearGradient(0f, boxT, 0f, boxT + boxS, 0x00000000, 0xFF000000.toInt(), Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (boxS <= 0f) return
        // リング(赤が真上から始まるように回転)
        canvas.save()
        canvas.rotate(-90f, cx, cy)
        canvas.drawCircle(cx, cy, outerR - ringW / 2f, ringPaint)
        canvas.restore()
        // 四角
        val rect = RectF(boxL, boxT, boxL + boxS, boxT + boxS)
        canvas.drawRect(rect, boxA)
        canvas.drawRect(rect, boxB)
        // リングの印
        val ang = Math.toRadians((hue - 90f).toDouble())
        val rr = outerR - ringW / 2f
        val hx = cx + (rr * Math.cos(ang)).toFloat()
        val hy = cy + (rr * Math.sin(ang)).toFloat()
        fill.color = 0xFF000000.toInt() or ColorMath.fromHsv(hue, 1f, 1f)
        canvas.drawCircle(hx, hy, ringW * 0.42f, fill)
        canvas.drawCircle(hx, hy, ringW * 0.42f, black)
        canvas.drawCircle(hx, hy, ringW * 0.42f, white)
        // 四角の印(いまの色で塗る)
        val sx = boxL + sat * boxS
        val sy = boxT + (1f - vl) * boxS
        fill.color = 0xFF000000.toInt() or currentRgb()
        canvas.drawCircle(sx, sy, 8f * d, fill)
        canvas.drawCircle(sx, sy, 8f * d, black)
        canvas.drawCircle(sx, sy, 8f * d, white)
    }

    private fun apply(x: Float, y: Float) {
        if (region == 1) {
            var deg = Math.toDegrees(atan2((y - cy).toDouble(), (x - cx).toDouble())).toFloat() + 90f
            if (deg < 0f) deg += 360f
            hue = deg % 360f
        } else {
            sat = ((x - boxL) / boxS).coerceIn(0f, 1f)
            vl = 1f - ((y - boxT) / boxS).coerceIn(0f, 1f)
        }
        rebuildBox()
        invalidate()
        onChange?.invoke(currentRgb())
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val dist = hypot(e.x - cx, e.y - cy)
                region = if (dist > outerR - ringW - 1f * d) 1 else 2
                parent?.requestDisallowInterceptTouchEvent(true)
                apply(e.x, e.y)
            }
            MotionEvent.ACTION_MOVE -> if (region != 0) apply(e.x, e.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> region = 0
        }
        return true
    }
}
