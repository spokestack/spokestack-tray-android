# Spokestack Tray

[ ![JCenter](https://api.bintray.com/packages/spokestack/io.spokestack/spokestack-tray-android/images/download.svg) ](https://bintray.com/spokestack/io.spokestack/spokestack-tray-android/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

A `Fragment` for adding voice control via Spokestack to any Android app. You can find a simple demo app that shows the tray in action in the `example` directory.

## Usage

By default, Spokestack Tray handles ASR, NLU, and TTS for voice interactions with users—that's converting their voice to text, processing that text to produce an action, and synthesizing the app's response to be read back to the user. For more information on these features, see [the Spokestack docs](https://www.spokestack.io/docs/Concepts).

To use NLU and TTS, you'll need a [free Spokestack account](https://www.spokestack.io/create). From your account page, you'll be able to create and download NLU models; and the client ID and secret key are needed at runtime for TTS requests.

As mentioned above, Spokestack Tray is implemented as a `Fragment` that renders on top of your existing `Activity` and handles voice interaction, so you'll want to add it to your activity's layout:

```xml
    <!-- nested in the main layout, after other views/sublayouts -->

    <include
        layout="@layout/spokestack_tray_fragment"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent" />

```

Then in your activity itself:

```kotlin
import io.spokestack.tray.*

class MyActivity : AppCompatActivity(), SpokestackTrayListener {

    lateinit var tray: SpokestackTray

    // ...

    override fun onCreate(savedInstanceState: Bundle?) {

        val config = TrayConfig.Builder()
            // credentials from your Spokestack account
            .credentials("spokestack-client-id", "spokestack-secret-key")
            .wakewordModelURL("https://path-to-wakeword-models")
            .nluURL("https://path-to-nlu-files")
            .withListener(this)
            // optional builder customization; see the documentation for more details...
            .build()
        supportFragmentManager.fragmentFactory = SpokestackTrayFactory(config)

        // note that the factory is instantiated and set on the manager BEFORE calling
        // `super.onCreate()`
        super.onCreate(savedInstanceState)

        // set the value of the lateinit `tray` var
        tray = SpokestackTray.getInstance(config)
    }
```

If you prefer using a Fragment transaction manager instead of declaring the Tray Fragment in XML, add this after the code in the last section:

```kotlin
        val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader,
            SpokestackTray::class.java.name)
        supportFragmentManager.beginTransaction()
            .replace(R.id.spokestack_tray, fragment)
            .commitNow()
```

## Configuration

The above sample will get you up and running with minimal fuss, but it's far from all that Spokestack Tray offers. When you're building a `TrayConfig` instance, you have access to the full underlying `Spokestack` builder and all its configuration, which you can read about [here](https://www.spokestack.io/docs/Android/setup-wrapper). This lets you do things like change ASR providers, set up custom listeners for events from individual systems, and add your own components.

There are also a range of options that are applicable to the Tray itself:



## License

Copyright 2020 Spokestack, Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
