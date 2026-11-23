package com.example.colorstudy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class Memo(val id: Long, val rgb: Int, var note: String, val image: String, val time: Long)

/** 色メモの保存(アプリ内のファイル memos.json) */
class Store(private val ctx: Context) {
    private fun file(): File = File(ctx.filesDir, "memos.json")

    fun all(): MutableList<Memo> {
        val out = mutableListOf<Memo>()
        try {
            val f = file()
            if (!f.exists()) return out
            val a = JSONArray(f.readText())
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(Memo(o.getLong("id"), o.getInt("rgb") and 0xFFFFFF, o.optString("note"), o.optString("image"), o.optLong("time")))
            }
        } catch (e: Exception) {
        }
        return out
    }

    private fun write(list: List<Memo>) {
        val a = JSONArray()
        for (m in list) {
            val o = JSONObject()
            o.put("id", m.id)
            o.put("rgb", m.rgb)
            o.put("note", m.note)
            o.put("image", m.image)
            o.put("time", m.time)
            a.put(o)
        }
        val tmp = File(ctx.filesDir, "memos.json.tmp")
        tmp.writeText(a.toString())
        tmp.renameTo(file())
    }

    fun add(rgb: Int, note: String, image: String) {
        val list = all()
        val now = System.currentTimeMillis()
        list.add(Memo(now, rgb, note, image, now))
        write(list)
    }

    fun updateNote(id: Long, note: String) {
        val list = all()
        for (m in list) if (m.id == id) m.note = note
        write(list)
    }

    fun delete(id: Long) {
        write(all().filter { it.id != id })
    }
}
