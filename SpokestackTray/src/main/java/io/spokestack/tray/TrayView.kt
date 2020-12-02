package io.spokestack.tray

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

/**
 * A custom layout for Spokestack Tray that handles drag events anywhere in the status bar to
 * resize the message stream, à la
 * [fsck's SplitView](https://github.com/k9mail/splitview/blob/master/lib/src/com/fsck/splitview/SplitView.java).
 */
class TrayView(trayContext: Context, attributeSet: AttributeSet) :
    ConstraintLayout(trayContext, attributeSet), View.OnTouchListener {
    private val heightMin = resources.getDimensionPixelSize(R.dimen.spsk_messageStreamMinHeight)

    lateinit var statusBar: LinearLayout
    lateinit var messageStream: RecyclerView

    private var lastY = 0f
    private var parentHeight: Int = 0

    // the status bar does not need to handle clicks since the event it does handle (drag to
    // resize the message stream) is purely visual in nature rather than functional
    @SuppressLint("ClickableViewAccessibility")
    override fun onFinishInflate() {
        super.onFinishInflate()
        statusBar = findViewById(R.id.statusBar)
        messageStream = findViewById(R.id.messageStream)
        statusBar.setOnTouchListener(this)
        val tv = TypedValue()
        context.theme.resolveAttribute(R.attr.actionBarSize, tv, true)
        val screenHeight = context.resources.displayMetrics.heightPixels
        parentHeight = screenHeight - TypedValue.complexToDimensionPixelSize(
            tv.data, resources.displayMetrics
        )
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (v != statusBar) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
                setHeight(event.rawY - lastY)
                lastY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                setHeight(event.rawY - lastY)
                lastY = event.rawY
            }
        }
        return true
    }

    private fun setHeight(amount: Float) {
        var newHeight = (messageStream.measuredHeight - amount).toInt()
        // the message stream shouldn't push the status bar offscreen
        newHeight = min(newHeight, parentHeight - statusBar.measuredHeight)
        val params: ViewGroup.LayoutParams = messageStream.layoutParams as ViewGroup.LayoutParams
        if (newHeight >= heightMin) {
            params.height = newHeight
            messageStream.layoutParams = params
        }
    }
}