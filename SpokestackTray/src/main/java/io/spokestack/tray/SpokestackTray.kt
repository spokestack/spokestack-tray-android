@file:JvmName("SpokestackTray")

package io.spokestack.tray

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.spokestack.spokestack.SpeechContext
import io.spokestack.spokestack.Spokestack
import io.spokestack.spokestack.SpokestackAdapter
import io.spokestack.spokestack.SpokestackModule
import io.spokestack.spokestack.dialogue.DialogueEvent
import io.spokestack.spokestack.nlu.NLUResult
import io.spokestack.spokestack.tts.SynthesisRequest
import io.spokestack.spokestack.tts.TTSEvent
import io.spokestack.spokestack.util.EventTracer
import io.spokestack.tray.databinding.TrayFragmentBinding
import io.spokestack.tray.message.Message
import io.spokestack.tray.message.MessageAdapter

/**
 * A Fragment that exposes the primary functionality of the Spokestack tray.
 *
 * The tray wraps an instance of the
 * [`Spokestack`](https://github.com/spokestack/spokestack-android) library that is used to
 * capture user voice input, convert it to text, display that text, and classify it using an NLU.
 * An application can register a component to listen to tray events, return responses to NLU
 * classifications, and have those responses automatically synthesized via Spokestack TTS to be
 * read to the user.
 *
 * To include the tray in your `Activity`'s `layout.xml`:
 *
 *
 * ```xml
 * <!-- nested in the main layout, after other views/sublayouts -->
 *
 *     <include
 *          android:id="@+id/tray_fragment"
 *          layout="@layout/spokestack_tray_fragment"
 *      />
 * ```
 *
 * Then in the `Activity` itself:
 *
 * ```kotlin
 * import io.spokestack.tray.*
 *
 * class MyActivity : AppCompatActivity(), SpokestackTrayListener {
 *
 *     lateinit var tray: SpokestackTray
 *
 *     // ...
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *
 *         val config = TrayConfig.Builder()
 *             // credentials from your Spokestack account
 *             .credentials("spokestack-client-id", "spokestack-secret-key")
 *             .wakewordModelURL("https://path-to-wakeword-models")
 *             .nluURL("https://path-to-nlu-files")
 *             .withListener(this)
 *             // optional builder customization; see the documentation for more details...
 *             .build()
 *         supportFragmentManager.fragmentFactory = SpokestackTrayFactory(config)
 *
 *         // note that the factory is instantiated and set on the manager BEFORE calling
 *         // `super.onCreate()`
 *         super.onCreate(savedInstanceState)
 *     }
 *
 *     override fun onStart() {
 *         // set the value of the lateinit `tray` var
 *         tray = SpokestackTray.getInstance(config)
 *         super.onStart()
 *     }
 * ```
 *
 */
