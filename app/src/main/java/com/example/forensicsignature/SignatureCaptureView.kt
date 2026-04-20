package com.example.forensicsignature

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class SignatureCaptureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val LOGICAL_WIDTH = 960
        const val LOGICAL_HEIGHT = 540
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 4f
    }

    private val hoverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 70, 70, 70)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2f
    }

    private val drawPath = Path()
    private val hoverPath = Path()
    private val samples = mutableListOf<SignatureSample>()

    var onSampleCountChanged: ((Int) -> Unit)? = null

    fun clearAll() {
        drawPath.reset()
        hoverPath.reset()
        samples.clear()
        onSampleCountChanged?.invoke(0)
        invalidate()
    }

    fun getSamplesSnapshot(): List<SignatureSample> = samples.toList()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        canvas.drawPath(drawPath, strokePaint)
        canvas.drawPath(hoverPath, hoverPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex
        val pointerId = event.getPointerId(index)

        when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(index)
                val y = event.getY(index)
                drawPath.moveTo(x, y)
                appendSample(event, index, CaptureEventType.DOWN)
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val x = event.getX(i)
                    val y = event.getY(i)
                    drawPath.lineTo(x, y)
                    appendSample(event, i, CaptureEventType.MOVE)
                    appendHistoricalSamples(event, i)
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                val x = event.getX(index)
                val y = event.getY(index)
                drawPath.lineTo(x, y)
                appendSample(event, index, CaptureEventType.UP)
            }
        }

        onSampleCountChanged?.invoke(samples.size)
        invalidate()
        return true
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        val isStylus = event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
        if (!isStylus) {
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER -> {
                hoverPath.moveTo(event.x, event.y)
                appendSample(event, 0, CaptureEventType.HOVER_ENTER)
            }

            MotionEvent.ACTION_HOVER_MOVE -> {
                hoverPath.lineTo(event.x, event.y)
                appendSample(event, 0, CaptureEventType.HOVER_MOVE)
                appendHistoricalSamples(event, 0, hover = true)
            }

            MotionEvent.ACTION_HOVER_EXIT -> {
                hoverPath.lineTo(event.x, event.y)
                appendSample(event, 0, CaptureEventType.HOVER_EXIT)
            }
        }

        onSampleCountChanged?.invoke(samples.size)
        invalidate()
        return true
    }

    private fun appendHistoricalSamples(event: MotionEvent, pointerIndex: Int, hover: Boolean = false) {
        val pointerId = event.getPointerId(pointerIndex)
        for (h in 0 until event.historySize) {
            val x = event.getHistoricalX(pointerIndex, h)
            val y = event.getHistoricalY(pointerIndex, h)
            if (!hover) {
                drawPath.lineTo(x, y)
            } else {
                hoverPath.lineTo(x, y)
            }
            samples += SignatureSample(
                eventTimeMs = event.getHistoricalEventTime(h),
                xNorm = normalizeX(x),
                yNorm = normalizeY(y),
                pressure = event.getHistoricalPressure(pointerIndex, h),
                eventType = CaptureEventType.MOVE_HIST,
                toolType = toolTypeFrom(event.getToolType(pointerIndex)),
                tilt = event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, pointerIndex, h),
                orientation = event.getHistoricalAxisValue(MotionEvent.AXIS_ORIENTATION, pointerIndex, h),
                distance = event.getHistoricalAxisValue(MotionEvent.AXIS_DISTANCE, pointerIndex, h),
                pointerId = pointerId,
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            )
        }
    }

    private fun appendSample(event: MotionEvent, pointerIndex: Int, eventType: CaptureEventType) {
        samples += SignatureSample(
            eventTimeMs = event.eventTime,
            xNorm = normalizeX(event.getX(pointerIndex)),
            yNorm = normalizeY(event.getY(pointerIndex)),
            pressure = event.getPressure(pointerIndex),
            eventType = eventType,
            toolType = toolTypeFrom(event.getToolType(pointerIndex)),
            tilt = event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex),
            orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION, pointerIndex),
            distance = event.getAxisValue(MotionEvent.AXIS_DISTANCE, pointerIndex),
            pointerId = event.getPointerId(pointerIndex),
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        )
    }

    private fun normalizeX(rawX: Float): Float {
        val w = max(width, 1)
        return (rawX / w.toFloat()) * LOGICAL_WIDTH.toFloat()
    }

    private fun normalizeY(rawY: Float): Float {
        val h = max(height, 1)
        return (rawY / h.toFloat()) * LOGICAL_HEIGHT.toFloat()
    }

    private fun toolTypeFrom(toolType: Int): CaptureToolType = when (toolType) {
        MotionEvent.TOOL_TYPE_STYLUS -> CaptureToolType.STYLUS
        MotionEvent.TOOL_TYPE_FINGER -> CaptureToolType.FINGER
        MotionEvent.TOOL_TYPE_ERASER -> CaptureToolType.ERASER
        else -> CaptureToolType.UNKNOWN
    }
}
