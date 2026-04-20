package com.example.aplikacjataty

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SignatureCaptureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val NORMALIZED_WIDTH = 9600f
        private const val NORMALIZED_HEIGHT = 6000f
    }

    private val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val drawPath = Path()
    private val points = mutableListOf<SignaturePoint>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        canvas.drawPath(drawPath, drawPaint)
    }

    fun clearSignature() {
        drawPath.reset()
        points.clear()
        invalidate()
    }

    fun getCapturedPoints(): List<SignaturePoint> = points.toList()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val toolType = event.getToolType(0)

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath.moveTo(event.x, event.y)
                addPointFromEvent(event, event.x, event.y, event.pressure, "DOWN")
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val historySize = event.historySize
                for (i in 0 until historySize) {
                    val hx = event.getHistoricalX(i)
                    val hy = event.getHistoricalY(i)
                    drawPath.lineTo(hx, hy)
                    addHistoricalPointFromEvent(event, i, hx, hy, "MOVE_HIST")
                }

                drawPath.lineTo(event.x, event.y)
                addPointFromEvent(event, event.x, event.y, event.pressure, "MOVE")
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                drawPath.lineTo(event.x, event.y)
                addPointFromEvent(event, event.x, event.y, event.pressure, "UP")
                invalidate()
                return true
            }
        }

        return toolType == MotionEvent.TOOL_TYPE_STYLUS || super.onTouchEvent(event)
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        if (event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            return super.onHoverEvent(event)
        }

        val eventType = when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER -> "HOVER_ENTER"
            MotionEvent.ACTION_HOVER_MOVE -> "HOVER_MOVE"
            MotionEvent.ACTION_HOVER_EXIT -> "HOVER_EXIT"
            else -> null
        }

        if (eventType != null) {
            addPointFromEvent(event, event.x, event.y, event.pressure, eventType)
            return true
        }

        return super.onHoverEvent(event)
    }

    private fun addHistoricalPointFromEvent(
        event: MotionEvent,
        historyIndex: Int,
        rawX: Float,
        rawY: Float,
        eventType: String
    ) {
        val (nx, ny) = normalize(rawX, rawY)
        val toolType = toolTypeToString(event.getToolType(0))

        val tilt = event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, historyIndex)
        val orientation = event.getHistoricalAxisValue(MotionEvent.AXIS_ORIENTATION, historyIndex)
        val distance = event.getHistoricalAxisValue(MotionEvent.AXIS_DISTANCE, historyIndex)

        points.add(
            SignaturePoint(
                time = event.getHistoricalEventTime(historyIndex),
                x = nx,
                y = ny,
                pressure = event.getHistoricalPressure(historyIndex),
                eventType = eventType,
                toolType = toolType,
                tilt = if (tilt.isFinite()) tilt else 0f,
                orientation = if (orientation.isFinite()) orientation else 0f,
                distance = if (distance.isFinite()) distance else 0f
            )
        )
    }

    private fun addPointFromEvent(
        event: MotionEvent,
        rawX: Float,
        rawY: Float,
        pressure: Float,
        eventType: String
    ) {
        val (nx, ny) = normalize(rawX, rawY)
        val toolType = toolTypeToString(event.getToolType(0))

        val tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
        val orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)
        val distance = event.getAxisValue(MotionEvent.AXIS_DISTANCE)

        points.add(
            SignaturePoint(
                time = event.eventTime,
                x = nx,
                y = ny,
                pressure = pressure,
                eventType = eventType,
                toolType = toolType,
                tilt = if (tilt.isFinite()) tilt else 0f,
                orientation = if (orientation.isFinite()) orientation else 0f,
                distance = if (distance.isFinite()) distance else 0f
            )
        )
    }

    private fun normalize(rawX: Float, rawY: Float): Pair<Float, Float> {
        val w = if (width > 0) width.toFloat() else 1f
        val h = if (height > 0) height.toFloat() else 1f
        val scaledX = rawX * (NORMALIZED_WIDTH / w)
        val scaledY = rawY * (NORMALIZED_HEIGHT / h)
        return scaledX to scaledY
    }

    private fun toolTypeToString(toolType: Int): String {
        return when (toolType) {
            MotionEvent.TOOL_TYPE_STYLUS -> "STYLUS"
            MotionEvent.TOOL_TYPE_FINGER -> "FINGER"
            MotionEvent.TOOL_TYPE_MOUSE -> "MOUSE"
            MotionEvent.TOOL_TYPE_ERASER -> "ERASER"
            else -> "UNKNOWN"
        }
    }
}
