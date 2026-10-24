# Tuner 🎸

A clean, fast guitar tuner for Android, written in Kotlin with Jetpack Compose.

- **Automatic string detection** – just pluck; the app works out which string you're playing.
- **String lock** – tap a string to tune against it only (handy when you're far out of tune).
- **8 tunings** – Standard, Drop D, Half/Full step down, Drop C, Open G, Open D, DADGAD.
- **Needle gauge** with ±50 cent range; green within ±5 cents.
- **A4 calibration** from 415–466 Hz (tap the value to reset to 440).
- Works fully offline. Audio is analysed on the device and never recorded or sent anywhere.

Pitch detection uses the [YIN algorithm](http://audition.ens.fr/adc/pdf/2002_JASA_YIN.pdf)
on a 4096-sample window at 44.1 kHz, updated ~43 times a second, with median smoothing.

## Install

Every push builds an APK in GitHub Actions:

1. Open the repo's **Actions** tab → latest **Build APK** run → download the `tuner-apk` artifact.
2. Unzip it, copy `app-release.apk` to your phone and open it
   (allow "Install unknown apps" for your file manager/browser when asked).

Requires Android 8.0 (API 26) or newer.

## Build locally

With Android Studio, open the project and press Run. From the command line (needs the Android SDK):

```sh
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest    # pitch-detection unit tests
```

The release build is signed with the debug key so it installs directly; set up your own
signing config before publishing to the Play Store.
