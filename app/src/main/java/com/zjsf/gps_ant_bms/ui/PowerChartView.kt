package com.zjsf.gps_ant_bms.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class PowerChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(16, 23, 21)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        style = Paint.Style.STROKE
        strokeWidth = dp(0f)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(18, 243, 250, 247)
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(46, 243, 250, 247)
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(119, 240, 111)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dp(3f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(95, 113, 108)
        textSize = dp(12f)
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(119, 240, 111)
        style = Paint.Style.FILL
    }

    private val rect = RectF()
    private val path = Path()
    private var points: List<Double> = emptyList()
    private var maxPower = 1000.0

    fun setData(points: List<Double>, maxPower: Double, lineColor: Int = Color.rgb(119, 240, 111)) {
        this.points = points
        this.maxPower = max(100.0, maxPower)
        linePaint.color = lineColor
        pointPaint.color = lineColor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = dp(7f)
        rect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat()
        )
        canvas.drawRoundRect(rect, radius, radius, bgPaint)

        for (i in 1 until 10) {
            val x = rect.left + rect.width() * i / 10f
            canvas.drawLine(x, rect.top, x, rect.bottom, gridPaint)
        }
        for (i in 1 until 3) {
            val y = rect.top + rect.height() * i / 3f
            canvas.drawLine(rect.left, y, rect.right, y, gridPaint)
        }

        val zeroY = rect.centerY()
        canvas.drawLine(rect.left, zeroY, rect.right, zeroY, zeroPaint)
        if (borderPaint.strokeWidth > 0f) {
            canvas.drawRoundRect(rect, radius, radius, borderPaint)
        }

        textPaint.textSize = dp(12f)
        canvas.drawText("+${maxPower.toInt()}W", rect.left + dp(8f), rect.top + dp(17f), textPaint)
        canvas.drawText("0W", rect.left + dp(8f), zeroY - dp(6f), textPaint)
        canvas.drawText("-${maxPower.toInt()}W", rect.left + dp(8f), rect.bottom - dp(8f), textPaint)

        if (points.isEmpty()) return

        val visiblePoints = points.takeLast(120)
        val stepX = if (visiblePoints.size <= 1) rect.width() else rect.width() / (visiblePoints.size - 1)
        path.reset()
        visiblePoints.forEachIndexed { index, value ->
            val x = rect.left + stepX * index
            val normalized = (value / maxPower).coerceIn(-1.0, 1.0).toFloat()
            val y = zeroY - normalized * rect.height() / 2f
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, linePaint)

        val latest = visiblePoints.last()
        val latestY = zeroY - (latest / maxPower).coerceIn(-1.0, 1.0).toFloat() * rect.height() / 2f
        canvas.drawCircle(rect.right, latestY, dp(4f), pointPaint)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
