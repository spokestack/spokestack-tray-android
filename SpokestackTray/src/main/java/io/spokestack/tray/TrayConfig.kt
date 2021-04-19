@file:JvmName("TrayConfig")

package io.spokestack.tray

import io.spokestack.spokestack.Spokestack
import io.spokestack.spokestack.TranscriptEditor
import io.spokestack.spokestack.tts.SynthesisRequest.Mode
import io.spokestack.spokestack.util.EventTracer

/**
 * Data class holding all configuration for the Spokestack Tray.
 */
class TrayConfig private constructor(builder: Builder) {

    /**
     * Determines the initial placement of the microphone button and the
     * opening direction of the tray.
     */
    val orientation: Orientation = builder.orientation

    /**
     * A flag determining whether haptic feedback is sent when the tray opens.
     */
    val haptic: Boolean = builder.haptic

    /**
     * A message to display and/or read via TTS synthesis when the tray opens for
     * the first time.
     */
    val greeting: String = builder.greeting

    /**
     * A flag determining whether the initial greeting message, if set, should be
     * read allowed (it will always be displayed in the message stream).
     */
    val sayGreeting: Boolean = builder.sayGreeting

    /**
     * Client ID from your Spokestack account.
     */
    val clientId: String = builder.clientId.toString()

    /**
     * Secret key from your Spokestack account.
     */
    val clientSecret: String = builder.clientSecret.toString()

    /**
     * Synthesis mode for TTS. This defaults to `TEXT` and should only be changed if
     * the messages you return to the Tray in [Prompt]s are in SSML or Speech
     * Markdown format.
     */
    val ttsMode: Mode = builder.ttsMode

    /**
     * Voice to use for TTS synthesis. Defaults to a free voice offered by Spokestack.
     */
    val voice: String = builder.voice

    /**
     * A preconfigured Spokestack builder instance for use with the tray.
     * Use this to customize ASR providers, etc.
     */
    val spokestackBuilder: Spokestack.Builder = builder.spokestackBuilder

    /**
     * Configuration properties forwarded to the Spokestack builder.
     * These can also be set via [spokestackBuilder], but this field provides a more
     * lightweight form of configuration than setting up a full builder.
     */
    val properties: Map<String, Any> = builder.properties

    /**
     * Level of Spokestack log messages that will be produced and passed to registered listeners.
     * @see io.spokestack.spokestack.util.EventTracer.Level
     */
    val logLevel: Int = builder.logLevel

    /**
     * The URL to the directory containing your wakeword models. This URL should not end in a
     * filename; the library will automatically append filenames and use this base URL to download
     * multiple models.
     */
    val wakewordModelURL: String? = builder.wakewordModelURL

    /**
     * The URL to the directory containing your NLU files. This URL should not end in a
     * filename; the library will automatically append filenames and use this base URL to download
     * multiple files.
     */
    val nluURL: String? = builder.nluURL

    /**
     * The URL to the Rasa Open Source server. If this is set, Spokestack will use Rasa Core
     * for both NLU and dialogue management.
     *
     * For production use, this endpoint should be secured. See the documentation
     * for [io.spokestack.spokestack.rasa.RasaOpenSourceNLU] for descriptions of authentication
     * methods you can specify using [Builder.withProperty].
     */
    val rasaOssURL: String? = builder.rasaOssURL

    /**
     * Whether wakeword and NLU models should be forcibly (re-)downloaded on launch.
     */
    val refreshModels: Boolean = builder.refreshModels

    /**
     * Used to edit the transcript before classification and displaying to the user. This can be
     * used to correct ASR errors noticed during app usage.
     */
    val transcriptEditor: TranscriptEditor? = builder.transcriptEditor

    /**
     * A listener that will receive events from Spokestack Tray. Registering a listener is the
     * only way to receive notifications of, and hence respond automatically to, recognized
     * speech/intents.
     */
    val listener: SpokestackTrayListener? = builder.listener

    /**
     * A simple enum representing the tray's affinity for a certain side of the screen.
     */
    enum class Orientation {
        LEFT,
        RIGHT
    }

