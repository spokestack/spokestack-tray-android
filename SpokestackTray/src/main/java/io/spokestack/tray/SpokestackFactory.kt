package io.spokestack.tray

import android.content.Context
import androidx.lifecycle.Lifecycle
import io.spokestack.spokestack.Spokestack
import io.spokestack.spokestack.SpokestackAdapter
import java.io.File

object SpokestackFactory {
    var spokestack: Spokestack? = null
    var spokestackListener: SpokestackAdapter? = null

    /**
     * Get the singleton Spokestack instance with a new configuration.
     */
    fun getConfigured(
        trayConfig: TrayConfig,
        context: Context,
        lifecycle: Lifecycle,
        listener: SpokestackAdapter
    ): Spokestack {
        if (spokestack == null) {
            val builder =
                trayConfig.spokestackBuilder
                    .setProperty("spokestack-id", trayConfig.clientId)
                    .setProperty("spokestack-secret", trayConfig.clientSecret)
                    .setProperty("trace-level", trayConfig.logLevel)
                    .withTranscriptEditor(trayConfig.transcriptEditor)
                    .withAndroidContext(context.applicationContext)
                    .withLifecycle(lifecycle)
                    .addListener(listener)
            trayConfig.properties.entries.forEach { entry ->
                builder.setProperty(entry.key, entry.value)
            }
            this.spokestackListener = listener
            spokestack = withModels(trayConfig, builder, context).build()
        } else {
            spokestack?.let {
                it.removeListener(this.spokestackListener)
                it.addListener(listener)
                it.prepareTts()
                this.spokestackListener = listener
            }
        }
        return spokestack!!
    }

    private fun withModels(
        trayConfig: TrayConfig,
        builder: Spokestack.Builder,
        context: Context
    ): Spokestack.Builder {
        if (trayConfig.wakewordModelURL != null) {
            ensureWakeword(trayConfig, builder, context)
        } else {
            builder.withoutWakeword()
        }
        if (trayConfig.nluURL != null) {
            ensureNlu(trayConfig, builder, context)
        } else {
            builder.withoutNlu()
        }
        return builder
    }

    private fun ensureWakeword(
        trayConfig: TrayConfig,
        builder: Spokestack.Builder,
        context: Context
    ) {
        val cacheDir = context.cacheDir.absolutePath
        val models = listOf("filter", "encode", "detect")
        val wakewordPath = removeTrailingFile(trayConfig.wakewordModelURL!!)
        models.forEach { model ->
            val downloadUrl = "$wakewordPath/$model.tflite"
            val path = namespacedPath(cacheDir, "$model.tflite")
            if (!File(path).exists() || trayConfig.refreshModels) {
                downloadFile(downloadUrl, cacheDir)
            }
            builder.setProperty("wake-$model-path", path)
        }
    }

    private fun ensureNlu(trayConfig: TrayConfig, builder: Spokestack.Builder, context: Context) {
        val cacheDir = context.cacheDir.absolutePath
        val files = listOf("nlu.tflite", "metadata.json", "vocab.txt")
        val missing = anyMissing(cacheDir, files)
        if (missing || trayConfig.refreshModels) {
            val nluPath = removeTrailingFile(trayConfig.nluURL!!)
            files.forEach {
                val downloadUrl = "$nluPath/$it"
                downloadFile(downloadUrl, cacheDir)
            }
        }
        builder.setProperty("nlu-metadata-path", namespacedPath(cacheDir, "metadata.json"))
        builder.setProperty("nlu-model-path", namespacedPath(cacheDir, "nlu.tflite"))
        builder.setProperty("wordpiece-vocab-path", namespacedPath(cacheDir, "vocab.txt"))
    }

    private fun anyMissing(dir: String, files: List<String>): Boolean {
        return files.any {
            val path = namespacedPath(dir, it)
            !File(path).exists()
        }
    }

    private fun namespacedPath(dir: String, fileName: String): String {
        val prefixed = namespaced_key(fileName)
        return "$dir/$prefixed"
    }

    private fun removeTrailingFile(url: String): String {
        return url.replace(Regex("/\\w+\\.\\w+$"), "")
    }
}
