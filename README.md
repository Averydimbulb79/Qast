# Qast

### [Download the latest Qast Android APK](https://github.com/Averydimbulb79/Qast/releases/download/latest-alpha/Qast-debug.apk)

[All Qast releases](https://github.com/Averydimbulb79/Qast/releases) | [APK build status](https://github.com/Averydimbulb79/Qast/actions/workflows/android.yml)

The direct APK link becomes available after the first successful release-publishing workflow. Each successful build on `main` updates the same download URL.

A native Android media player with a cinematic interface. Planned features include Google Cast, DLNA/UPnP, Google Drive, OneDrive, Dropbox, WebDAV, SMB/NAS, and broad format playback.

## Current release: v0.1.2-alpha

This initial Android foundation includes:
- In-player vertical swipe gestures: left edge controls display brightness, right edge controls Android media volume
- Compact percentage HUD for both gestures, with upward swipes increasing and downward swipes decreasing levels
- Window-only brightness override restored when leaving the player; system-wide display settings are not modified
- Branded Qast logo in the main toolbar and adaptive Android launcher icon
- Original flat-color vector mark with circular Q, play triangle and casting waves under `assets/qast-brand.svg`
- Kotlin + Jetpack Compose dark UI with Home, Videos, Cloud and Settings navigation
- Working local file picker using Android Storage Access Framework
- Real playback using AndroidX Media3 ExoPlayer and its native playback controls
- In-session recently opened media list
- Explicit placeholders for cloud accounts and casting; neither is implemented yet
- Automated debug APK builds with GitHub Actions

This version uses Media3 only. LibVLC will be integrated in a later phase for expanded format support.

## Build maintenance

- 2026-10-23: Added a permanent direct APK download link to the README and automated publication of the latest successful alpha APK as a GitHub Release asset. Release publishing must be verified on GitHub Actions.


- 2026-10-23: v0.1.2 adds edge-zone brightness and volume gestures and a compact playback indicator. Build/device verification pending.


- 2026-10-23: Added approved Qast logo identity and stylised Q launcher icon. The logo is stored as scalable vector resources and is used in the Android app UI.

- 2026-10-22: CI fix for Android SDK setup: removed the obsolete `tools` package request, installed SDK platform 35 explicitly, and upgraded Java setup to v5. Build verification pending.

## Install the latest APK

**[Download Qast APK directly](https://github.com/Averydimbulb79/Qast/releases/download/latest-alpha/Qast-debug.apk)**

Download `Qast-debug.apk` on your Android device and open it to install. You may need to allow installation from your browser or file manager. Android 9 or newer is required.

This is an automatically generated **debug/alpha** build for testing, not a signed production release. The direct URL always points to the most recently published successful build on `main`. If the release has not yet been published, or publishing fails, use [GitHub Actions](https://github.com/Averydimbulb79/Qast/actions/workflows/android.yml) and download the `Qast-debug-apk` artifact from the latest successful run.

Keep this download link near the very top of the README and update the build instructions with every release.

## Build locally

Requires Android SDK 35, JDK 17, and Gradle 8.10.2.

```sh
gradle assembleDebug
```

The APK will be created at `app/build/outputs/apk/debug/app-debug.apk`.

## Planned milestones

| Version | Goal |
| --- | --- |
| v0.2 | Enhanced media library and cloud connector foundation, Google Drive |
| v0.3 | Google Cast and DLNA/UPnP |
| v0.4 | OneDrive, Dropbox, WebDAV/Nextcloud |
| v0.5 | SMB/NAS and downloads |
| v0.6 | Advanced subtitles, audio and gesture controls |
| v1.0 | Stable release and performance testing |

## Architecture

The UI is implemented with Compose; actual playback is currently handled by Media3. The platform architecture will be split into independent playback, cloud source and casting modules as those capabilities are implemented. Do not store OAuth secrets or access tokens in source control.

## Project links

- Repository: https://github.com/Averydimbulb79/Qast
- APK build history: https://github.com/Averydimbulb79/Qast/actions/workflows/android.yml

## Licensing

The project is in alpha. Review all dependencies and choose a project license before public binary distribution.
