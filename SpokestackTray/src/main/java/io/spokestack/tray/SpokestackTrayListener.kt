package io.spokestack.tray

import io.spokestack.spokestack.nlu.NLUResult
import io.spokestack.spokestack.tts.TTSEvent
import io.spokestack.spokestack.util.EventTracer

/**
 * An interface for classes that wish to receive events from the Spokestack tray.
 *
 * For trace messages, the tray dispatches those that meet or exceed the logging level specified
 * in tray configuration. Errors are dispatched via `onError`.
 *
 * @see TrayConfig
 * @see EventTracer.Level
 */
interface SpokestackTrayListener {

    /**
     * A log message was received from Spokestack.
     *
     * @param message The log message.
     */
    fun onLog(message: String)

    /**
     * Spokestack's NLU classified an utterance.
     *
     * @param result The classification result.
     * @return A string representing the application's response to the user utterance.
     */
    fun onClassification(result: NLUResult): VoicePrompt?

    /**
     * Spokestack encountered an error.
     *
     * @param error The error.
     */
    fun onError(error: Throwable)

    /**
     * The tray UI opened. Called from the UI thread.
     */
    fun onOpen()

    /**
     * The tray UI closed. Called from the UI thread.
     */
    fun onClose()
}