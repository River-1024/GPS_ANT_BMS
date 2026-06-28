package com.zjsf.gps_ant_bms.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.max

class PowerBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(20, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        style = Paint.Style.STROKE
        strokeWidth = dp(0f)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(119, 240, 111)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val rect = RectF()
    private var displayRatio = 0f
    private var targetRatio = 0f
    private var animator: ValueAnimator? = null
    private var power = 0.0
    private var maxPower = 1000.0

    fun setPower(
        power: Double,
        maxPower: Double,
        normalColor: Int = Color.rgb(119, 240, 111),
        yellowRatio: Double = 0.55,
        redRatio: Double = 0.9
    ) {
        this.power = power
        this.maxPower = max(100.0, maxPower)
        targetRatio = (abs(power) / this.maxPower).coerceIn(0.0, 1.0).toFloat()
        fillPaint.color = when {
            power < 0.0 -> Color.rgb(98, 168, 255)
            targetRatio >= redRatio.toFloat() -> Color.rgb(255, 112, 111)
            targetRatio >= yellowRatio.toFloat() -> Color.rgb(244, 200, 90)
            else -> normalColor
        }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(displayRatio, targetRatio).apply {
            duration = 260L
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                displayRatio = it.animatedValue as Float
                invalidate()
            }
            start()
        }
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
        canvas.drawRoundRect(rect, radius, radius, trackPaint)

        val contentWidth = rect.width()
        val centerX = if (power < 0.0) rect.centerX() else rect.left
        val fillWidth = if (power < 0.0) contentWidth * displayRatio / 2f else contentWidth * displayRatio

        if (power < 0.0) {
            canvas.drawRoundRect(
                RectF(centerX - fillWidth, rect.top, centerX, rect.bottom),
                radius,
                radius,
                fillPaint
            )
        } else {
            canvas.drawRoundRect(
                RectF(rect.left, rect.top, rect.left + fillWidth, rect.bottom),
                radius,
                radius,
                fillPaint
            )
        }

        if (strokePaint.strokeWidth > 0f) {
            canvas.drawRoundRect(rect, radius, radius, strokePaint)
        }

        val percent = (targetRatio * 100f).toInt()
        textPaint.textSize = (height * 0.38f).coerceAtLeast(dp(18f))
        val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("$percent%", rect.centerX(), baseline, textPaint)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
