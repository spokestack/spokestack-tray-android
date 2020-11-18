package io.spokestack.tray.message

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlin.math.abs


/**
 * An item animator that mimics the logic of `DefaultItemAnimator` but only concerns itself with
 * adding new message bubbles since the message history is append-only.
 */
class MessageBubbleAnimator : SimpleItemAnimator() {
    var interpolator: TimeInterpolator? = null
    val pendingAdds: ArrayList<ViewHolder> = ArrayList()
    val additions: ArrayList<ArrayList<ViewHolder>> = ArrayList()
    val addAnimations: ArrayList<ViewHolder> = ArrayList()


    override fun animateChange(
        oldHolder: ViewHolder?,
        newHolder: ViewHolder?,
        fromLeft: Int,
        fromTop: Int,
        toLeft: Int,
        toTop: Int
    ): Boolean {
        if (oldHolder?.itemView != null) {
            dispatchChangeFinished(oldHolder, true)
        }
        if (newHolder?.itemView != null) {
            dispatchChangeFinished(newHolder, false)
        }
        return true
    }

    override fun runPendingAnimations() {
        if (pendingAdds.isEmpty()) {
            return
        }
        val additionList: ArrayList<ViewHolder> = ArrayList()
        additionList.addAll(pendingAdds)
        additions.add(additionList)
        pendingAdds.clear()
        val adder = Runnable {
            for (holder in additionList) {
                animateAddImpl(holder)
            }
            additionList.clear()
            additions.remove(additionList)
        }
        adder.run()
    }

    override fun animateAdd(holder: ViewHolder): Boolean {
        resetAnimation(holder)
        holder.itemView.translationY = holder.itemView.height.toFloat()
        holder.itemView.alpha = 0f
        pendingAdds.add(holder)
        return true
    }

    private fun animateAddImpl(holder: ViewHolder) {
        holder.itemView.animate().apply {
            translationY(0f)
            alpha(1f)
            val durationMs = 500L
            duration = durationMs
//            duration = addDuration
            interpolator = this.interpolator
            setListener(AddAnimationListener(holder))
            startDelay = abs(holder.adapterPosition * durationMs / 4)
        }.start()
    }

    override fun endAnimation(item: ViewHolder) {
        val view: View = item.itemView
        // this will trigger end callback which should set properties to their target values.
        view.animate().cancel()
        if (pendingAdds.remove(item)) {
            view.alpha = 1.0f
            dispatchAddFinished(item)
        }
        for (i in additions.size - 1 downTo 0) {
            val additionList: ArrayList<ViewHolder> = additions[i]
            if (additionList.remove(item)) {
                view.alpha = 1.0f
                dispatchAddFinished(item)
                if (additionList.isEmpty()) {
                    additions.removeAt(i)
                }
            }
        }
        dispatchFinishedWhenDone()
    }

    private fun dispatchFinishedWhenDone() {
        if (!isRunning) {
            dispatchAnimationsFinished()
        }
    }

    override fun endAnimations() {
        var count = pendingAdds.size
        for (i in count - 1 downTo 0) {
            val item: ViewHolder = pendingAdds.get(i)
            item.itemView.alpha = 1f
            dispatchAddFinished(item)
            pendingAdds.removeAt(i)
        }
        val listCount = additions.size
        for (i in listCount - 1 downTo 0) {
            val additionList: ArrayList<ViewHolder> = additions.get(i)
            count = additionList.size
            for (j in count - 1 downTo 0) {
                val item = additionList[j]
                val view = item.itemView
                view.alpha = 1f
                dispatchAddFinished(item)
                additionList.removeAt(j)
                if (additionList.isEmpty()) {
                    additions.remove(additionList)
                }
            }
        }
        cancelAll(addAnimations)
        dispatchAnimationsFinished()
    }

    private fun cancelAll(viewHolders: List<ViewHolder>) {
        for (holder in viewHolders.reversed()) {
            holder.itemView.animate().cancel()
        }
    }

    override fun isRunning(): Boolean {
        return pendingAdds.isEmpty() && additions.isEmpty()
    }

    private fun resetAnimation(holder: ViewHolder) {
        if (interpolator == null) {
            interpolator = ValueAnimator().interpolator
        }
        holder.itemView.animate().interpolator = interpolator
        endAnimation(holder)
    }

    override fun animateRemove(holder: ViewHolder?): Boolean {
        dispatchRemoveFinished(holder)
        return true
    }

    override fun animateMove(
        holder: ViewHolder?,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        dispatchMoveFinished(holder)
        return true
    }

    private fun clear(v: View) {
        v.apply {
            alpha = 1f
            scaleY = 1f
            scaleX = 1f
            translationY = 0f
            translationX = 0f
            rotation = 0f
            rotationY = 0f
            rotationX = 0f
            pivotY = v.measuredHeight / 2f
            pivotX = v.measuredWidth / 2f
            animate().setInterpolator(null).startDelay = 0
        }
    }

    private inner class AddAnimationListener(val holder: ViewHolder): AnimatorListenerAdapter() {

        override fun onAnimationStart(animator: Animator?) {
            dispatchAddStarting(holder)
        }

        override fun onAnimationCancel(animator: Animator?) {
            clear(holder.itemView)
        }

        override fun onAnimationEnd(animator: Animator?) {
            clear(holder.itemView)
            dispatchAddFinished(holder)
            addAnimations.remove(holder)
            dispatchFinishedWhenDone()
        }
    }
}