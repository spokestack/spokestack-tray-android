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
        val forty = (width shr 1).toFloat()
        val twenty = (width shr 2).toFloat()
        val twentyFive = twenty + (width shr 4).toFloat()
        path.moveTo(twenty, 0f)
        path.rCubicTo(-twentyFive, 0f, -twentyFive, forty, 0f, forty)
        path.lineTo(twenty * 3, forty)
        path.rCubicTo(twentyFive, 0f, twentyFive, -forty, 0f, -forty)
        path.close()
    }

    override fun draw(canvas: Canvas) {
        canvas.clipPath(path)
        super.draw(canvas)
    }
}
