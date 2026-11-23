package com.example.colorstudy

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MemoActivity : Activity() {
    private lateinit var store: Store
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(0xFF121212.toInt())

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(12), dp(10), dp(12), dp(10))
        val title = TextView(this)
        title.text = "色メモ"
        title.textSize = 20f
        title.setTextColor(0xFFEEEEEE.toInt())
        top.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val share = flatButton(this, "まとめて共有")
        share.setOnClickListener { shareAll() }
        top.addView(share)
        val back = flatButton(this, "戻る", 0xFF3D5A80.toInt())
        back.setOnClickListener { finish() }
        val blp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        blp.setMargins(dp(8), 0, 0, 0)
        top.addView(back, blp)
        root.addView(top)

        val sv = ScrollView(this)
        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        list.setPadding(dp(10), 0, dp(10), dp(16))
        sv.addView(list)
        root.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        refresh()
    }

    private fun refresh() {
        list.removeAllViews()
        val memos = store.all().sortedByDescending { it.time }
        if (memos.isEmpty()) {
            val t = TextView(this)
            t.text = "まだメモがありません。画像で色を拾って「保存」を押すと、ここに並びます。"
            t.setTextColor(0xFF999999.toInt())
            t.setPadding(dp(4), dp(24), dp(4), 0)
            list.addView(t)
            return
        }
        for (m in memos) list.addView(row(m))
    }

    private fun label(s: String, size: Float, color: Int): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setTextColor(color)
        return t
    }

    private fun row(m: Memo): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.setPadding(dp(10), dp(10), dp(10), dp(10))
        val bg = android.graphics.drawable.GradientDrawable()
        bg.setColor(0xFF1E1E1E.toInt())
        bg.cornerRadius = dp(12).toFloat()
        card.background = bg
        val clp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        clp.setMargins(0, dp(6), 0, dp(2))
        card.layoutParams = clp

        val sw = View(this)
        sw.background = swatch(this, m.rgb)
        card.addView(sw, LinearLayout.LayoutParams(dp(64), dp(64)))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(12), 0, 0, 0)
        val hs = ColorMath.hsv(m.rgb)
        col.addView(label(ColorMath.hex(m.rgb) + "  " + ColorMath.describe(m.rgb), 16f, 0xFFEEEEEE.toInt()))
        col.addView(label(
            "RGB " + ColorMath.r(m.rgb) + "," + ColorMath.g(m.rgb) + "," + ColorMath.b(m.rgb) +
                "   HSV " + hs[0].roundToInt() + "° " + (hs[1] * 100).roundToInt() + "% " + (hs[2] * 100).roundToInt() + "%",
            12f, 0xFFAAAAAA.toInt()
        ))
        if (m.note.isNotEmpty()) {
            val n = label(m.note, 14f, 0xFFE0C080.toInt())
            n.setPadding(0, dp(4), 0, 0)
            col.addView(n)
        }
        val date = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(m.time))
        val src = label((if (m.image.isNotEmpty()) m.image + "  ・  " else "") + date, 11f, 0xFF777777.toInt())
        src.setPadding(0, dp(4), 0, 0)
        col.addView(src)

        val btns = LinearLayout(this)
        btns.orientation = LinearLayout.HORIZONTAL
        btns.setPadding(0, dp(8), 0, 0)
        val copy = flatButton(this, "コピー")
        copy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("color", ColorMath.hex(m.rgb)))
            Toast.makeText(this, ColorMath.hex(m.rgb) + " をコピーしました", Toast.LENGTH_SHORT).show()
        }
        val edit = flatButton(this, "メモ編集")
        edit.setOnClickListener { editNote(m) }
        val del = flatButton(this, "削除", 0xFF7A3434.toInt())
        del.setOnClickListener { confirmDelete(m) }
        val blp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        blp.setMargins(0, 0, dp(6), 0)
        btns.addView(copy, blp)
        btns.addView(edit, LinearLayout.LayoutParams(blp))
        btns.addView(del, LinearLayout.LayoutParams(blp))
        col.addView(btns)
        card.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return card
    }

    private fun editNote(m: Memo) {
        val e = EditText(this)
        e.setText(m.note)
        e.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        e.setSelection(e.text.length)
        AlertDialog.Builder(this)
            .setTitle(ColorMath.hex(m.rgb) + " のメモ")
            .setView(e)
            .setPositiveButton("保存") { _, _ ->
                store.updateNote(m.id, e.text.toString().trim())
                refresh()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun confirmDelete(m: Memo) {
        AlertDialog.Builder(this)
            .setMessage(ColorMath.hex(m.rgb) + " のメモを削除しますか?")
            .setPositiveButton("削除") { _, _ ->
                store.delete(m.id)
                refresh()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun shareAll() {
        val memos = store.all().sortedBy { it.time }
        if (memos.isEmpty()) {
            Toast.makeText(this, "共有するメモがありません", Toast.LENGTH_SHORT).show()
            return
        }
        val sb = StringBuilder()
        for (m in memos) {
            val hs = ColorMath.hsv(m.rgb)
            sb.append(ColorMath.hex(m.rgb)).append("  ")
                .append("RGB(").append(ColorMath.r(m.rgb)).append(",").append(ColorMath.g(m.rgb)).append(",").append(ColorMath.b(m.rgb)).append(")  ")
                .append("HSV(").append(hs[0].roundToInt()).append("°,").append((hs[1] * 100).roundToInt()).append("%,").append((hs[2] * 100).roundToInt()).append("%)")
            if (m.note.isNotEmpty()) sb.append("  ").append(m.note)
            sb.append("\n")
        }
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, sb.toString())
        startActivity(Intent.createChooser(i, "色メモを共有"))
    }
}
