@file:JvmName("SpokestackTray")

package io.spokestack.tray

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.spokestack.spokestack.SpeechContext
import io.spokestack.spokestack.Spokestack
import io.spokestack.spokestack.SpokestackAdapter
import io.spokestack.spokestack.SpokestackModule
import io.spokestack.spokestack.nlu.NLUResult
import io.spokestack.spokestack.tts.SynthesisRequest
import io.spokestack.spokestack.tts.TTSEvent
import io.spokestack.spokestack.util.EventTracer
import io.spokestack.tray.databinding.TrayFragmentBinding
import io.spokestack.tray.message.Message
import io.spokestack.tray.message.MessageAdapter
import kotlin.math.ceil

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
 * <include
 * layout="@layout/spokestack_tray_fragment"
 * android:layout_width="wrap_content"
 * android:layout_height="wrap_content"
 * app:layout_constraintBottom_toBottomOf="parent"
 * app:layout_constraintStart_toStartOf="parent" />
 *
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
 *
 *         // set the value of the lateinit `tray` var
 *         tray = SpokestackTray.getInstance(config)
 *     }
 * ```
 *
 * If you prefer using a Fragment transaction manager instead of declaring the Fragment in
 * XML, add this after the code in the last section:
 *
 * ```kotlin
 *     val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader,
 *         SpokestackTray::class.java.name)
 *     supportFragmentManager.beginTransaction()
 *         .replace(R.id.spokestack_tray, fragment)
 *         .commitNow()
 * ```
 *
 */
class SpokestackTray private constructor(private val config: TrayConfig) : Fragment(),
    MotionLayout.TransitionListener {

    private val logTag = javaClass.simpleName
    private val audioPermission = 1337

    private lateinit var viewModel: TrayViewModel
    private lateinit var spokestack: Spokestack
    private lateinit var listenGradient: ScrollingGradient
    private lateinit var spokestackListener: SpokestackListener

    private var _binding: TrayFragmentBinding? = null
    private var ready: Boolean = false
    private var playTts: Boolean = true
    private var openOnPermissions: Boolean = false

    // binding is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!
    internal val trayListener: SpokestackTrayListener? = config.listener

    companion object {
        private var tray: SpokestackTray? = null
        fun getInstance(config: TrayConfig): SpokestackTray {
            if (tray == null) {
                tray = SpokestackTray(config)
            }
            return tray!!
        }
    }

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
     * Submits text to the tray to be synthesized. Synthesis results will be read by the tray's
     * playback mechanism if the user has not disabled audio. The `text` field of the prompt will
     * be displayed regardless of the audio setting.
     *
     * @param prompt The prompt to be synthesized.
     *
     * @see [audioEnabled]
     */
    fun say(prompt: VoicePrompt) {
        synthesize(prompt)
        displayAndListen(prompt.text, prompt.expectFollowup)
    }

    private fun synthesize(prompt: VoicePrompt) {
        spokestackListener.expectFollowup = prompt.expectFollowup
        if (audioEnabled()) {
            val request = SynthesisRequest.Builder(prompt.voice)
                .withMode(config.ttsMode)
                .withVoice(config.voice)
                .build()
            spokestack.synthesize(request)
        }
    }

    private fun displayAndListen(text: String, listen: Boolean) {
        addMessage(text, isSystem = true)
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
        return binding.trayMotion.currentState == R.id.tray_opened
    }

    /**
     * Opens or closes the tray, optionally listening on open.
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
        var targetState = R.id.tray_closed
        if (open) {
            spokestack.start()
            if (maybeGreet()) {
                listening = false
            } else if (listen) {
                spokestack.activate()
            }
            targetState = R.id.tray_opened
            if (config.haptic) {
                binding.micButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        } else {
            spokestack.deactivate()
        }

        activity?.runOnUiThread {
            setListening(listening)
            binding.trayMotion.transitionToState(targetState)
        }
    }

    private fun maybeGreet(): Boolean {
        val isFirstOpen = viewModel.state.firstOpen
        viewModel.state.firstOpen = false
        var playedGreeting = false
        if (isFirstOpen && config.greeting != "") {
            if (config.sayGreeting) {
                val prompt = VoicePrompt(config.greeting, expectFollowup = true)
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
            val text = requireContext().resources.getString(R.string.spsk_listening)
            binding.listenText.text = text
            binding.listenGradient.background = listenGradient
            listenGradient.start()
        } else {
            listenGradient.stop()
            binding.listenText.text = ""
            val color =
                ContextCompat.getColor(requireContext(), R.color.spsk_colorGradientOne)
            binding.listenGradient.setBackgroundColor(color)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(this).get(TrayViewModel::class.java)
        lifecycleScope.launchWhenCreated {
            spokestackListener = SpokestackListener()
            spokestack =
                SpokestackFactory.getInstance(config, context, lifecycle, spokestackListener)
            setReady()
        }
    }

    private fun setReady() {
        // if the model downloads finish after the fragment has been started, wait until now to
        // make the UI visible
        if (lifecycle.currentState >= Lifecycle.State.STARTED) {
            binding.trayView.visibility = VISIBLE
        }
        ready = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = TrayFragmentBinding.inflate(layoutInflater)

        configureAnimations(requireContext())

        // if we're still downloading models, hide the UI for now; it will be made visible when
        // the download is complete
        val visible = if (ready) VISIBLE else INVISIBLE
        binding.trayMotion.visibility = visible
        binding.trayMotion.addTransitionListener(this)
        binding.trayView.bottom = 0

        savedInstanceState?.classLoader = javaClass.classLoader
        val savedState: TrayState? = savedInstanceState?.getParcelable(TrayState.SERIALIZATION_KEY)
        viewModel.state = savedState ?: TrayState(binding.messageStream.context)
        restoreState(viewModel.state)

        configureButtons()
        configureMessageStream()

        return binding.root
    }

    private fun configureAnimations(context: Context) {
        val px = resources.displayMetrics.widthPixels.toFloat() / 2
        listenGradient = ScrollingGradient(context, px)
    }

    private fun restoreState(state: TrayState) {
        this.playTts = state.playTts
        val params: ViewGroup.LayoutParams =
            binding.messageStream.layoutParams as ViewGroup.LayoutParams
        params.height = state.messageStreamHeight
        binding.messageStream.layoutParams = params
        setOpen(state.isActive)
    }

    private fun configureButtons() {
        binding.micButton.apply {
            setOnClickListener { setOpen(true) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                adjustInsets(this)
            }
        }
        binding.backButton.setOnClickListener { setOpen(false) }

        setSoundButtonBg()
        binding.soundButton.setOnClickListener {
            this.playTts = !this.playTts
            setSoundButtonBg()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun adjustInsets(imageButton: ImageButton) {
        val tab = imageButton.background as LayerDrawable
        val mic = tab.findDrawableByLayerId(R.id.mic_icon) as InsetDrawable
        val tabWidth = tab.intrinsicWidth
        val verticalInset = (tabWidth / 15) * 8
        val leftInset = ceil(tabWidth * .3).toInt()
        val rightInset = ceil(tabWidth * .4).toInt()
        val newMic = InsetDrawable(mic, leftInset, verticalInset, rightInset, verticalInset)
        tab.setDrawable(1, newMic)
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

    private fun configureMessageStream() {
        // value is set in TrayViewModel and can never be null
        val viewAdapter = MessageAdapter(requireContext())
        viewAdapter.submitList(viewModel.getMessages().value)

        // observe messages for changes
        viewModel.getMessages().observe(viewLifecycleOwner, { messages ->
            viewAdapter.notifyDataSetChanged()
            binding.messageStream.apply {
                scrollToPosition(messages.size - 1)
            }
        })

        binding.messageStream.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = viewAdapter
        }
    }

    override fun onResume() {
        if (checkMicPermission()) {
            spokestack.start()
            if (viewModel.state.isActive) {
                spokestack.activate()
            }
        }
        super.onResume()
    }

    override fun onPause() {
        viewModel.state.apply {
            messageStreamHeight = binding.messageStream.layoutParams.height
            viewModel.state.playTts = audioEnabled()
        }
        if (listenGradient.isRunning) {
            listenGradient.stop()
        }
        spokestack.stop()
        super.onPause()
    }

    override fun onDetach() {
        _binding = null
        super.onDetach()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(TrayState.SERIALIZATION_KEY, viewModel.state)
        super.onSaveInstanceState(outState)
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

    private fun addMessage(text: String, isSystem: Boolean) {
        activity?.runOnUiThread {
            viewModel.addMessage(Message(isSystem, text))
        }
    }

    override fun onTransitionCompleted(layout: MotionLayout?, state: Int) {
        val open = state == R.id.tray_opened
        if (open != isOpen()) {
            setOpen(open)
        }
    }

    // unnecessary MotionLayout listener methods
    override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {
    }

    override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
    }

    override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
    }

    inner class SpokestackListener : SpokestackAdapter() {
        var expectFollowup: Boolean = false

        override fun nluResult(result: NLUResult) {
            trayListener?.onClassification(result)?.let {
                say(it)
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
                }
                TTSEvent.Type.ERROR -> dispatchError(event.error)
                TTSEvent.Type.AUDIO_AVAILABLE -> onTrace(
                    EventTracer.Level.PERF,
                    "TTS audio available"
                )
                // no-op
                else -> Unit
            }
        }

        private fun resumeListening() {
            spokestack.activate()
            this.expectFollowup = false
        }

        override fun speechEvent(event: SpeechContext.Event, context: SpeechContext) {
            when (event) {
                SpeechContext.Event.ACTIVATE -> {
                    onTrace(EventTracer.Level.PERF, "ACTIVATE")
                    setOpen(true)
                }
                SpeechContext.Event.PARTIAL_RECOGNIZE, SpeechContext.Event.RECOGNIZE -> {
                    var message = context.transcript
                    if (config.transcriptEditor != null) {
                        message = config.transcriptEditor.editTranscript(context.transcript)
                    }
                    updateUserMessage(message)
                }
                SpeechContext.Event.TIMEOUT -> {
                    onTrace(EventTracer.Level.PERF, "TIMEOUT")
                    setOpen(false)
                }
                SpeechContext.Event.ERROR -> dispatchError(context.error)
                SpeechContext.Event.TRACE -> onTrace(EventTracer.Level.PERF, context.message)
                SpeechContext.Event.DEACTIVATE -> {
                    onTrace(EventTracer.Level.PERF, "DEACTIVATE")
                    setListening(false)
                }
            }
        }

        private fun updateUserMessage(text: String) {
            if (text.isEmpty()) {
                return
            }
            val message = viewModel.getMessages().value?.lastOrNull()
            if (message == null || message.isSystem) {
                addMessage(text, false)
            } else {
                viewModel.updateLastUserMessage(text)
            }
        }

        override fun trace(module: SpokestackModule, message: String) {
            trayListener?.onLog(message)
        }

        override fun error(module: SpokestackModule, err: Throwable) {
            dispatchError(err)
        }

        private fun dispatchError(error: Throwable) {
            trayListener?.onError(error)
        }
    }
}