# Qast

### [Download the latest Qast Android APK](https://github.com/Averydimbulb79/Qast/releases/download/latest-alpha/Qast-debug.apk)

[All Qast releases](https://github.com/Averydimbulb79/Qast/releases) | [APK build status](https://github.com/Averydimbulb79/Qast/actions/workflows/android.yml)

The direct APK link becomes available after the first successful release-publishing workflow. Each successful build on `main` updates the same download URL.

A native Android media player with a cinematic interface. Planned features include Google Cast, DLNA/UPnP, Google Drive, OneDrive, Dropbox, WebDAV, SMB/NAS, and broad format playback.

## Current release: v0.1.5-alpha

This initial Android foundation includes:
- Playback regression fix: local files open without initialising Cast or querying remote sessions; Cast initialises only when requested
- Playback failure banner showing Media3 error codes when a video cannot decode or open
- AirPlay scanning restricted to the Settings page instead of continuing during playback
- Proper system status-bar clearance on the Home and Player screens
- Approved Qast wordmark used directly as vector outlines in the header, without substitute fonts, manually rebuilt letters, or a white square
- Adaptive launcher icon with the approved Q and cyan play/cast artwork on navy, inset to respect Android icon masks
- Initial AirPlay receiver discovery via Android NSD/mDNS (`_airplay._tcp`), showing discovered device names in Settings
- AirPlay pairing and media sending are NOT implemented yet; discovered devices cannot yet be selected for AirPlay playback
- Google Cast SDK integration: Cast receiver picker, session lifecycle, automatic media handoff, stop control
- Local video served over a temporary random-token LAN HTTP URL with byte-range support for seeking
- Fixed right-edge media-volume gesture using a continuous accumulator instead of losing fractional movement
- Casting status displayed in the player; compatible video codecs and same-Wi-Fi connection required
- In-player vertical swipe gestures: left edge controls display brightness, right edge controls Android media volume
- Compact percentage HUD for both gestures, with upward swipes increasing and downward swipes decreasing levels
- Window-only brightness override restored when leaving the player; system-wide display settings are not modified
- Branded Qast logo in the main toolbar and adaptive Android launcher icon
- Approved-logo silhouettes traced from the final master image into `qast_approved_wordmark.xml` and `qast_approved_iconmark.xml`
- Kotlin + Jetpack Compose dark UI with Home, Videos, Cloud and Settings navigation
- Working local file picker using Android Storage Access Framework
- Real playback using AndroidX Media3 ExoPlayer and its native playback controls
- In-session recently opened media list
- Cloud accounts and DLNA support are planned; Google Cast support is alpha and requires device testing
- Automated debug APK builds with GitHub Actions

This version uses Media3 only. LibVLC will be integrated in a later phase for expanded format support.

## Build maintenance

- 2026-10-23: v0.1.5 stabilises local video playback, delays Cast initialisation, corrects status-bar insets and installs approved branding. Build/device validation pending.


- 2026-10-23: v0.1.4 adds read-only AirPlay device discovery. AirPlay streaming requires a sender protocol implementation and compatibility testing. The reference AirSonic project is noncommercial-licensed and has NOT been incorporated.


- 2026-10-23: v0.1.3 implements the Google Cast sender, temporary local HTTP streaming, and fixes the volume gesture. Build and hardware casting remain to be verified. A Cast TV must share the phone's Wi-Fi. The receiver must support the source codec; there is no transcoding. DLNA discovery and control follow next.


- 2026-10-23: Added a permanent direct APK download link to the README and automated publication of the latest successful alpha APK as a GitHub Release asset. Release publishing must be verified on GitHub Actions.


- 2026-10-23: v0.1.2 adds edge-zone brightness and volume gestures and a compact playback indicator. Build/device verification pending.


- 2026-10-23: Added approved Qast logo identity and stylised Q launcher icon. The logo is stored as scalable vector resources and is used in the Android app UI.

- 2026-10-22: CI fix for Android SDK setup: removed the obsolete `tools` package request, installed SDK platform 35 explicitly, and upgraded Java setup to v5. Build verification pending.

## Required one-time signing setup for in-place app updates

Android will not update an installed app unless both APKs have the same application ID and signing certificate, and the new APK has a higher `versionCode`. GitHub's default debug key changes across clean CI runners. Qast now requires a permanent signing key stored in repository **Actions secrets**, never in source control.

Generate a keystore locally with Java 17's `keytool`:

```sh
keytool -genkeypair -v -keystore qast-signing.jks -alias qast -keyalg RSA -keysize 3072 -validity 10000 -storepass YOUR_PRIVATE_PASSWORD -keypass YOUR_PRIVATE_PASSWORD -dname "CN=Qast, OU=Development, O=Qast"
```

Keep that `.jks` file and password backed up privately. Convert the file to a single-line Base64 string locally, e.g. PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("qast-signing.jks")) | Set-Clipboard
```

In **GitHub repository → Settings → Secrets and variables → Actions**, create repository secrets `QAST_KEYSTORE_B64` (the Base64 contents) and `QAST_KEYSTORE_PASSWORD` (the password). Rerun the Android APK workflow. CI deliberately fails its main-branch signing step until both are configured, so an unstable-signature APK cannot replace the permanent release asset.

**One unavoidable reset:** existing builds were signed with temporary debug keys. The first build signed by the new permanent keystore requires uninstalling the old Qast. After that, later releases signed with this same key and incrementing `versionCode` can update in place. Do not lose or replace this keystore.

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
| v0.3 | Harden Google Cast support and add DLNA/UPnP receiver discovery and controls |
| v0.4 | OneDrive, Dropbox, WebDAV/Nextcloud |
| v0.5 | SMB/NAS and downloads |
| v0.6 | Advanced subtitles, audio and gesture controls |
| v1.0 | Stable release and performance testing |

## Architecture

The UI is implemented with Compose; local playback uses Media3. Google Cast uses the official Google Cast sender SDK and a temporary, random-token LAN HTTP media endpoint. This is not screen mirroring. Casting stops if the Android process is terminated. Network compatibility and media decoding depend on the receiver. The platform architecture will be split into independent playback, cloud source and casting modules as those capabilities are implemented. Do not store OAuth secrets or access tokens in source control.

## Project links

- Repository: https://github.com/Averydimbulb79/Qast
- APK build history: https://github.com/Averydimbulb79/Qast/actions/workflows/android.yml

## Licensing

The project is in alpha. Review all dependencies and choose a project license before public binary distribution.
