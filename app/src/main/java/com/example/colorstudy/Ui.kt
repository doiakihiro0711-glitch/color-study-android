package com.example.colorstudy

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.Button

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun swatch(ctx: Context, rgb: Int): GradientDrawable {
    val g = GradientDrawable()
    g.setColor(0xFF000000.toInt() or rgb)
    g.cornerRadius = ctx.dp(10).toFloat()
    g.setStroke(ctx.dp(1), 0xFF666666.toInt())
    return g
}

fun flatButton(ctx: Context, text: String, bg: Int = 0xFF2E2E2E.toInt()): Button {
    val b = Button(ctx)
    b.text = text
    b.isAllCaps = false
    b.setTextColor(0xFFEEEEEE.toInt())
    b.textSize = 13f
    val g = GradientDrawable()
    g.setColor(bg)
    g.cornerRadius = ctx.dp(8).toFloat()
    b.background = g
    b.minHeight = 0
    b.minimumHeight = 0
    b.minWidth = 0
    b.minimumWidth = 0
    b.setPadding(ctx.dp(12), ctx.dp(8), ctx.dp(12), ctx.dp(8))
    return b
}