class SpokestackTray constructor(
    private val config: TrayConfig,
    private val state: TrayState
) : Fragment(),
    MotionLayout.TransitionListener,
    AutoCloseable {

    private val logTag = javaClass.simpleName
    private val audioPermission = 1337

    private lateinit var spokestack: Spokestack
    private lateinit var listenBubbleBg: ListenBubble
    private lateinit var spokestackListener: SpokestackListener

    private var _binding: TrayFragmentBinding? = null
    private var ready: Boolean = false
    private var playTts: Boolean = true
    private var openOnPermissions: Boolean = false

    // binding is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    var listener: SpokestackTrayListener? = config.listener

    /**
     * @return The Spokestack instance in use by the tray.
     */
    fun spokestack(): Spokestack {
        return spokestack
    }

    /**
     * @return Whether the tray is currently set to synthesize and play audio sent from the
     * application.
     */
    fun audioEnabled(): Boolean {
        return playTts
    }

    /**
     * Explicitly start the underlying [Spokestack] instance and its speech pipeline.
     *
     * This method should not be required under normal usage, but is necessary after an
     * explicit call to [stop] or [close] to re-enable voice control without destroying the tray
     * fragment.
     */
    fun start() {
        spokestack.start()
    }

    /**
     * Release resources associated with the Tray and the underlying Spokestack instance.
     * Note that `spokestack.close()`/`.stop()` are not called automatically because in the normal
     * case, we want Spokestack to continue listening even if the fragment is killed (i.e., on
     * device rotation).
     *
     * A convenience method that renames [close] and provides parallelism for [start].
     */
    fun stop() {
        close()
    }

    /**
     * Release resources associated with the Tray and the underlying Spokestack instance.
     * Note that `spokestack.close()`/`.stop()` are not called automatically because in the normal
     * case, we want Spokestack to continue listening even if the fragment is killed (i.e., on
     * device rotation).
     *
     * **Note**: This method is **not** for visually sliding the tray's UI closed;
     * use [setOpen] for that. This method's name is inherited from
     * [AutoCloseable].
     */
    override fun close() {
        spokestack.close()
    }

    /**
     * Clear the tray's internal conversation history, making the next time it opens act like
     * the first without resetting any user modifications like muting TTS or changing the tray
     * size.
     */
    fun clear() {
        state.clear()
    }

    /**
     * Get a copy of the tray's current internal state.
     *
     * This state can be transferred to a [Bundle] in [onSaveInstanceState]:
     *
     * ```kotlin
     * outState.putParcelable("tray_state", getState())
     * ```
     *
     * @return A copy of the tray's internal state.
     */
    fun getState(): TrayState {
        return state.copy()
    }

    /**
     * Load the tray's internal state from a saved version.
     *
     * If saved to a [Bundle] as mentioned in [getState], the state can be retrieved
     * as a [android.os.Parcelable]:
     *
     * ```kotlin
     * val trayState: TrayState? = savedInstanceState.getParcelable("tray_state")
     * ```
     *
     * The UI will be updated immediately to reflect the loaded state.
     *
     * @param savedState A previously saved version of the tray's state.
     */
    fun loadState(savedState: TrayState) {
        state.loadFrom(savedState)
        activity?.runOnUiThread {
            restoreState()
        }
    }

    /**
     * Submits text to the tray to be synthesized. Synthesis results will be read by the tray's
     * playback mechanism if the user has not disabled audio. The `text` field of the prompt will
     * be displayed regardless of the audio setting.
     *
     * @param prompt The prompt to be synthesized.
     *
     * @see [audioEnabled]
     */
    fun say(prompt: Prompt) {
        if (prompt.text.isNotEmpty()) {
            synthesize(prompt)
        }
        displayAndListen(prompt)
    }

    private fun synthesize(prompt: Prompt) {
        spokestackListener.expectFollowup = prompt.expectFollowup
        if (audioEnabled()) {
            val request = SynthesisRequest.Builder(prompt.voice)
                .withMode(config.ttsMode)
                .withVoice(config.voice)
                .build()
            spokestack.synthesize(request)
        }
    }

    private fun displayAndListen(prompt: Prompt) {
        val (text, _, imageURL, listen) = prompt
        addMessage(text, imageURL, isSystem = true)
        if (audioEnabled() || !listen) {
            // if playback is enabled, the synthesis callback handles reactivating ASR,
            // so we don't explicitly listen here
            setOpen(open = true, listen = false)
        } else {
            binding.messageStream.postDelayed(
                { setOpen(open = true, listen = true) },
                1000
            )
        }
    }

    /**
     * @return Whether the tray is currently open.
     */
    fun isOpen(): Boolean {
        val curState = binding.trayMotion.currentState
        return curState == R.id.tray_opened_right || curState == R.id.tray_opened_left
    }

    /**
     * Opens or closes the tray UI, optionally listening on open.
     *
     * @param open Whether the tray should open or close.
     * @param listen Whether the tray should begin listening when opened.
     * Defaults to the value of `open`.
     */
    fun setOpen(open: Boolean, listen: Boolean = open) {
        if (open && !checkMicPermission()) {
            Log.w(logTag, "Microphone permission must be granted for Spokestack to operate")
            this.openOnPermissions = true
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), audioPermission)
            return
        }

        var listening = open && listen
        if (open) {
            if (maybeGreet()) {
                listening = false
            } else if (listen) {
                spokestack.activate()
            }
            if (config.haptic) {
                binding.micButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        } else {
            if (spokestack.speechPipeline.context.isActive) {
                spokestack.deactivate()
            }
        }

        activity?.runOnUiThread {
            setListening(listening)
            if (open) {
                binding.trayMotion.transitionToEnd()
            } else {
                binding.trayMotion.transitionToStart()
            }
        }
    }

    private fun maybeGreet(): Boolean {
        val isFirstOpen = state.firstOpen
        state.firstOpen = false
        var playedGreeting = false
        if (isFirstOpen && config.greeting != "") {
            if (config.sayGreeting) {
                val prompt = Prompt(config.greeting, expectFollowup = true)
                synthesize(prompt)
                playedGreeting = true
            }
            addMessage(config.greeting, isSystem = true)
        }
        return playedGreeting
    }

    private fun setListening(isListening: Boolean) {
        if (isListening) {
            binding.messageStream.adapter?.itemCount?.let {
                binding.messageStream.scrollToPosition(it - 1)
            }
            listenBubbleBg.start()
            binding.listenBubble.visibility = VISIBLE
            state.isActive = true
        } else {
            binding.listenBubble.visibility = INVISIBLE
            listenBubbleBg.stop()
            state.isActive = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenCreated {
            spokestackListener = SpokestackListener()
            spokestack =
                SpokestackFactory.getConfigured(
                    config,
                    requireContext(),
                    spokestackListener
                )
            if (checkMicPermission()) {
                spokestack.start()
            }
            setReady()
        }
    }

    private fun setReady() {
        // if the model downloads finish after the fragment has been started, wait until now to
        // make the UI visible
        if (lifecycle.currentState >= Lifecycle.State.STARTED) {
            binding.trayMotion.visibility = VISIBLE
        }
        ready = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TrayFragmentBinding.inflate(inflater)

        configureButtons()
        configureAnimations(requireContext())

        configureMessageStream()

        // if we're still downloading models, hide the UI for now; it will be made visible when
        // the download is complete
        val visible = if (ready) VISIBLE else INVISIBLE
        binding.trayMotion.visibility = visible
        binding.trayMotion.addTransitionListener(this)

        savedInstanceState?.classLoader = javaClass.classLoader

        return binding.root
    }

    private fun configureButtons() {
        binding.micButton.setOrientation(config.orientation)
        binding.micButton.trayView = binding.trayView
        binding.micButton.setTransitionProgress = this::setOpenPercentage

        binding.backButton.setOnClickListener {
            spokestack.stopPlayback()
            setOpen(false)
        }

        binding.soundButton.setOnClickListener {
            this.playTts = !this.playTts
            setSoundButtonBg()
        }
    }

    private fun setOpenPercentage(percent: Float) {
        when (percent) {
            0.0f -> setOpen(false)
            1.0f -> setOpen(true)
            else -> binding.trayMotion.progress = percent
        }
    }

    private fun setSoundButtonBg() {
        val background =
            if (audioEnabled()) {
                ContextCompat.getDrawable(requireContext(), R.drawable.sound_on_btn)
            } else {
                ContextCompat.getDrawable(requireContext(), R.drawable.sound_off_btn)
            }
        activity?.runOnUiThread {
            binding.soundButton.background = background
        }
    }

    private fun configureAnimations(context: Context) {
        // default is lefthand orientation; only change if necessary
        if (config.orientation == TrayConfig.Orientation.RIGHT) {
            binding.trayMotion.setTransition(R.id.tray_closed_right, R.id.tray_opened_right)
        }
        val px = resources.getDimensionPixelSize(R.dimen.spsk_listenButtonWidth).toFloat()
        listenBubbleBg = ListenBubble(context, px)
        binding.listenBubble.background = listenBubbleBg
    }

    private fun configureMessageStream() {
        val viewAdapter = MessageAdapter(requireContext())
        viewAdapter.submitList(state.messages)

        val observer = { messages: List<Message> ->
            val lastMessage = messages.size - 1
            viewAdapter.notifyItemChanged(lastMessage)
            binding.messageStream.scrollToPosition(lastMessage)
        }
        viewAdapter.onImageLoad = observer

        // observe messages for changes
        state.liveData().observe(viewLifecycleOwner, observer)

        binding.messageStream.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = viewAdapter
        }
    }

    override fun onResume() {
        if (checkMicPermission()) {
            setOpen(state.isOpen, state.isActive)
        }
        // ensure that spokestack has the right listener (e.g., after returning to an
        // already-created fragment on the backstack)
        spokestack =
            SpokestackFactory.getConfigured(config, requireContext(), spokestackListener)
        restoreState()
        super.onResume()
    }

    private fun restoreState() {
        this.playTts = state.playTts
        setSoundButtonBg()
        if (state.messageStreamHeight > 0) {
            binding.messageStream.layoutParams.apply {
                height = state.messageStreamHeight
            }
        }
        spokestackListener.expectFollowup = state.expectFollowup
    }

    override fun onPause() {
        state.apply {
            messageStreamHeight = binding.messageStream.layoutParams.height
            playTts = audioEnabled()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        if (listenBubbleBg.isRunning) {
            listenBubbleBg.stop()
        }
        super.onDestroyView()
    }

    override fun onDetach() {
        _binding = null
        super.onDetach()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (this.openOnPermissions
            && requestCode == audioPermission
            && PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()
        ) {
            setOpen(true)
        }
    }

    private fun checkMicPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun addMessage(text: String, imageURL: String = "", isSystem: Boolean) {
        activity?.runOnUiThread {
            state.addMessage(Message(isSystem, text, imageURL))
        }
    }

    override fun onTransitionCompleted(layout: MotionLayout?, state: Int) {
        if (isOpen()) {
            listener?.onOpen()
            this.state.isOpen = true
        } else {
            listener?.onClose()
            this.state.isOpen = false
        }
    }

    // unnecessary MotionLayout listener methods
    override fun onTransitionStarted(p0: MotionLayout?, startId: Int, endId: Int) {
    }

    override fun onTransitionChange(p0: MotionLayout?, startId: Int, endId: Int, progress: Float) {
    }

    override fun onTransitionTrigger(p0: MotionLayout?, trigger: Int, pos: Boolean, prog: Float) {
    }

    inner class SpokestackListener : SpokestackAdapter() {
        var expectFollowup: Boolean = false
            set(value) {
                state.expectFollowup = value
                field = value
            }

        override fun nluResult(result: NLUResult) {
            // if there's a dialogue manager, let it handle generating the
            // prompts; otherwise, defer to the listener's NLU handling logic
            if (spokestack.dialogueManager == null) {
                try {
                    listener?.onClassification(result)?.let {
                        say(it)
                    }
                } catch (e: Exception) {
                    listener?.onError(e)
                }
            }
        }

        override fun onDialogueEvent(event: DialogueEvent) {
            try {
                listener?.onDialogueEvent(event)?.let {
                    say(it)
                }
            } catch (e: Exception) {
                listener?.onError(e)
            }
        }

        override fun ttsEvent(event: TTSEvent) {
            when (event.type) {
                TTSEvent.Type.PLAYBACK_COMPLETE -> {
                    if (this.expectFollowup) {
                        resumeListening()
                    } else {
                        setOpen(false)
                    }
                    onTrace(EventTracer.Level.PERF, "TTS audio complete")
                }
                TTSEvent.Type.ERROR -> dispatchError(event.error)
                TTSEvent.Type.AUDIO_AVAILABLE -> onTrace(
                    EventTracer.Level.PERF,
                    "TTS audio available"
                )
                TTSEvent.Type.PLAYBACK_STARTED -> onTrace(
                    EventTracer.Level.PERF,
                    "TTS started playing"
                )
                TTSEvent.Type.PLAYBACK_STOPPED -> onTrace(
                    EventTracer.Level.PERF,
                    "TTS stopped playing"
                )
                // no-op
                else -> Unit
            }
        }

        private fun resumeListening() {
            spokestack.activate()
            this.expectFollowup = false
        }

        override fun speechEvent(event: SpeechContext.Event, speechContext: SpeechContext) {
            var logMsg: String? = null
            when (event) {
                SpeechContext.Event.ACTIVATE -> {
                    logMsg = event.name
                    setOpen(true)
                }
                SpeechContext.Event.PARTIAL_RECOGNIZE, SpeechContext.Event.RECOGNIZE -> {
                    displayTranscript(speechContext)
                }
                SpeechContext.Event.TIMEOUT -> {
                    logMsg = event.name
                    setOpen(false)
                }
                SpeechContext.Event.DEACTIVATE -> {
                    logMsg = event.name
                    setListening(false)
                }
                else -> return  // noop; traces and errors are handled by the superclass
            }

            // send PERF log for some events if configured to receive them
            logMsg?.let {
                if (config.logLevel <= EventTracer.Level.PERF.value()) {
                    trace(SpokestackModule.SPEECH_PIPELINE, it)
                }
            }
        }

        private fun displayTranscript(speechContext: SpeechContext) {
            var message = speechContext.transcript
            if (config.transcriptEditor != null) {
                message = config.transcriptEditor.editTranscript(speechContext.transcript)
            }
            if (message.isNotEmpty()) {
                state.addOrUpdateUserMessage(message)
            }
        }

        override fun trace(module: SpokestackModule, message: String) {
            listener?.onLog(message)
        }

        override fun error(module: SpokestackModule, err: Throwable) {
            dispatchError(err)
        }

        private fun dispatchError(error: Throwable) {
            listener?.onError(error)
        }
    }
}
