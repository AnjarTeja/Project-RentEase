package com.example.rentease

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.graphics.Color
import android.view.animation.DecelerateInterpolator
import kotlin.random.Random

class StarFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Star(
        var x: Float,
        var y: Float,
        var radius: Float,
        var alpha: Float,
        var baseAlpha: Float,
        var twinkleSpeed: Float,
        var phase: Float
    )

    private val stars = mutableListOf<Star>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        generateStars(w, h)
        startTwinkling()
    }

    private fun generateStars(w: Int, h: Int) {
        stars.clear()
        val count = (w * h) / 4000
        for (i in 0 until count.coerceIn(20, 80)) {
            stars.add(
                Star(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    radius = Random.nextFloat() * 2.0f + 0.5f,
                    alpha = 0f,
                    baseAlpha = Random.nextFloat() * 0.5f + 0.2f,
                    twinkleSpeed = Random.nextFloat() * 0.6f + 0.3f,
                    phase = Random.nextFloat() * 6.28f
                )
            )
        }
    }

    private fun startTwinkling() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            duration = 3000
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val elapsed = it.animatedFraction * 3.0f
                for (star in stars) {
                    val twinkle = (kotlin.math.sin(elapsed * star.twinkleSpeed + star.phase) + 1.0f) / 2.0f
                    star.alpha = star.baseAlpha * (0.5f + twinkle * 0.5f)
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (star in stars) {
            paint.alpha = (star.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(star.x, star.y, star.radius, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
