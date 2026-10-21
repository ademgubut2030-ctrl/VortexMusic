package com.example.vortexmusic

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.View

// ================== NEON ZEMİN ==================

class NeonBackgroundDrawable(
    private val top: Int,
    private val bottom: Int,
    private val purple: Int,
    private val blue: Int,
    private val density: Float
) : Drawable() {

    private val fillPaint = Paint()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha shl 24)

    private fun glowLine(
        canvas: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int
    ) {
        val widths = floatArrayOf(14f, 8f, 4f, 1.6f)
        val alphas = intArrayOf(14, 28, 56, 140)
        for (i in widths.indices) {
            linePaint.strokeWidth = widths[i] * density
            linePaint.color = withAlpha(color, alphas[i])
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        fillPaint.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(bounds, fillPaint)
        glowLine(canvas, -0.05f * w, 0.19f * h, 1.05f * w, 0.00f * h, purple)
        glowLine(canvas, -0.05f * w, 0.25f * h, 1.05f * w, 0.06f * h, purple)
        glowLine(canvas, -0.05f * w, 0.86f * h, 0.55f * w, 1.04f * h, blue)
        glowLine(canvas, -0.05f * w, 0.93f * h, 0.30f * w, 1.04f * h, blue)
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

// ================== MİNİMALİST İKONLAR ==================

class IconDrawable(
    private val kind: Int,
    private val tint: Int,
    private val sizePx: Int,
    private val filled: Boolean = false,
    private val glowColor: Int = 0
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = buildPath()

    override fun getIntrinsicWidth(): Int = sizePx
    override fun getIntrinsicHeight(): Int = sizePx

    override fun draw(canvas: Canvas) {
        val w = bounds.width()
        val h = bounds.height()
        if (w <= 0 || h <= 0) return
        val s = minOf(w, h) / 24f
        canvas.save()
        canvas.translate(
            bounds.left + (w - 24f * s) / 2f,
            bounds.top + (h - 24f * s) / 2f
        )
        canvas.scale(s, s)
        val base = if (kind == PAUSE) 3.2f else 1.9f
        val doFill = filled && (kind == PLAY || kind == HEART || kind == NEXT || kind == PREV)
        if (glowColor != 0) {
            drawPass(canvas, glowColor, base + 4.5f, 40, false)
            drawPass(canvas, glowColor, base + 2.2f, 80, false)
        }
        drawPass(canvas, tint, base, 255, doFill)
        canvas.restore()
    }

    private fun drawPass(canvas: Canvas, color: Int, width: Float, alpha: Int, fill: Boolean) {
        paint.color = color
        paint.alpha = alpha
        paint.strokeWidth = width
        paint.style = if (fill) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
        canvas.drawPath(path, paint)
    }

    private fun buildPath(): Path {
        val p = Path()
        when (kind) {
            HOME -> {
                p.moveTo(3.5f, 11f); p.lineTo(12f, 4f); p.lineTo(20.5f, 11f)
                p.moveTo(6f, 9.5f); p.lineTo(6f, 20f); p.lineTo(18f, 20f); p.lineTo(18f, 9.5f)
            }
            SEARCH -> {
                p.addCircle(10.5f, 10.5f, 6.5f, Path.Direction.CW)
                p.moveTo(15.5f, 15.5f); p.lineTo(20.5f, 20.5f)
            }
            NOTE -> {
                p.moveTo(9.5f, 17.5f); p.lineTo(9.5f, 6f); p.lineTo(18.5f, 4f); p.lineTo(18.5f, 15.5f)
                p.addCircle(7f, 17.5f, 2.5f, Path.Direction.CW)
                p.addCircle(16f, 15.5f, 2.5f, Path.Direction.CW)
            }
            HEART -> {
                p.moveTo(12f, 20.5f)
                p.cubicTo(5f, 15f, 3f, 11.5f, 3f, 8.5f)
                p.cubicTo(3f, 5.7f, 5f, 4f, 7.2f, 4f)
                p.cubicTo(9.2f, 4f, 11f, 5.2f, 12f, 7f)
                p.cubicTo(13f, 5.2f, 14.8f, 4f, 16.8f, 4f)
                p.cubicTo(19f, 4f, 21f, 5.7f, 21f, 8.5f)
                p.cubicTo(21f, 11.5f, 19f, 15f, 12f, 20.5f)
                p.close()
            }
            PLAY -> {
                p.moveTo(8f, 5f); p.lineTo(19f, 12f); p.lineTo(8f, 19f); p.close()
            }
            PAUSE -> {
                p.moveTo(8.5f, 5.5f); p.lineTo(8.5f, 18.5f)
                p.moveTo(15.5f, 5.5f); p.lineTo(15.5f, 18.5f)
            }
            NEXT -> {
                p.moveTo(6f, 5.5f); p.lineTo(15.5f, 12f); p.lineTo(6f, 18.5f); p.close()
                p.moveTo(18.5f, 5.5f); p.lineTo(18.5f, 18.5f)
            }
            PREV -> {
                p.moveTo(18f, 5.5f); p.lineTo(8.5f, 12f); p.lineTo(18f, 18.5f); p.close()
                p.moveTo(5.5f, 5.5f); p.lineTo(5.5f, 18.5f)
            }
            SHUFFLE -> {
                p.moveTo(3f, 7f); p.lineTo(7f, 7f)
                p.cubicTo(12.5f, 7f, 11f, 17f, 16.5f, 17f); p.lineTo(20.5f, 17f)
                p.moveTo(3f, 17f); p.lineTo(7f, 17f)
                p.cubicTo(8.7f, 17f, 9.9f, 16.1f, 10.8f, 14.8f)
                p.moveTo(13.2f, 9.2f)
                p.cubicTo(14.1f, 7.9f, 15.3f, 7f, 16.5f, 7f); p.lineTo(20.5f, 7f)
                p.moveTo(18f, 14.5f); p.lineTo(20.5f, 17f); p.lineTo(18f, 19.5f)
                p.moveTo(18f, 4.5f); p.lineTo(20.5f, 7f); p.lineTo(18f, 9.5f)
            }
            REPEAT -> {
                p.moveTo(4f, 11f); p.lineTo(4f, 9f)
                p.cubicTo(4f, 7.3f, 5.3f, 6f, 7f, 6f); p.lineTo(20f, 6f)
                p.moveTo(17f, 3f); p.lineTo(20f, 6f); p.lineTo(17f, 9f)
                p.moveTo(20f, 13f); p.lineTo(20f, 15f)
                p.cubicTo(20f, 16.7f, 18.7f, 18f, 17f, 18f); p.lineTo(4f, 18f)
                p.moveTo(7f, 15f); p.lineTo(4f, 18f); p.lineTo(7f, 21f)
            }
            FOLDER, FOLDER_PLUS -> {
                p.moveTo(3f, 7f); p.lineTo(3f, 18.5f)
                p.cubicTo(3f, 19.3f, 3.7f, 20f, 4.5f, 20f); p.lineTo(19.5f, 20f)
                p.cubicTo(20.3f, 20f, 21f, 19.3f, 21f, 18.5f); p.lineTo(21f, 9.5f)
                p.cubicTo(21f, 8.7f, 20.3f, 8f, 19.5f, 8f); p.lineTo(11f, 8f)
                p.lineTo(9f, 5.5f); p.lineTo(4.5f, 5.5f)
                p.cubicTo(3.7f, 5.5f, 3f, 6.2f, 3f, 7f)
                p.close()
                if (kind == FOLDER_PLUS) {
                    p.moveTo(12f, 11.5f); p.lineTo(12f, 17f)
                    p.moveTo(9.25f, 14.25f); p.lineTo(14.75f, 14.25f)
                }
            }
            PLUS -> {
                p.moveTo(12f, 5f); p.lineTo(12f, 19f)
                p.moveTo(5f, 12f); p.lineTo(19f, 12f)
            }
            ARTIST -> {
                p.addCircle(12f, 8f, 3.8f, Path.Direction.CW)
                p.moveTo(5f, 20f)
                p.cubicTo(5f, 15.5f, 8f, 14f, 12f, 14f)
                p.cubicTo(16f, 14f, 19f, 15.5f, 19f, 20f)
            }
            DISC -> {
                p.addCircle(12f, 12f, 8.5f, Path.Direction.CW)
                p.addCircle(12f, 12f, 2.5f, Path.Direction.CW)
            }
            DOWNLOAD -> {
                p.moveTo(12f, 4f); p.lineTo(12f, 15f)
                p.moveTo(7f, 10.5f); p.lineTo(12f, 15.5f); p.lineTo(17f, 10.5f)
                p.moveTo(5f, 20f); p.lineTo(19f, 20f)
            }
            CLOCK -> {
                p.addCircle(12f, 13.5f, 7.5f, Path.Direction.CW)
                p.moveTo(12f, 13.5f); p.lineTo(12f, 9.5f)
                p.moveTo(9.5f, 3.5f); p.lineTo(14.5f, 3.5f)
            }
            CHEVRON -> {
                p.moveTo(15f, 5f); p.lineTo(8f, 12f); p.lineTo(15f, 19f)
            }
            CHART -> {
                p.moveTo(6f, 20f); p.lineTo(6f, 13f)
                p.moveTo(12f, 20f); p.lineTo(12f, 5f)
                p.moveTo(18f, 20f); p.lineTo(18f, 10f)
            }
            EQ -> {
                p.moveTo(6f, 4f); p.lineTo(6f, 20f)
                p.moveTo(12f, 4f); p.lineTo(12f, 20f)
                p.moveTo(18f, 4f); p.lineTo(18f, 20f)
                p.addCircle(6f, 9f, 2.2f, Path.Direction.CW)
                p.addCircle(12f, 15f, 2.2f, Path.Direction.CW)
                p.addCircle(18f, 8f, 2.2f, Path.Direction.CW)
            }
        }
        return p
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        const val HOME = 0
        const val SEARCH = 1
        const val NOTE = 2
        const val HEART = 3
        const val PLAY = 4
        const val PAUSE = 5
        const val NEXT = 6
        const val PREV = 7
        const val SHUFFLE = 8
        const val REPEAT = 9
        const val FOLDER = 10
        const val FOLDER_PLUS = 11
        const val PLUS = 12
        const val ARTIST = 13
        const val DISC = 14
        const val DOWNLOAD = 15
        const val CLOCK = 16
        const val CHEVRON = 17
        const val CHART = 18
        const val EQ = 19
    }
}

// ================== GRAFİK ==================

class BarChartView(
    context: Context,
    private val values: LongArray,
    private val labels: Array<String>,
    private val colorTop: Int,
    private val colorBottom: Int,
    private val textColor: Int,
    private val density: Float
) : View(context) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11f * density
        color = textColor
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (160f * density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val n = values.size
        if (n == 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val labelH = 20f * density
        val chartH = h - labelH
        var max = 1L
        for (v in values) if (v > max) max = v
        val slot = w / n
        val barW = slot * 0.62f
        for (i in 0 until n) {
            val has = values[i] > 0
            val bh = if (has) maxOf(values[i].toFloat() / max * chartH, 5f * density) else 2f * density
            val left = i * slot + (slot - barW) / 2f
            val top = chartH - bh
            barPaint.shader = LinearGradient(0f, top, 0f, chartH, colorTop, colorBottom, Shader.TileMode.CLAMP)
            barPaint.alpha = if (has) 255 else 70
            canvas.drawRoundRect(left, top, left + barW, chartH, 6f * density, 6f * density, barPaint)
            if (labels[i].isNotEmpty()) {
                canvas.drawText(labels[i], i * slot + slot / 2f, h - 4f * density, textPaint)
            }
        }
    }
}