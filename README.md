# HealthConnectBridge

HealthConnectBridge is a small native Android app that receives weight values from
the Home Assistant Companion app through `command_broadcast_intent` and writes
them to Health Connect as `WeightRecord` entries.

The app has no Home Assistant login, REST token, MQTT connection, polling, cloud
service, or telemetry. Home Assistant credentials remain in the Companion app.

## Requirements

- Android 9 (API 28) or newer
- Health Connect available on the device
- Home Assistant Companion app for Android
- Android Studio with JDK 17 and Android SDK 36 for building

Health Connect is integrated into Android 14 and newer. On Android 13 and older,
the separate Health Connect app must be installed and current.

## Build and install

1. Open this directory in Android Studio.
2. Let Gradle sync and install Android SDK 36 if prompted.
3. Connect an Android device or start an emulator with Google Play services.
4. Run the `app` configuration.

Command-line build:

```powershell
gradle assembleDebug
```

This project pins Gradle 8.13 in its GitHub workflow. For local command-line
builds, install Gradle 8.13 or use Android Studio's bundled Gradle integration.

The debug APK is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build on GitHub

The repository includes `.github/workflows/android-build.yml`. It runs on every
push and pull request and can also be started manually:

1. Upload the complete project directory to a GitHub repository.
2. Open the repository's **Actions** tab.
3. Select **Android build**.
4. Use **Run workflow**, or push a commit to start it automatically.
5. Open the completed workflow run.
6. Download `HealthConnectBridge-debug-<run number>` under **Artifacts**.

The downloaded ZIP contains `app-debug.apk`. Android will treat it as a debug
build, so the device may require permission to install apps from the browser or
file manager used to open it.

The workflow uses a clean Ubuntu runner with JDK 17, Android SDK 36, and Gradle
8.13. No local company certificate or GitHub secret is required for the debug
APK. A distributable release APK would additionally require a private signing
key stored as encrypted GitHub secrets.

## Grant Health Connect permission

1. Open HealthConnectBridge.
2. Tap **Grant Health Connect permission**.
3. Allow write access for weight.

The app requests only:

```text
android.permission.health.WRITE_WEIGHT
```

No Health Connect data is read. Permission can be revoked at any time through
the **Open Health Connect settings** button.

## Home Assistant automation

Replace both the entity ID and the mobile-app notify action with values from
your Home Assistant installation:

```yaml
alias: Push weight to Health Connect Bridge
mode: single

triggers:
  - trigger: state
    entity_id: sensor.xiaomi_weight

conditions:
  - condition: template
    value_template: >
      {{ trigger.to_state.state not in ['unknown', 'unavailable', 'none'] }}

actions:
  - action: notify.mobile_app_DEIN_HANDY
    data:
      message: command_broadcast_intent
      data:
        intent_package_name: de.axelcypher.healthconnectbridge
        intent_action: de.axelcypher.healthconnectbridge.WRITE_WEIGHT
        intent_extras: >-
          weight_kg:{{ trigger.to_state.state | float }},
          timestamp:{{ trigger.to_state.last_changed.isoformat() | urlencode }}:String.urlencoded,
          source:homeassistant
```

The exact notify action depends on the Android device name, for example:

```text
notify.mobile_app_pixel_8_pro
```

`timestamp` is URL-encoded because the Companion app uses commas and colons as
delimiters inside `intent_extras`. The `String.urlencoded` type causes the
Companion app to decode it before delivering the broadcast.

The receiving contract is:

```text
Package: de.axelcypher.healthconnectbridge
Action:  de.axelcypher.healthconnectbridge.WRITE_WEIGHT

weight_kg: required String, Float, Double, or another Number
timestamp: optional ISO-8601 String
source: optional; when present it must be "homeassistant"
```

Both `89.4` and `89,4` are accepted by the app. Home Assistant should send the
decimal-point form because commas delimit extras in `intent_extras`.

## Test

1. Grant the Health Connect write permission.
2. Trigger the Home Assistant automation by changing the weight entity.
3. Open HealthConnectBridge and inspect **Last received weight**, **Last written
   to Health Connect**, and **Local log**.
4. Open Health Connect and verify the weight entry.

Debug builds also include **Send test weight locally**. It sends an explicit
broadcast to the app itself and exercises the same receiver and repository as a
Home Assistant broadcast.

## Validation and duplicate handling

- Only the declared broadcast action is processed.
- Weight must be between 20 and 300 kg.
- Missing or invalid timestamps use the current device time.
- A supplied `source` other than `homeassistant` is rejected.
- The last successfully written `timestamp|weight` combination is stored in
  SharedPreferences and ignored when received again.
- A failed write is not marked as a duplicate, so it can be retried.
- **Clear duplicate state** removes the saved last-written combination.

The exported receiver is intentionally callable by the Home Assistant Companion
app. Android broadcasts do not authenticate the sender in this setup, so action,
source, and plausibility checks reduce accidental input but are not a
cryptographic trust boundary.

## Troubleshooting

### Nothing arrives

- Verify that Home Assistant Companion notifications work on the device.
- Verify that `notify.mobile_app_DEIN_HANDY` is the correct notify action.
- Verify that `intent_package_name` is exactly
  `de.axelcypher.healthconnectbridge`.
- Verify that `intent_action` is exactly
  `de.axelcypher.healthconnectbridge.WRITE_WEIGHT`.
- Check whether notification commands are enabled in the Companion app.

### Weight is received but not written

- Grant Health Connect write access for weight.
- Open Health Connect and review HealthConnectBridge under app permissions.
- Verify that the weight is between 20 and 300 kg.
- Check the local log for an insert error.
- On Android 13 or older, install or update the Health Connect app.
- Health Connect does not support use from an Android work profile.

### Duplicate ignored

The same normalized weight and timestamp was already written successfully.
Use **Clear duplicate state** only when the same record should deliberately be
inserted again.

## Technical basis

- [Home Assistant Companion notification commands](https://companion.home-assistant.io/docs/notifications/notification-commands/)
- [Health Connect get started](https://developer.android.com/health-and-fitness/health-connect/get-started)
- [Health Connect write data](https://developer.android.com/health-and-fitness/health-connect/write-data)
- `androidx.health.connect:connect-client:1.1.0`
- Jetpack Compose with compile/target SDK 36
