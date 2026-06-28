package com.zjsf.gps_ant_bms.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SocFillView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(21, 29, 27)
        style = Paint.Style.FILL
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(119, 240, 111)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(31, 232, 246, 241)
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val rect = RectF()
    private val fillRect = RectF()
    private var percent = 0

    fun setSoc(percent: Int, color: Int) {
        this.percent = percent.coerceIn(0, 100)
        fillPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = dp(8f)
        rect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat()
        )
        canvas.drawRoundRect(rect, radius, radius, backgroundPaint)

        val fillHeight = rect.height() * percent / 100f
        fillRect.set(rect.left, rect.bottom - fillHeight, rect.right, rect.bottom)
        canvas.save()
        canvas.clipRect(fillRect)
        canvas.drawRoundRect(rect, radius, radius, fillPaint)
        canvas.restore()

        canvas.drawRoundRect(rect, radius, radius, borderPaint)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
