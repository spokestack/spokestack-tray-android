package io.spokestack.tray.message

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.spokestack.tray.R

class MessageAdapter :
    ListAdapter<Message, RecyclerView.ViewHolder>(Message.DIFF_UTIL_CALLBACK) {

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
        layout.findViewById<TextView>(R.id.messageContent).text = message.content
    }


    class BubbleViewHolder(val msgLayout: LinearLayout) : RecyclerView.ViewHolder(msgLayout)

}
