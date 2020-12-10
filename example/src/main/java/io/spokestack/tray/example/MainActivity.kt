package io.spokestack.tray.example

import android.os.Bundle
import io.spokestack.spokestack.nlu.NLUResult
import io.spokestack.spokestack.util.EventTracer
import io.spokestack.tray.*

class MainActivity : TrayActivity(), SpokestackTrayListener {
    private val greeting = "Welcome! This example uses Minecraft sample models. " +
            "Try saying, \"How do I make a castle?\""
    private var lastResponse: String = greeting

    override fun getTrayConfig(): TrayConfig {
        return TrayConfig.Builder()
            .credentials(
                "f0bc990c-e9db-4a0c-a2b1-6a6395a3d97e",
                "5BD5483F573D691A15CFA493C1782F451D4BD666E39A9E7B2EBE287E6A72C6B6"
            )
            .wakewordModelURL("https://d3dmqd7cy685il.cloudfront.net/model/wake/spokestack")
            .nluURL("https://d3dmqd7cy685il.cloudfront.net/nlu/production/shared/XtASJqxkO6UwefOzia-he2gnIMcBnR2UCF-VyaIy-OI")
            .logLevel(EventTracer.Level.PERF.value())
            .withListener(this)
            .greeting(greeting)
            // for righthand orientation, uncomment the following line and ensure the fragment is
            // right-aligned to its parent in the layout
//            .orientation(TrayConfig.Orientation.RIGHT)
            .build()
    }

    override fun getTrayListener(): SpokestackTrayListener {
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onLog(message: String) {
        println("LOG: $message")
    }

    override fun onClassification(result: NLUResult): VoicePrompt {
        val (text, followup) = when (result.intent) {
            "AMAZON.RepeatIntent" -> Pair(lastResponse, true)
            "AMAZON.YesIntent" -> Pair("I heard you say yes! What would you like to make?", true)
            "AMAZON.NoIntent" -> Pair("I heard you say no. Goodbye.", false)
            "AMAZON.StopIntent", "AMAZON.CancelIntent", "AMAZON.FallbackIntent" ->
                Pair("Goodbye!", false)
            "RecipeIntent" -> Pair("If I were a real app, I would show a screen now on how " +
                    "to make a ${result.slots["Item"]?.value}. Want to continue?", true)

            else -> Pair(lastResponse, true)
        }
        return VoicePrompt(text, expectFollowup = followup)
    }

    override fun onError(error: Throwable) {
        println("ERROR: ${error.localizedMessage}")
    }

    override fun onOpen() {
        println("OPENED")
    }

    override fun onClose() {
        println("CLOSED")
    }
}
