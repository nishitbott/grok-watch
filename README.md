# Grok for Galaxy Watch

Standalone Wear OS app for Galaxy Watch Ultra (Wear OS 5). Ask Grok by voice or by typing, then read the reply on the watch. Package `com.nishit.grokwatch`. The launcher name is **Grok**.

The app talks to a bridge:

- `POST {base}/api/watch` with `{"prompt":"..."}` returns `202 {"id","status"}`
- `GET {base}/api/watch/{id}` returns `{"id","prompt","status":"pending|done|error","reply","error"}`
- `GET {base}/api/watch` returns `{"jobs":[...]}`

Requests send `Authorization: Bearer <key>`.

## Requirements

- JDK 17
- Android SDK platform 34
- Gradle 8.7 wrapper jar (see below). This repo includes `gradle/wrapper/gradle-wrapper.properties` and does not include `gradle-wrapper.jar`.

Versions: AGP 8.5.2, Kotlin 1.9.24, Compose compiler extension 1.5.14, Wear Compose 1.3.1, minSdk 30, targetSdk 34, compileSdk 34.

## Configure the API key

Create `local.properties` in the project root (it is gitignored). Never commit a key.

```properties
sdk.dir=/path/to/Android/Sdk
grok.apiKey=your_key_here
```

`BuildConfig.GROK_API_KEY` is that value, or an empty string when `grok.apiKey` is missing. `BuildConfig.GROK_BASE_URL` defaults to `https://discord-grok-bridge.vercel.app`. Change either value later on the watch in Settings. The key is stored in DataStore and shown masked.

JSON uses the platform `org.json` classes (`JSONObject` / `JSONArray`). A separate `org.json` Maven artifact is not added, because it clashes with the copy already in `android.jar`.

## Generate the wrapper, then build

From a machine that already has Gradle installed:

```bash
gradle wrapper --gradle-version 8.7
./gradlew :app:assembleDebug
```

`gradle wrapper` writes `gradle-wrapper.jar` plus the `gradlew` scripts. The properties file already points at the Gradle 8.7 distribution.

The debug APK is:

```text
app/build/outputs/apk/debug/grok-watch-debug.apk
```

Copy it to the name used for sideload:

```bash
cp app/build/outputs/apk/debug/grok-watch-debug.apk grok-watch.apk
```

## Sideload on Galaxy Watch Ultra

On the watch:

1. Open **Settings > About watch > Software information**.
2. Tap **Software version** 5 times. Developer options turns on.
3. Open **Developer options** and enable **ADB debugging** and **Wireless debugging**.
4. Open **Pair new device**. Note the pairing host, port, and pairing code. Wireless debugging also shows a separate connection host and port.

On the computer, with the watch on the same Wi-Fi network:

```bash
adb pair IP:PORT CODE
adb connect IP:PORT
adb install -r grok-watch.apk
```

Use the pairing address with `adb pair`, then the connection address with `adb connect`. `adb devices` should list the watch before install.
