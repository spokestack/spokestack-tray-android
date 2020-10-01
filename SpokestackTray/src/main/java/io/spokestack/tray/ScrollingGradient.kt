package io.spokestack.tray

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

/**
 * A scrolling gradient animation for the listening indicator.
 * Adapted from https://stackoverflow.com/a/48696216/421784
 */
class ScrollingGradient(val context: Context, private val pixelsPerSecond: Float) : Drawable(),
    Animatable,
    TimeAnimator.TimeListener {
    private val paint = Paint()
    private var x: Float = 0.toFloat()
    private val animator = TimeAnimator()
    private val startColor = ContextCompat.getColor(context, R.color.spsk_colorGradientOne)
    private val endColor = ContextCompat.getColor(context, R.color.spsk_colorGradientTwo)

    init {
        animator.setTimeListener(this)
    }

    override fun onBoundsChange(bounds: Rect) {
        paint.shader = LinearGradient(
            0f,
            0f,
            bounds.width().toFloat(),
            0f,
            startColor,
            endColor,
            Shader.TileMode.MIRROR
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.clipRect(bounds)
        canvas.translate(x, 0f)
        canvas.drawPaint(paint)
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int = PixelFormat.OPAQUE

    override fun start() {
        animator.start()
    }

    override fun stop() {
        animator.cancel()
    }

    override fun isRunning(): Boolean = animator.isRunning

    override fun onTimeUpdate(animation: TimeAnimator, totalTime: Long, deltaTime: Long) {
        x = pixelsPerSecond * totalTime / 1000
        invalidateSelf()
    }
}
