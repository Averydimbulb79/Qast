# Qast

A native Android media player with a cinematic interface. Planned features include Google Cast, DLNA/UPnP, Google Drive, OneDrive, Dropbox, WebDAV, SMB/NAS, and broad format playback.

## Current release: v0.1.0-alpha

This initial Android foundation includes:
- Kotlin + Jetpack Compose dark UI with Home, Videos, Cloud and Settings navigation
- Working local file picker using Android Storage Access Framework
- Real playback using AndroidX Media3 ExoPlayer and its native playback controls
- In-session recently opened media list
- Explicit placeholders for cloud accounts and casting; neither is implemented yet
- Automated debug APK builds with GitHub Actions

This version uses Media3 only. LibVLC will be integrated in a later phase for expanded format support.

## Build maintenance

- 2026-10-22: CI fix for Android SDK setup: removed the obsolete `tools` package request, installed SDK platform 35 explicitly, and upgraded Java setup to v5. Build verification pending.

## Install a test APK

Open **Actions** → **Android APK** → select a successful run → **Artifacts** → download **Qast-v0.1.0-alpha-debug**. Extract the ZIP and install `app-debug.apk` on an Android 9+ device. This is an unsigned-for-release debug build, not a Play Store package.

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
