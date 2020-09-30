package io.spokestack.tray

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import io.spokestack.tray.message.Message

/**
 * A simple data class that describes the state necessary to provide a seamless transition
 * across app lifecycle events.
 */
internal data class TrayState(
    var isActive: Boolean = false,
    var playTts: Boolean = true,
    var firstOpen: Boolean = true,
    var messageStreamHeight: Int = 0,
    val messages: ArrayList<Message> = ArrayList()
) : Parcelable {

    constructor(context: Context) : this() {
        this.messageStreamHeight =
            context.resources.getDimensionPixelSize(R.dimen.message_stream_height)
    }

    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
        parcel.readArrayList(ClassLoader.getSystemClassLoader()) as ArrayList<Message>
    )

    override fun describeContents(): Int {
        // unnecessary
        Parcelable.CONTENTS_FILE_DESCRIPTOR
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte(if (isActive) 1 else 0)
        dest.writeByte(if (playTts) 1 else 0)
        dest.writeByte(if (firstOpen) 1 else 0)
        dest.writeInt(messageStreamHeight)
        dest.writeList(messages as List<Message>)
    }

    companion object CREATOR : Parcelable.Creator<TrayState> {
        override fun createFromParcel(parcel: Parcel): TrayState {
            return TrayState(parcel)
        }

        override fun newArray(size: Int): Array<TrayState?> {
            return arrayOfNulls(size)
        }

        val SERIALIZATION_KEY = namespaced_key("state")
    }

}
