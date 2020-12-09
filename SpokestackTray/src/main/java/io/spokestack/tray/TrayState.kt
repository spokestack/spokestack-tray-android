package io.spokestack.tray

import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.spokestack.tray.message.Message

/**
 * A simple data class that describes the state necessary to provide a seamless transition
 * across app lifecycle events.
 */
data class TrayState(
    var isOpen: Boolean = false,
    var isActive: Boolean = false,
    var playTts: Boolean = true,
    var firstOpen: Boolean = true,
    var expectFollowup: Boolean = false,
    var messageStreamHeight: Int = 0,
    val messages: ArrayList<Message> = ArrayList()
) : Parcelable {

    private val messageData: MutableLiveData<ArrayList<Message>> = MutableLiveData(messages)

    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
        parcel.readArrayList(ClassLoader.getSystemClassLoader()) as ArrayList<Message>
    )

    fun liveData(): LiveData<ArrayList<Message>> {
        return messageData
    }

    /**
     * Clear the conversation state without resetting any user modifications
     * like muting TTS or changing the tray size.
     */
    fun clear() {
        apply {
            expectFollowup = false
            firstOpen = true
            messages.clear()
            messageData.notifyObserver()
        }
    }

    fun addMessage(message: Message) {
        // observers only need to know when to add a message to the chat stream;
        // other data is only for saving/restoring UI state
        messages.add((message))
        messageData.notifyObserver()
    }

    fun addOrUpdateUserMessage(text: String) {
        val message = messages.lastOrNull()
        if (message == null || message.isSystem) {
            addMessage(Message(content = text))
        } else {
            messages[messages.size - 1] = Message(false, text)
        }
        messageData.notifyObserver()
    }

    override fun describeContents(): Int {
        // unnecessary
        Parcelable.CONTENTS_FILE_DESCRIPTOR
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte(if (isOpen) 1 else 0)
        dest.writeByte(if (isActive) 1 else 0)
        dest.writeByte(if (playTts) 1 else 0)
        dest.writeByte(if (firstOpen) 1 else 0)
        dest.writeByte(if (expectFollowup) 1 else 0)
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

// the LiveData's value must be set (hence incrementing its version) for observers to be updated
// see https://stackoverflow.com/a/52075248/421784
fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}
