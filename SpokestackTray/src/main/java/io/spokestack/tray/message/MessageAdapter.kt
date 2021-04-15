package io.spokestack.tray.message

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.spokestack.tray.R

class MessageAdapter(context: Context) :
    ListAdapter<Message, RecyclerView.ViewHolder>(Message.DIFF_UTIL_CALLBACK) {

    private var startPosition = -1
    private val bubbleAnimation = AnimationUtils.loadAnimation(context, R.anim.item_enter)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val msgView: LinearLayout

        val isSystem = viewType == 0
        msgView = if (isSystem) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.system_msg_view, parent, false) as LinearLayout
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.user_msg_view, parent, false) as LinearLayout
        }

        return BubbleViewHolder(msgView)
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (currentList[position].isSystem) 0 else 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = currentList[position]

        val layout = (holder as BubbleViewHolder).msgLayout
        if (position > startPosition) {
            layout.startAnimation(bubbleAnimation)
            startPosition = position
        }

        val textView = layout.findViewById<TextView>(R.id.messageContent)
        if (message.imageURL.isNotEmpty()) {
            val imageUri = Uri.parse(message.imageURL)
            Glide.with(holder.itemView).load(imageUri).into(MessageBubbleTarget(textView))
        } else {
            textView.setCompoundDrawables(null, null, null, null)
        }
        textView.text = message.content
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        (holder as BubbleViewHolder).msgLayout.clearAnimation()
    }

    class BubbleViewHolder(val msgLayout: LinearLayout) : RecyclerView.ViewHolder(msgLayout)

    class MessageBubbleTarget(private val textView: TextView) :
        CustomTarget<Drawable>(SIZE_ORIGINAL, SIZE_ORIGINAL) {
        override fun onResourceReady(
            resource: Drawable,
            transition: Transition<in Drawable>?
        ) {
            textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, resource)
        }

        override fun onLoadCleared(placeholder: Drawable?) {
            textView.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                null,
                placeholder
            )
        }
    }
}
