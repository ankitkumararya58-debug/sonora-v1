package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.CoverShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** Draws bitmaps for the home screen widgets (RemoteViews can only show bitmaps, not custom drawing). */
object WidgetRenderer {

    // ------------------------------------------------------------------
    // Spotify-code style bar strip
    // ------------------------------------------------------------------
    fun visualizerBitmap(
        seedText: String,
        color: Int,
        pixelStyle: Boolean,
        dimmed: Boolean,
        widthPx: Int = 480,
        heightPx: Int = 72
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = if (dimmed) 70 else 150
            strokeCap = if (pixelStyle) Paint.Cap.BUTT else Paint.Cap.ROUND
        }
        val count = 36
        val slot = widthPx.toFloat() / count
        val barWidth = slot * 0.55f
        paint.strokeWidth = barWidth
        val rnd = java.util.Random(seedText.hashCode().toLong())
        var prev = 0.5f
        val minH = heightPx * 0.12f
        val blocks = 8
        for (i in 0 until count) {
            prev = prev * 0.45f + rnd.nextFloat() * 0.55f
            var h = minH + (heightPx - minH) * prev.coerceIn(0f, 1f)
            if (pixelStyle) {
                val step = heightPx.toFloat() / blocks
                h = (Math.round(h / step).coerceAtLeast(1)) * step
            }
            val x = slot * i + slot / 2f
            val top = (heightPx - h) / 2f
            val bottom = top + h
            val inset = if (pixelStyle) 0f else barWidth / 2f
            canvas.drawLine(x, top + inset, x, bottom - inset, paint)
        }
        return bmp
    }

    // ------------------------------------------------------------------
    // Shaped cover for the standalone Cover widget
    // ------------------------------------------------------------------
    fun shapedCover(
        context: Context,
        art: Bitmap?,
        shape: CoverShape,
        canvasPx: Int,
        sizePercent: Int,
        cornerPercent: Int
    ): Bitmap {
        val out = Bitmap.createBitmap(canvasPx, canvasPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // keep some room for the hard shadow of the pixel shape
        val usable = if (shape == CoverShape.PIXEL) 0.92f else 1f
        val side = (canvasPx * (sizePercent.coerceIn(30, 100) / 100f) * usable).toInt().coerceAtLeast(16)
        val left = (canvasPx - side) / 2f
        val top = (canvasPx - side) / 2f

        val source = art ?: placeholder(context, side)
        val minSide = min(source.width, source.height)
        val cropped = Bitmap.createBitmap(
            source,
            (source.width - minSide) / 2,
            (source.height - minSide) / 2,
            minSide,
            minSide
        )
        val scaled = if (shape == CoverShape.PIXEL) {
            // genuine pixel-art look: shrink with averaging, blow up WITHOUT smoothing
            val tiny = Bitmap.createScaledBitmap(cropped, 40, 40, true)
            Bitmap.createScaledBitmap(tiny, side, side, false)
        } else {
            Bitmap.createScaledBitmap(cropped, side, side, true)
        }

        val path = buildPath(shape, left, top, side.toFloat(), cornerPercent)

        if (shape == CoverShape.PIXEL) {
            val shadow = Path(path)
            shadow.offset(side * 0.045f, side * 0.045f)
            canvas.drawPath(shadow, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111111") })
        }

        val shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = Matrix()
        matrix.postTranslate(left, top)
        shader.setLocalMatrix(matrix)
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
            isFilterBitmap = shape != CoverShape.PIXEL
        })

        if (shape == CoverShape.PIXEL) {
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = side * 0.03f
                strokeJoin = Paint.Join.MITER
                color = Color.parseColor("#111111")
            })
        }
        return out
    }

    private fun placeholder(context: Context, side: Int): Bitmap {
        val bmp = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#1E232B"))
        val d = ContextCompat.getDrawable(context, R.drawable.ic_widget_music_default)
        if (d != null) {
            val pad = side / 4
            d.setBounds(pad, pad, side - pad, side - pad)
            d.draw(canvas)
        }
        return bmp
    }

    private fun buildPath(shape: CoverShape, left: Float, top: Float, side: Float, cornerPercent: Int): Path {
        val rect = RectF(left, top, left + side, top + side)
        val path = Path()
        when (shape) {
            CoverShape.CIRCLE -> path.addOval(rect, Path.Direction.CW)
            CoverShape.ROUNDED -> {
                val r = side * (cornerPercent.coerceIn(8, 50) / 100f)
                path.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            CoverShape.SQUIRCLE -> {
                // superellipse |x|^4 + |y|^4 = 1
                val cx = rect.centerX()
                val cy = rect.centerY()
                val a = side / 2f
                var first = true
                var deg = 0
                while (deg <= 360) {
                    val t = Math.toRadians(deg.toDouble())
                    val c = cos(t)
                    val s = sin(t)
                    val x = cx + (a * sign(c) * abs(c).pow(0.5)).toFloat()
                    val y = cy + (a * sign(s) * abs(s).pow(0.5)).toFloat()
                    if (first) {
                        path.moveTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                    }
                    deg += 3
                }
                path.close()
            }
            CoverShape.PIXEL -> {
                val u = side / 16f
                val l = left
                val t = top
                val r = left + side
                val b = top + side
                // stepped (8-bit) corners
                path.moveTo(l + 2 * u, t)
                path.lineTo(r - 2 * u, t)
                path.lineTo(r - 2 * u, t + u)
                path.lineTo(r - u, t + u)
                path.lineTo(r - u, t + 2 * u)
                path.lineTo(r, t + 2 * u)
                path.lineTo(r, b - 2 * u)
                path.lineTo(r - u, b - 2 * u)
                path.lineTo(r - u, b - u)
                path.lineTo(r - 2 * u, b - u)
                path.lineTo(r - 2 * u, b)
                path.lineTo(l + 2 * u, b)
                path.lineTo(l + 2 * u, b - u)
                path.lineTo(l + u, b - u)
                path.lineTo(l + u, b - 2 * u)
                path.lineTo(l, b - 2 * u)
                path.lineTo(l, t + 2 * u)
                path.lineTo(l + u, t + 2 * u)
                path.lineTo(l + u, t + u)
                path.lineTo(l + 2 * u, t + u)
                path.close()
            }
        }
        return path
    }
}
