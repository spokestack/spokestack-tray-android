package io.spokestack.tray

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path

/**
 * A scrolling gradient shaped like a (very) rounded rectangle. Used as an ASR activity indicator.
 */
class ListenBubble(context: Context, pixelsPerSecond: Float) :
    ScrollingGradient(context, pixelsPerSecond) {
    private val path = Path()

    init {
        val resources = context.resources
        val width = resources.getDimensionPixelSize(R.dimen.spsk_listenButtonWidth)
        val twenty = (width shr 2).toFloat()
        val ten = (width shr 3).toFloat()
        path.moveTo(twenty, 0f)
        path.rCubicTo(-ten, 0f, -twenty, ten, -twenty, twenty)
        path.rCubicTo(0f, ten, ten, twenty, twenty, twenty)
        path.lineTo(twenty * 3, twenty * 2)
        path.rCubicTo(ten, 0f, twenty, -ten, twenty, -twenty)
        path.rCubicTo(0f, -ten, -ten, -twenty, -twenty, -twenty)
        path.close()
    }

    override fun draw(canvas: Canvas) {
        canvas.clipPath(path)
        super.draw(canvas)
    }
}
