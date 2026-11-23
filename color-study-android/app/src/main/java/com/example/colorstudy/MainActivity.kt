package com.example.colorstudy

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private lateinit var picker: PickerView
    private lateinit var wheel: ColorWheelView
    private lateinit var swatchView: View
    private lateinit var hexT: TextView
    private lateinit var rgbT: TextView
    private lateinit var hsvT: TextView
    private lateinit var hslT: TextView
    private lateinit var nameT: TextView
    private lateinit var noteE: EditText
    private lateinit var memoBtn: android.widget.Button
    private lateinit var sampleBtn: android.widget.Button
    private lateinit var store: Store

    private var lastRgb = 0x808080
    private var imageName = ""
    private val samples = intArrayOf(1, 3, 5, 9)
    private var sampleIdx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        buildUi()
        showColor(lastRgb, true)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        memoBtn.text = "メモ一覧 (" + store.all().size + ")"
    }

    private fun buildUi() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(0xFF121212.toInt())

        // 上のボタン
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.setPadding(dp(8), dp(8), dp(8), dp(8))
        val openBtn = flatButton(this, "画像を開く", 0xFF3D5A80.toInt())
        openBtn.setOnClickListener { openPicker() }
        sampleBtn = flatButton(this, "範囲: 1px")
        sampleBtn.setOnClickListener {
            sampleIdx = (sampleIdx + 1) % samples.size
            val n = samples[sampleIdx]
            picker.sampleN = n
            sampleBtn.text = if (n == 1) "範囲: 1px" else "平均: " + n + "×" + n
        }
        memoBtn = flatButton(this, "メモ一覧 (0)")
        memoBtn.setOnClickListener { startActivity(Intent(this, MemoActivity::class.java)) }
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp.setMargins(dp(3), 0, dp(3), 0)
        top.addView(openBtn, lp)
        top.addView(sampleBtn, LinearLayout.LayoutParams(lp))
        top.addView(memoBtn, LinearLayout.LayoutParams(lp))
        root.addView(top)

        // 画像
        picker = PickerView(this)
        picker.onPick = { rgb -> showColor(rgb, true) }
        root.addView(picker, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // 下のパネル
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.setBackgroundColor(0xFF1C1C1C.toInt())
        panel.setPadding(dp(10), dp(8), dp(10), dp(8))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        wheel = ColorWheelView(this)
        wheel.onChange = { rgb -> showColor(rgb, false) }
        row.addView(wheel, LinearLayout.LayoutParams(dp(150), dp(150)))

        val info = LinearLayout(this)
        info.orientation = LinearLayout.VERTICAL
        info.setPadding(dp(12), 0, 0, 0)
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        swatchView = View(this)
        head.addView(swatchView, LinearLayout.LayoutParams(dp(44), dp(44)))
        hexT = text(22f, true)
        hexT.setPadding(dp(10), 0, 0, 0)
        head.addView(hexT)
        info.addView(head)
        nameT = text(14f, false)
        nameT.setTextColor(0xFFE0C080.toInt())
        nameT.setPadding(0, dp(4), 0, dp(2))
        info.addView(nameT)
        rgbT = text(13f, false)
        hsvT = text(13f, false)
        hslT = text(13f, false)
        info.addView(rgbT)
        info.addView(hsvT)
        info.addView(hslT)
        row.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        panel.addView(row)

        // よく使う言葉
        val chips = LinearLayout(this)
        chips.orientation = LinearLayout.HORIZONTAL
        val tags = listOf("肌", "髪", "瞳", "影", "ハイライト", "反射光", "背景", "服", "空", "草木")
        for (t in tags) {
            val c = flatButton(this, t, 0xFF333333.toInt())
            c.textSize = 12f
            c.setOnClickListener {
                val cur = noteE.text.toString()
                noteE.setText(if (cur.isEmpty()) t else cur + " " + t)
                noteE.setSelection(noteE.text.length)
            }
            val clp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            clp.setMargins(0, dp(6), dp(6), dp(2))
            chips.addView(c, clp)
        }
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        hs.addView(chips)
        panel.addView(hs)

        val noteRow = LinearLayout(this)
        noteRow.orientation = LinearLayout.HORIZONTAL
        noteRow.gravity = Gravity.CENTER_VERTICAL
        noteE = EditText(this)
        noteE.hint = "メモ(例: 肌の影色)"
        noteE.setTextColor(0xFFEEEEEE.toInt())
        noteE.setHintTextColor(0xFF777777.toInt())
        noteE.textSize = 14f
        noteE.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        noteE.maxLines = 3
        noteRow.addView(noteE, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val saveBtn = flatButton(this, "保存", 0xFF2F7D5B.toInt())
        saveBtn.setOnClickListener { saveMemo() }
        val slp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        slp.setMargins(dp(8), 0, 0, 0)
        noteRow.addView(saveBtn, slp)
        panel.addView(noteRow)

        root.addView(panel)
        setContentView(root)
    }

    private fun text(size: Float, bold: Boolean): TextView {
        val t = TextView(this)
        t.textSize = size
        t.setTextColor(0xFFEEEEEE.toInt())
        if (bold) t.setTypeface(t.typeface, android.graphics.Typeface.BOLD)
        return t
    }

    /** 色の表示を更新する。fromImage が true のときは、カラーサークルも動かす */
    private fun showColor(rgb: Int, moveWheel: Boolean) {
        lastRgb = rgb
        if (moveWheel) wheel.setColor(rgb)
        swatchView.background = swatch(this, rgb)
        hexT.text = ColorMath.hex(rgb)
        nameT.text = ColorMath.describe(rgb)
        rgbT.text = "RGB  " + ColorMath.r(rgb) + ", " + ColorMath.g(rgb) + ", " + ColorMath.b(rgb)
        val v = ColorMath.hsv(rgb)
        hsvT.text = "HSV  " + v[0].roundToInt() + "°, " + (v[1] * 100).roundToInt() + "%, " + (v[2] * 100).roundToInt() + "%"
        val l = ColorMath.hsl(rgb)
        hslT.text = "HSL  " + l[0].roundToInt() + "°, " + (l[1] * 100).roundToInt() + "%, " + (l[2] * 100).roundToInt() + "%"
    }

    private fun saveMemo() {
        if (!picker.hasImage()) {
            Toast.makeText(this, "先に画像を開いてください", Toast.LENGTH_SHORT).show()
            return
        }
        store.add(lastRgb, noteE.text.toString().trim(), imageName)
        noteE.setText("")
        memoBtn.text = "メモ一覧 (" + store.all().size + ")"
        Toast.makeText(this, ColorMath.hex(lastRgb) + " を保存しました", Toast.LENGTH_SHORT).show()
    }

    // ---------- 画像を開く ----------
    private fun openPicker() {
        try {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)
            i.type = "image/*"
            startActivityForResult(i, REQ_IMAGE)
        } catch (e: Exception) {
            try {
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.type = "image/*"
                startActivityForResult(Intent.createChooser(i, "画像を選ぶ"), REQ_IMAGE)
            } catch (e2: Exception) {
                Toast.makeText(this, "画像を選ぶ画面を開けませんでした", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_IMAGE && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) loadImage(uri)
        }
    }

    private fun handleIntent(i: Intent?) {
        if (i == null) return
        if (Intent.ACTION_SEND == i.action) {
            @Suppress("DEPRECATION")
            val uri = i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) loadImage(uri)
        }
    }

    private fun loadImage(uri: Uri) {
        Toast.makeText(this, "読み込み中…", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val b = decode(uri)
                val name = nameOf(uri)
                runOnUiThread {
                    imageName = name
                    picker.setBitmap(b)
                }
            } catch (e: Throwable) {
                runOnUiThread { Toast.makeText(this, "画像を開けませんでした", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun nameOf(uri: Uri): String {
        try {
            val c = contentResolver.query(uri, null, null, null, null)
            if (c != null) {
                c.use {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && it.moveToFirst()) return it.getString(idx) ?: ""
                }
            }
        } catch (e: Exception) {
        }
        return uri.lastPathSegment ?: ""
    }

    private fun decode(uri: Uri): Bitmap {
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, o) }
        var sample = 1
        val side = max(o.outWidth, o.outHeight)
        while (side / sample > MAX_SIDE) sample *= 2
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = sample
        o2.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bmp = contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, o2) }
            ?: throw IllegalStateException("decode")
        var orient = ExifInterface.ORIENTATION_NORMAL
        try {
            contentResolver.openInputStream(uri)!!.use {
                orient = ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        } catch (e: Exception) {
        }
        val mx = Matrix()
        when (orient) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> mx.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> mx.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> mx.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { mx.postRotate(90f); mx.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> mx.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { mx.postRotate(-90f); mx.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> mx.postRotate(-90f)
            else -> return bmp
        }
        val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, mx, true)
        if (out !== bmp) bmp.recycle()
        return out
    }

    companion object {
        const val REQ_IMAGE = 1
        const val MAX_SIDE = 3072
    }
}
