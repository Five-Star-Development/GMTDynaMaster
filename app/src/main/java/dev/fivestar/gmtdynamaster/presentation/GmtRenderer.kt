package dev.fivestar.gmtdynamaster.presentation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Color.WHITE
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime
import androidx.core.graphics.withRotation
import java.time.LocalDate

private val RED = Color.rgb(179, 18, 46)
private val BLUE = Color.rgb(26, 58, 140)
private val RED_DIM = Color.rgb(90, 10, 24)
private val BLUE_DIM = Color.rgb(14, 30, 70)

private fun minToCanvasAngle(min: Float) = min / 1440f * 360f - 90f

class GmtRenderer(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    private val store: LocationStore,
) : Renderer.CanvasRenderer2<GmtRenderer.Assets>(
    surfaceHolder, currentUserStyleRepository, watchState,
    CanvasType.HARDWARE,
    interactiveDrawModeUpdateDelayMillis = 1000L,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {

    //wtf is this?
    class Assets : SharedAssets {
        override fun onDestroy() {}
    }

    override suspend fun createSharedAssets() = Assets()

    private var spanKey: Pair<LocalDate, LatLng?>? = null
    private var span = DaySpan.DEFAULT

    private val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = WHITE
        strokeCap = Paint.Cap.ROUND
    }
    private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = WHITE
        strokeCap = Paint.Cap.ROUND
    }
    private val gmtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RED
        strokeCap = Paint.Cap.ROUND
    }


    private val rect = RectF()
    private val path = Path()

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: Assets
    ) {
        updateSpan(zonedDateTime)

        val ambient = renderParameters.drawMode == DrawMode.AMBIENT
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val r = bounds.width() / 2f

        canvas.drawColor(Color.BLACK)
        drawBezel(canvas, bounds, r, ambient)
        drawBezelNumbers(canvas, cx, cy, r, ambient)
        drawDialMarkers(canvas, cx, cy, r)
        drawHands(canvas, cx, cy, r, zonedDateTime, ambient)

    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: Assets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)
    }

    private fun updateSpan(now: ZonedDateTime) {
        val loc = store.location.value
        val key = now.toLocalDate() to loc
        if (key == spanKey) return
        spanKey = key
        span = loc?.let { SunCalc.compute(now.toLocalDate(), now.zone, it) } ?: DaySpan.DEFAULT

        Log.d("GMT", "loc=$loc span=$span zone=${now.zone}")
    }

    // ---------- Bezel ----------

    private fun drawBezel(canvas: Canvas, bounds: Rect, r: Float, ambient: Boolean) {
        val width = r * if (ambient) 0.03f else 0.18f
        bezelPaint.strokeWidth = width
        rect.set(bounds)
        rect.inset(width / 2, width / 2)

        //Night: complete ring
        bezelPaint.color = if (ambient) BLUE_DIM else BLUE
        canvas.drawOval(rect, bezelPaint)

        //Day
        bezelPaint.color = if (ambient) RED_DIM else RED
        if (span.alwaysUp) {
            canvas.drawOval(rect, bezelPaint)
            return
        }

        val sweepMin = ((span.setMin - span.riseMin) % 1440f + 1440f) % 1440f
        canvas.drawArc(
            rect,
            minToCanvasAngle(span.riseMin),
            sweepMin / 1440f * 360f,
            false,
            bezelPaint
        )
    }

    private fun drawBezelNumbers(canvas: Canvas, cx: Float, cy: Float, r: Float, ambient: Boolean) {
        textPaint.textSize = r * 0.11f
        textPaint.color = if (ambient) Color.GRAY else Color.WHITE
        markerPaint.color = textPaint.color
        val numberRadius = r * 0.91f
        val textY = cy - numberRadius - (textPaint.descent() + textPaint.ascent()) / 2

        for (h in 1..23) {
            canvas.withRotation(h / 24f * 360f, cx, cy) {
                if (h % 2 == 0) {
                    drawText(h.toString(), cx, textY, textPaint)
                } else {
                    drawCircle(cx, cy - numberRadius, r * 0.015f, markerPaint)
                }
            }
        }

        // Triangle at 24
        val top = cy - r * 0.965f
        val s = r * 0.065f
        path.reset()
        path.moveTo(cx - s, top)
        path.lineTo(cx + s, top)
        path.lineTo(cx, top + s * 1.5f)
        path.close()
        canvas.drawPath(path, textPaint)
        markerPaint.color = Color.WHITE
    }

    // ---------- Dail ----------

    private fun drawDialMarkers(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val markerRadius = r * 0.70f
        for (h in 0 until 12) {
            canvas.withRotation(h * 30f, cx, cy) {
                when (h) {
                    0 -> { // Triangle at 12
                        val s = r * 0.055f
                        val top = cy - markerRadius - s
                        path.reset()
                        path.moveTo(cx - s, top)
                        path.lineTo(cx + s, top)
                        path.lineTo(cx, top + s * 1.8f)
                        path.close()
                        drawPath(path, markerPaint)
                    }

                    3, 6, 9 -> {
                        markerPaint.strokeWidth = r * 0.05f
                        drawLine(
                            cx,
                            cy - markerRadius - r * 0.05f,
                            cx,
                            cy - markerRadius + r * 0.05f,
                            markerPaint
                        )
                    }

                    else -> drawCircle(cx, cy - markerRadius, r * 0.04f, markerPaint)
                }
            }
        }
    }

    // ---------- Hands ----------

    private fun drawHands(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        t: ZonedDateTime,
        ambient: Boolean
    ) {
        val sec = t.second.toFloat()
        val min = t.minute + sec / 60f
        val hour12 = t.hour % 12 + min / 60f
        val hour24 = t.hour + min / 60f

        drawGmtHand(canvas, cx, cy, hour24 / 24f * 360f, r * 0.70f)

        handPaint.strokeWidth = r * 0.05f
        drawLineHand(canvas, cx, cy, hour12 / 12f * 360f, r * 0.45f, handPaint)

        handPaint.strokeWidth = r * 0.035f
        drawLineHand(canvas, cx, cy, min / 60f * 360f, r * 0.72f, handPaint)

        if (!ambient) {
            handPaint.strokeWidth = r * 0.015f
            drawLineHand(canvas, cx, cy, sec / 60f * 360f, r * 0.78f, handPaint)
        }
        canvas.drawCircle(cx, cy, r * 0.04f, handPaint)
    }

    private fun drawLineHand(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        angle: Float,
        length: Float,
        paint: Paint
    ) {
        canvas.withRotation(angle, cx, cy) {
            drawLine(cx, cy, cx, cy - length, paint)
        }
    }

    /** 24-Hour-Hand */
    private fun drawGmtHand(canvas: Canvas, cx: Float, cy: Float, angle: Float, length: Float) {
        canvas.withRotation(angle, cx, cy) {
            gmtPaint.strokeWidth = length * 0.025f
            drawLine(cx, cy, cx, cy - length, gmtPaint)

            val s = length * 0.07f
            path.reset()
            path.moveTo(cx, cy - length - s * 1.6f)
            path.lineTo(cx - s, cy - length)
            path.lineTo(cx + s, cy - length)
            path.close()
            drawPath(path, gmtPaint)
        }
    }


}
