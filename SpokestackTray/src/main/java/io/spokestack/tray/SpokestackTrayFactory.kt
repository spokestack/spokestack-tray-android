package io.spokestack.tray

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory

/**
 * A factory class used to instantiate the Spokestack Tray fragment with custom configuration.
 *
 * Use when adding the Tray to an `Activity` like the following:
 *
 * ```kotlin
 * override fun onCreate(savedInstanceState: Bundle?) {
 *
 *     val config = TrayConfig.Builder()
 *         // builder setup...
 *         .build()
 *     supportFragmentManager.fragmentFactory = SpokestackTrayFactory(config)
 *
 *     // note that the factory is instantiated and set on the manager BEFORE calling
 *     // `super.onCreate()`
 *     super.onCreate(savedInstanceState)
 *
 *     // you'll probably also want an instance of the tray around to use in your `Activity`:
 *     val tray = SpokestackTray.getInstance(config)
 * }
 * ```
 */
class SpokestackTrayFactory(private val trayConfig: TrayConfig) : FragmentFactory() {

    companion object {
        internal val state: TrayState = TrayState(messages = arrayListOf())
    }

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        if (className == SpokestackTray::class.java.name) {
            return SpokestackTray(trayConfig, state)
        }
        return super.instantiate(classLoader, className)
    }
}