package io.spokestack.tray

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.spokestack.tray.message.Message

/**
 * A view model comprising the tray's UI/behavioral state and the history of messages
 * between the application and user.
 */
class TrayViewModel : ViewModel() {
    internal var state: TrayState = TrayState(messages = arrayListOf())
    private val liveState: MutableLiveData<ArrayList<Message>> =
        MutableLiveData(this.state.messages)

    //    fun getState(): LiveData<TrayState> {
    fun getMessages(): LiveData<ArrayList<Message>> {
        return this.liveState
    }

    fun addMessage(message: Message) {
        // observers only need to know when to add a message to the chat stream;
        // other data is only for saving/restoring UI state
        this.liveState.value?.add((message))
        this.liveState.notifyObserver()
    }

    fun updateLastUserMessage(text: String) {
        val messages = this.liveState.value
        messages?.set(messages.size - 1, Message(false, text))
        this.liveState.notifyObserver()
    }
}

// the LiveData's value must be set (hence incrementing its version) for observers to be updated
// see https://stackoverflow.com/a/52075248/421784
fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}