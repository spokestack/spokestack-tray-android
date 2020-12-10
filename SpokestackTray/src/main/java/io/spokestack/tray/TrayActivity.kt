package io.spokestack.tray

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * An [AppCompatActivity] subclass that manages state and fragment concerns
 * common to all `Activity`s that wish to include [SpokestackTray].
 */
abstract class TrayActivity : AppCompatActivity() {
    private lateinit var fragmentTag: String

    /**
     * `tray` can be accessed when the activity is in the STARTED state
     * (at the end of `onStart`).
     */
    lateinit var tray: SpokestackTray

    /**
     * Get the configuration used to create the Spokestack Tray.
     *
     * @return A fully built [TrayConfig].
     */
    abstract fun getTrayConfig(): TrayConfig

    /**
     * Get the listener that should receive events from Spokestack Tray.
     *
     * This should usually be implemented by each subclass because the UIs of
     * different activities will necessarily react differently to voice
     * commands.
     *
     * @return A component that listens for tray events.
     */
    abstract fun getTrayListener(): SpokestackTrayListener

    override fun onCreate(savedInstanceState: Bundle?) {
        fragmentTag = resources.getString(R.string.fragment_tag)
        val trayConfig = getTrayConfig()
        supportFragmentManager.fragmentFactory = SpokestackTrayFactory(trayConfig)
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        tray = supportFragmentManager.findFragmentByTag(fragmentTag) as SpokestackTray
        super.onStart()
    }
}