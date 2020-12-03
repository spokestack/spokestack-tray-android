package io.spokestack.tray

import android.content.Context
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs
import kotlin.math.ceil

/**
 * View subclass that acts as a button that controls the tray's UI state. This button can be tapped
 * or swiped to open the tray.
 *
 * It's implemented as a custom view because at the time of writing, registering `OnSwipe` on the
 * tray's motion scene itself interfered with the button receiving click events, and registering
 * both `OnClick` and `OnSwipe` resulted in only one event being recognized.
 */
class MicButton(context: Context, attributes: AttributeSet) : View(context, attributes) {

    private var rightOrientation = false
    private var touchStarted = false
    private var closing = false
    private var startX = 0f
    private var lastX = 0f
    private var detector = GestureDetectorCompat(context, GestureHandler())
    private var screenWidth = resources.displayMetrics.widthPixels

    var trayView: TrayView? = null
    var setTransitionProgress: ((percent: Float) -> Unit)? = null


    /**
     * Set the button's orientation, which potentially changes its background
     * image and how it reacts to swipes.
     *
     * The default settings assume the button is oriented on the lefthand
     * side of the screen, opening the tray to the right.
     *
     * @param orientation The button's orientation.
     */
    fun setOrientation(orientation: TrayConfig.Orientation) {
        if (orientation == TrayConfig.Orientation.RIGHT) {
            val micBg = ContextCompat.getDrawable(context, R.drawable.mic_tab_btn_right)
            this.background = micBg
            this.rightOrientation = true
        }
        adjustInsets()
    }

    private fun adjustInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val tab = background as LayerDrawable
            val mic = tab.findDrawableByLayerId(R.id.mic_icon) as InsetDrawable
            val tabWidth = tab.intrinsicWidth
            val verticalInset = (tabWidth / 15) * 8
            val leftInset = ceil(tabWidth * .3).toInt()
            val rightInset = ceil(tabWidth * .4).toInt()
            val newMic = InsetDrawable(mic, leftInset, verticalInset, rightInset, verticalInset)
            tab.setDrawable(1, newMic)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        trayView?.onTouch(trayView?.statusBar!!, event)
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStarted = true
                startX = event.rawX
                lastX = startX
                closing = false
                true
            }
            MotionEvent.ACTION_UP -> {
                if (touchStarted) {
                    touchStarted = false
                    val touchMoved = abs(event.rawX - startX)
                    startX = 0f

                    val swipePercent = touchMoved / screenWidth

                    // we've tapped, or swiped either far enough or fast enough to open
                    val tap = touchMoved < 20
                    val shouldOpen = swipePercent > 0.2 || detector.onTouchEvent(event)
                    if (tap || (!closing && (shouldOpen))) {
                        performClick()
                    } else {
                        setTransitionProgress?.invoke(0.0f)
                    }
                }
                true
            }
            MotionEvent.ACTION_MOVE -> {
                closing = when (rightOrientation) {
                    true -> event.rawX > lastX
                    false -> event.rawX < lastX
                }
                lastX = event.rawX
                val adjustedX = abs(lastX - startX)
                val swipePercent = adjustedX / screenWidth
                if (swipePercent > 0) {
                    setTransitionProgress?.invoke(swipePercent)
                }
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    override fun performClick(): Boolean {
        setTransitionProgress?.invoke(1.0f)
        super.performClick()
        return true
    }

    inner class GestureHandler : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            return true
        }
    }
}