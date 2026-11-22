package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/** Pixel size of one home screen widget (kept small so RemoteViews bitmaps stay light). */
class WidgetMetrics(val width: Int, val height: Int, val density: Float)

/** Draws bitmaps for the home screen widgets (RemoteViews can only show bitmaps, not custom drawing). */
object WidgetRenderer {

    private const val MAX_BG_PIXELS = 1_000_000L
    private val PIXEL_INK = Color.parseColor("#111111")
    private val PIXEL_PAPER = Color.parseColor("#F4F4F4")

    // ------------------------------------------------------------------
    // Widget size
    // ------------------------------------------------------------------
    fun metricsFor(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        defaultHeightDp: Int
    ): WidgetMetrics {
        val options = manager.getAppWidgetOptions(widgetId)
        val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val wDp = (if (portrait) options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        else options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 250)).coerceAtLeast(60)
        val hDp = (if (portrait) options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, defaultHeightDp)
        else options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, defaultHeightDp)).coerceAtLeast(40)

        var density = context.resources.displayMetrics.density
        var w = (wDp * density).toInt()
        var h = (hDp * density).toInt()
        val pixels = w.toLong() * h
        if (pixels > MAX_BG_PIXELS) {
            val s = sqrt(MAX_BG_PIXELS.toDouble() / pixels).toFloat()
            w = (w * s).toInt()
            h = (h * s).toInt()
            density *= s
        }
        return WidgetMetrics(max(w, 1), max(h, 1), density)
    }

    // ------------------------------------------------------------------
    // Minimal "bars + dots" strip. Bars = played part, dots = what is left.
    // progress < 0 means "length unknown" (all bars).
    // ------------------------------------------------------------------
    fun progressBarsBitmap(
        seedText: String,
        progress: Float,
        color: Int,
        pixelStyle: Boolean,
        dimmed: Boolean,
        widthPx: Int = 336,
        heightPx: Int = 64
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val count = 20
        val slot = widthPx.toFloat() / count
        val barW = slot * 0.38f
        val dot = slot * 0.30f
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = if (dimmed) 110 else 235
            strokeWidth = barW
            strokeCap = if (pixelStyle) Paint.Cap.BUTT else Paint.Cap.ROUND
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = if (dimmed) 60 else 120
        }
        val played = if (progress < 0f) count else Math.round(progress.coerceIn(0f, 1f) * count)
        val rnd = java.util.Random(seedText.hashCode().toLong())
        var prev = 0.5f
        val minH = heightPx * 0.22f
        val blocks = 6
        for (i in 0 until count) {
            prev = prev * 0.4f + rnd.nextFloat() * 0.6f
            val cx = slot * i + slot / 2f
            val cy = heightPx / 2f
            if (i < played) {
                var h = minH + (heightPx - minH) * prev.coerceIn(0f, 1f)
                if (pixelStyle) {
                    val step = heightPx.toFloat() / blocks
                    h = Math.round(h / step).coerceAtLeast(1) * step
                }
                val inset = if (pixelStyle) 0f else barW / 2f
                canvas.drawLine(cx, cy - h / 2f + inset, cx, cy + h / 2f - inset, barPaint)
            } else if (pixelStyle) {
                canvas.drawRect(cx - dot / 2f, cy - dot / 2f, cx + dot / 2f, cy + dot / 2f, dotPaint)
            } else {
                canvas.drawCircle(cx, cy, dot / 2f, dotPaint)
            }
        }
        return bmp
    }

    // ------------------------------------------------------------------
    // Thin progress line with a knob (Default theme)
    // ------------------------------------------------------------------
    fun progressLineBitmap(progress: Float, color: Int, widthPx: Int = 480, heightPx: Int = 48): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val thickness = heightPx * 0.09f
        val knobW = thickness * 1.5f
        val knobH = heightPx * 0.62f
        val cy = heightPx / 2f
        val p = progress.coerceIn(0f, 1f)
        val x = (knobW / 2f + (widthPx - knobW) * p)

        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = 80
            strokeWidth = thickness
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(thickness, cy, widthPx - thickness, cy, track)

        val filled = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = thickness
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(thickness, cy, x, cy, filled)

        val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(
            RectF(x - knobW / 2f, cy - knobH / 2f, x + knobW / 2f, cy + knobH / 2f),
            knobW / 2f, knobW / 2f, knob
        )
        return bmp
    }

    // ------------------------------------------------------------------
    // Default theme: cover fills the whole widget, rounded corners, dark fade for the text
    // ------------------------------------------------------------------
    fun defaultBackground(context: Context, art: Bitmap?, w: Int, h: Int, radiusPx: Float): Bitmap {
        val content = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(content)
        c.drawColor(Color.parseColor("#1E232B"))

        if (art != null && art.width > 0 && art.height > 0) {
            val scale = max(w.toFloat() / art.width, h.toFloat() / art.height)
            val m = Matrix()
            m.postScale(scale, scale)
            m.postTranslate((w - art.width * scale) / 2f, (h - art.height * scale) / 2f)
            c.drawBitmap(art, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        } else {
            val d = ContextCompat.getDrawable(context, R.drawable.ic_widget_music_default)
            if (d != null) {
                val s = (min(w, h) * 0.45f).toInt()
                val left = (w - s) / 2
                val top = (h - s) / 2
                d.setBounds(left, top, left + s, top + s)
                d.draw(c)
            }
        }

        // soft dark fade: light at the top, strong at the bottom where the text sits
        val fade = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(
                    Color.parseColor("#33000000"),
                    Color.parseColor("#14000000"),
                    Color.parseColor("#99000000"),
                    Color.parseColor("#E6000000")
                ),
                floatArrayOf(0f, 0.35f, 0.72f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fade)

        // round the corners with an anti-aliased mask
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val path = Path()
        val r = min(radiusPx, min(w, h) / 2f)
        path.addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, Path.Direction.CW)
        Canvas(out).drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(content, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        })
        content.recycle()
        return out
    }

    // ------------------------------------------------------------------
    // Pixel theme: off-white body, black outline, stepped (8-bit) corners, no shadow
    // ------------------------------------------------------------------
    fun pixelBackground(w: Int, h: Int, unit: Float): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val stroke = unit
        val half = stroke / 2f
        val path = steppedPath(half, half, w - half, h - half, unit)
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = PIXEL_PAPER
        })
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeJoin = Paint.Join.MITER
            color = PIXEL_INK
        })
        return out
    }

    /** Rectangle whose four corners are cut in two small steps. */
    private fun steppedPath(l: Float, t: Float, r: Float, b: Float, u: Float): Path {
        val p = Path()
        p.moveTo(l + 2 * u, t)
        p.lineTo(r - 2 * u, t)
        p.lineTo(r - 2 * u, t + u)
        p.lineTo(r - u, t + u)
        p.lineTo(r - u, t + 2 * u)
        p.lineTo(r, t + 2 * u)
        p.lineTo(r, b - 2 * u)
        p.lineTo(r - u, b - 2 * u)
        p.lineTo(r - u, b - u)
        p.lineTo(r - 2 * u, b - u)
        p.lineTo(r - 2 * u, b)
        p.lineTo(l + 2 * u, b)
        p.lineTo(l + 2 * u, b - u)
        p.lineTo(l + u, b - u)
        p.lineTo(l + u, b - 2 * u)
        p.lineTo(l, b - 2 * u)
        p.lineTo(l, t + 2 * u)
        p.lineTo(l + u, t + 2 * u)
        p.lineTo(l + u, t + u)
        p.lineTo(l + 2 * u, t + u)
        p.close()
        return p
    }

    // ------------------------------------------------------------------
    // Round icon of the music app that is playing (Spotify, YouTube Music...)
    // ------------------------------------------------------------------
    fun appIconCircle(context: Context, packageName: String?, sizePx: Int): Bitmap? {
        if (packageName.isNullOrEmpty() || sizePx <= 0) return null
        return try {
            val d = context.packageManager.getApplicationIcon(packageName)
            val icon = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val ic = Canvas(icon)
            d.setBounds(0, 0, sizePx, sizePx)
            d.draw(ic)
            val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            Canvas(out).drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(icon, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            })
            icon.recycle()
            out
        } catch (t: Throwable) {
            null
        }
    }

    // ------------------------------------------------------------------
    // Shaped cover (small covers inside widgets + the standalone Cover widget)
    //  ROUNDED = rounded rectangle that fills the widget (never a plain rectangle)
    //  CIRCLE / SQUIRCLE / PIXEL = square, centred
    // ------------------------------------------------------------------
    fun shapedCover(
        context: Context,
        art: Bitmap?,
        shape: CoverShape,
        widthPx: Int,
        heightPx: Int,
        sizePercent: Int,
        cornerPercent: Int
    ): Bitmap {
        val out = Bitmap.createBitmap(max(widthPx, 1), max(heightPx, 1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val scalePct = sizePercent.coerceIn(30, 100) / 100f

        val boxW: Int
        val boxH: Int
        if (shape == CoverShape.ROUNDED) {
            boxW = (widthPx * scalePct).toInt().coerceAtLeast(16)
            boxH = (heightPx * scalePct).toInt().coerceAtLeast(16)
        } else {
            val pixelFactor = if (shape == CoverShape.PIXEL) 0.97f else 1f
            val side = (min(widthPx, heightPx) * scalePct * pixelFactor).toInt().coerceAtLeast(16)
            boxW = side
            boxH = side
        }
        val left = (widthPx - boxW) / 2f
        val top = (heightPx - boxH) / 2f

        val source = art ?: placeholder(context, max(boxW, boxH))

        // centre-crop the source to the box aspect ratio
        val boxRatio = boxW.toFloat() / boxH
        val srcRatio = source.width.toFloat() / source.height
        val cropW: Int
        val cropH: Int
        if (srcRatio > boxRatio) {
            cropH = source.height
            cropW = (source.height * boxRatio).toInt().coerceIn(1, source.width)
        } else {
            cropW = source.width
            cropH = (source.width / boxRatio).toInt().coerceIn(1, source.height)
        }
        val cropped = Bitmap.createBitmap(
            source,
            (source.width - cropW) / 2,
            (source.height - cropH) / 2,
            cropW,
            cropH
        )
        val scaled = if (shape == CoverShape.PIXEL) {
            // genuine pixel-art look: shrink with averaging, blow up WITHOUT smoothing
            val tiny = Bitmap.createScaledBitmap(cropped, 40, 40, true)
            Bitmap.createScaledBitmap(tiny, boxW, boxH, false)
        } else {
            Bitmap.createScaledBitmap(cropped, boxW, boxH, true)
        }

        val path = buildPath(shape, left, top, boxW.toFloat(), boxH.toFloat(), cornerPercent)

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
                strokeWidth = max(boxW, boxH) * 0.03f
                strokeJoin = Paint.Join.MITER
                color = PIXEL_INK
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

    private fun buildPath(
        shape: CoverShape,
        left: Float,
        top: Float,
        boxW: Float,
        boxH: Float,
        cornerPercent: Int
    ): Path {
        val rect = RectF(left, top, left + boxW, top + boxH)
        val path = Path()
        when (shape) {
            CoverShape.CIRCLE -> path.addOval(rect, Path.Direction.CW)
            CoverShape.ROUNDED -> {
                val r = min(boxW, boxH) * (cornerPercent.coerceIn(8, 50) / 100f)
                path.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            CoverShape.SQUIRCLE -> {
                // superellipse |x|^4 + |y|^4 = 1
                val cx = rect.centerX()
                val cy = rect.centerY()
                val a = boxW / 2f
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
                val u = boxW / 16f
                path.addPath(steppedPath(left, top, left + boxW, top + boxH, u))
            }
        }
        return path
    }
}
