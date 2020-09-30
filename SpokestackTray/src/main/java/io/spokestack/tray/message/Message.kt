package io.spokestack.tray.message

import android.os.Parcel
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil

/**
 * A simple data class that describes a message in Spokestack. Messages have text contents and can
 * be initiated by the user or the system.
 */
data class Message(val isSystem: Boolean = false, val content: String) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readString()!!
    )

    companion object CREATOR : Parcelable.Creator<Message> {
        override fun createFromParcel(parcel: Parcel): Message {
            return Message(parcel)
        }

        override fun newArray(size: Int): Array<Message?> {
            return arrayOfNulls(size)
        }

        val DIFF_UTIL_CALLBACK = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem.content == newItem.content
            }

        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (isSystem != other.isSystem) return false
        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isSystem.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }

    override fun describeContents(): Int {
        // unnecessary
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte(if (isSystem) 1 else 0)
        dest.writeString(content)
    }
}