    /**
     * A fluent builder interface for creating a tray configuration.
     */
    data class Builder(
        internal var orientation: Orientation = Orientation.LEFT,
        internal var haptic: Boolean = true,
        internal var sayGreeting: Boolean = true,
        internal var greeting: String = "",
        internal var ttsMode: Mode = Mode.TEXT,
        internal var voice: String = "demo-male",
        internal var clientId: String? = null,
        internal var clientSecret: String? = null,
        internal val properties: HashMap<String, Any> = HashMap(),
        internal var logLevel: Int = EventTracer.Level.NONE.value(),
        internal var wakewordModelURL: String? = null,
        internal var nluURL: String? = null,
        internal var rasaOssURL: String? = null,
        internal var refreshModels: Boolean = false,
        internal var editTranscript: ((String) -> String)? = null,
        internal var listener: SpokestackTrayListener? = null,
        internal var transcriptEditor: TranscriptEditor? = null,
    ) {
        var spokestackBuilder: Spokestack.Builder = Spokestack.Builder()

        /**
         * Set the tray's orientation, which determines the mic button's
         * initial position and the opening direction of the tray.
         *
         * @param value The tray's orientation.
         */
        fun orientation(value: Orientation) = apply { this.orientation = value }

        /**
         * Set whether haptic feedback should be sent when the tray opens.
         *
         * @param value Whether haptic feedback should occur.
         */
        fun haptic(value: Boolean) = apply { this.haptic = value }

        /**
         * Set whether the greeting message, if one exists, should be synthesized and
         * read to the user as well as displayed.
         *
         * @param value Whether the greeting message should be synthesized via TTS.
         */
        fun sayGreeting(value: Boolean) = apply { this.sayGreeting = value }

        /**
         * Set the message displayed and/or read to the user when the tray opens for
         * the first time.
         *
         * @param value The greeting message.
         */
        fun greeting(value: String) = apply { this.greeting = value }

        /**
         * Set the Spokestack credentials to be used by the tray.
         *
         * @param clientId The application's client ID.
         * @param clientSecret The application's client secret.
         */
        fun credentials(clientId: String, clientSecret: String) = apply {
            this.clientId = clientId
            this.clientSecret = clientSecret
        }

        /**
         * Set the synthesis mode to use for TTS requests.
         *
         * @param value The synthesis mode to use for TTS requests.
         * Can be [Mode.TEXT] (the default), [Mode.SSML], or [Mode.MARKDOWN].
         */
        fun ttsMode(value: Mode) = apply { this.ttsMode = value }

        /**
         * Set the voice to use for TTS synthesis. Defaults to a free Spokestack voice.
         *
         * @param value The voice to use for TTS.
         */
        fun ttsVoice(value: String) = apply { this.voice = value }

        /**
         * Add a property to the underlying Spokestack configuration.
         *
         * @param key The property's key.
         * @param value The property's value.
         */
        fun withProperty(key: String, value: Any) = apply { this.properties[key] = value }

        /**
         * Use a custom Spokestack builder for this configuration. Any properties set via
         * [withProperty] will be honored in the final construction.
         *
         * @param value The custom Spokestack builder.
         */
        fun withSpokestackBuilder(value: Spokestack.Builder) =
            apply { this.spokestackBuilder = value }

        /**
         * Set the level at which a registered [SpokestackTrayListener] will receive log events.
         *
         * @param value The minimum level of Spokestack log/trace events to be sent to
         * a registered listener.
         */
        fun logLevel(value: Int) = apply { this.logLevel = value }

        /**
         * Set the URL to the directory containing wakeword model files. If omitted, wakeword
         * activation will be unavailable at runtime.
         *
         * @param value The URL to the directory containing wakeword models. This URL should not
         * end in a filename; the library will automatically append filenames and use
         * this base URL to download multiple models.
         */
        fun wakewordModelURL(value: String?) = apply { this.wakewordModelURL = value }

        /**
         * Set the URL to the directory containing NLU files. If omitted, NLU will be
         * unavailable at runtime.
         *
         * @param value The URL to the directory containing NLU files. This URL should not end in a
         * filename; the library will automatically append filenames and use this base URL to
         * download multiple files.
         */
        fun nluURL(value: String?) = apply { this.nluURL = value }

        /**
         * Set the URL to a Rasa Open Source server. If set, [nluURL] will be ignored,
         * as Rasa Open Source will handle both NLU and dialogue management.
         *
         * For production use, this endpoint should be secured. See the documentation
         * for [io.spokestack.spokestack.rasa.RasaOpenSourceNLU] for descriptions of authentication
         * methods you can specify with [withProperty].
         *
         * @param value The URL to the Rasa Open Source server's REST endpoint.
         */
        fun rasaOssUrl(value: String?) = apply { this.rasaOssURL = value }

        /**
         * Set whether wakeword and NLU models should be unconditionally redownloaded on
         * launch. Defaults to `false`.
         *
         * @param value `true` if models should be redownloaded on launch.
         */
        fun refreshModels(value: Boolean) = apply { this.refreshModels = value }

        /**
         * Register a listener to receive events from both Spokestack and the tray. This is
         * the only way an application will be notified about speech recognition and/or NLU
         * results.
         *
         * @param listener The listener to register.
         */
        fun withListener(listener: SpokestackTrayListener) = apply { this.listener = listener }

        /**
         * Register a transcript editor to be applied to ASR results before they are sent to the NLU
         * for classification. This can be useful for, e.g., correcting domain-specific terminology
         * that ASR consistently misrecognizes.
         *
         * @param editor The editor to apply to ASR transcripts.
         */
        fun withTranscriptEditor(editor: TranscriptEditor) =
            apply { this.transcriptEditor = editor }

        /**
         * Create a finalized Spokestack Tray configuration from the current state of this builder.
         */
        fun build() = TrayConfig(this)
    }
}
