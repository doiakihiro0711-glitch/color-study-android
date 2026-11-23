package com.example.colorstudy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min

/** 画像を表示して、触れた場所の色をスポイトするビュー。1本指=スポイト、2本指=拡大・移動 */
class PickerView(ctx: Context) : View(ctx) {
    var onPick: ((Int) -> Unit)? = null
    var sampleN: Int = 1
        set(v) {
            field = v
            if (bmp != null && hasPick) {
                emit()
                invalidate()
            }
        }

    private var bmp: Bitmap? = null
    private val m = Matrix()
    private val inv = Matrix()
    private var fitScale = 1f
    private var pickX = 0
    private var pickY = 0
    private var hasPick = false
    private var loupe = false
    private var loupeX = 0f
    private var loupeY = 0f
    private var mode = 0 // 0:なし 1:スポイト中 2:拡大中
    private var prevX = 0
    private var prevY = 0
    private var prevHas = false
    private var lastCx = 0f
    private var lastCy = 0f
    private var lastDist = 0f
    private val d = ctx.resources.displayMetrics.density

    private val smooth = Paint(Paint.FILTER_BITMAP_FLAG)
    private val crisp = Paint()
    private val black = Paint(Paint.ANTI_ALIAS_FLAG)
    private val white = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setBackgroundColor(0xFF101010.toInt())
        crisp.isFilterBitmap = false
        black.style = Paint.Style.STROKE
        black.strokeWidth = 4f * d
        black.color = 0xFF000000.toInt()
        white.style = Paint.Style.STROKE
        white.strokeWidth = 2f * d
        white.color = 0xFFFFFFFF.toInt()
        hint.color = 0xFF888888.toInt()
        hint.textSize = 15f * d
        hint.textAlign = Paint.Align.CENTER
    }

    fun hasImage(): Boolean = bmp != null

    fun setBitmap(b: Bitmap) {
        bmp = b
        fit()
        pickX = b.width / 2
        pickY = b.height / 2
        hasPick = true
        loupe = false
        emit()
        invalidate()
    }

    private fun fit() {
        val b = bmp ?: return
        if (width == 0 || height == 0) return
        val s = min(width.toFloat() / b.width, height.toFloat() / b.height)
        fitScale = s
        m.reset()
        m.postScale(s, s)
        m.postTranslate((width - b.width * s) / 2f, (height - b.height * s) / 2f)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        fit()
    }

    private fun scaleNow(): Float {
        val v = FloatArray(9)
        m.getValues(v)
        return v[Matrix.MSCALE_X]
    }

    private fun sampleColor(): Int {
        val b = bmp ?: return 0
        val half = sampleN / 2
        var r = 0L
        var g = 0L
        var bl = 0L
        var cnt = 0
        for (dy in -half..half) {
            for (dx in -half..half) {
                val x = (pickX + dx).coerceIn(0, b.width - 1)
                val y = (pickY + dy).coerceIn(0, b.height - 1)
                val px = b.getPixel(x, y)
                r += (px shr 16) and 255
                g += (px shr 8) and 255
                bl += px and 255
                cnt++
            }
        }
        return Color.rgb((r / cnt).toInt(), (g / cnt).toInt(), (bl / cnt).toInt()) and 0xFFFFFF
    }

    private fun emit() {
        onPick?.invoke(sampleColor())
    }

    private fun pickAt(vx: Float, vy: Float) {
        val b = bmp ?: return
        m.invert(inv)
        val p = floatArrayOf(vx, vy)
        inv.mapPoints(p)
        pickX = floor(p[0]).toInt().coerceIn(0, b.width - 1)
        pickY = floor(p[1]).toInt().coerceIn(0, b.height - 1)
        hasPick = true
        val r = 64f * d
        loupeX = vx.coerceIn(r, (width - r).coerceAtLeast(r))
        loupeY = vy - r - 70f * d
        if (loupeY < r) loupeY = vy + r + 70f * d
        loupe = true
        emit()
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (bmp == null) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = pickX
                prevY = pickY
                prevHas = hasPick
                mode = 1
                pickAt(e.x, e.y)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (e.pointerCount == 2) {
                    mode = 2
                    // 拡大を始めたので、さっきのスポイト位置を元に戻す
                    pickX = prevX
                    pickY = prevY
                    hasPick = prevHas
                    loupe = false
                    emit()
                    lastCx = (e.getX(0) + e.getX(1)) / 2f
                    lastCy = (e.getY(0) + e.getY(1)) / 2f
                    lastDist = hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1))
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == 1 && e.pointerCount == 1) {
                    pickAt(e.x, e.y)
                } else if (mode == 2 && e.pointerCount >= 2) {
                    val cx = (e.getX(0) + e.getX(1)) / 2f
                    val cy = (e.getY(0) + e.getY(1)) / 2f
                    val dist = hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1))
                    if (lastDist > 0f && dist > 0f) {
                        val cur = scaleNow()
                        val target = (cur * dist / lastDist).coerceIn(fitScale * 0.5f, fitScale * 60f)
                        val ratio = target / cur
                        m.postScale(ratio, ratio, cx, cy)
                    }
                    m.postTranslate(cx - lastCx, cy - lastCy)
                    lastCx = cx
                    lastCy = cy
                    lastDist = dist
                    invalidate()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (e.pointerCount == 2) mode = 0
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = 0
                loupe = false
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val b = bmp
        if (b == null) {
            canvas.drawText("「画像を開く」で写真を選んでください", width / 2f, height / 2f, hint)
            return
        }
        canvas.drawBitmap(b, m, if (scaleNow() >= 3f) crisp else smooth)
        if (hasPick) {
            val p = floatArrayOf(pickX + 0.5f, pickY + 0.5f)
            m.mapPoints(p)
            canvas.drawCircle(p[0], p[1], 9f * d, black)
            canvas.drawCircle(p[0], p[1], 9f * d, white)
        }
        if (loupe && hasPick) drawLoupe(canvas, b)
    }

    private fun drawLoupe(canvas: Canvas, b: Bitmap) {
        val r = 64f * d
        val path = Path()
        path.addCircle(loupeX, loupeY, r, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)
        canvas.drawColor(0xFF000000.toInt())
        val k = r / 7f
        canvas.translate(loupeX, loupeY)
        canvas.scale(k, k)
        canvas.translate(-(pickX + 0.5f), -(pickY + 0.5f))
        canvas.drawBitmap(b, 0f, 0f, crisp)
        canvas.restore()
        val half = sampleN * k / 2f
        canvas.drawRect(loupeX - half, loupeY - half, loupeX + half, loupeY + half, black)
        canvas.drawRect(loupeX - half, loupeY - half, loupeX + half, loupeY + half, white)
        canvas.drawCircle(loupeX, loupeY, r, black)
        canvas.drawCircle(loupeX, loupeY, r, white)
    }
}
