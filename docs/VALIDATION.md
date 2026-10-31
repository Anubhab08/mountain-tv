# Validation

## Version 0.6.4 — 31 October 2026

The user approved Home-key access, and version 0.6.4 / code 13 was installed on the SWTV-20AE. The user tested the physical remote from an app and confirmed Mountain TV opens directly. Injected ADB Home events still enter the vendor system route, so injected-event timing is not claimed as evidence of the physical-remote result.

Native Home-role selection was tried reversibly; this firmware continued resolving Home to Google’s priority-2 launcher. The original Google Home role was restored. The helper now consumes short Home gestures before the stock launcher opens, with held Home routed to Google voice search on release. Cancelled, repeated and orphaned key sequences are covered by the added policy tests. The window-event fallback remains for firmware that does not deliver Home keys. No system-wide animation settings or launcher packages were changed.

`testDebugUnitTest lintDebug assembleDebug` passed: all 15 tests, zero lint errors and two existing warnings. APK signature verified. Matching Apple-style custom utility vectors were installed and visually checked on the TV. Wi-Fi opened NetworkActivity; Bluetooth opened native Remotes & Accessories; Search opened Google’s existing typing interface. The dot paused redirection and retained the original home after a Home press; reopening Mountain TV resumed the helper. No pairing or network changes were made. Current helper capabilities are 9; key events are not stored or transmitted. Home hold/voice, reboot and long-term vendor behavior were not freshly verified.

## Version 0.6.3

On 31 October 2026, `testDebugUnitTest lintDebug assembleDebug` passed: 11 tests, zero lint errors and two existing TV warnings. Signature verification passed. Version 0.6.3 / code 12 was installed on the SWTV-20AE. Remote navigation reached Bluetooth beside Wi-Fi; clicking it opened the native Remotes & Accessories side panel. No pairing or Bluetooth toggle was invoked. Back returned through Settings, and Home returned to Mountain TV. The one-shot lookup consumed its request and restored window-state-only events after selecting the menu. Content-retrieval capability 1 is used solely for this Settings lookup.

## Version 0.6.2

Packaging checks were rerun on 29 October 2026. `testDebugUnitTest lintDebug assembleDebug` passed: all 11 tests passed, zero lint errors and two intentional TV warnings (banner vector rasterization and fixed landscape). The supplied APK signature verifies. Private keys, API credentials, local toolchains, device diagnostics and build caches are excluded.

## Device checks recorded on 25–26 October 2026

Tested on SWTV-20AE, Android 10, at 1920 × 1080:

- Twelve installed app shortcuts rendered in two rows; requested welcome/section/footer text removed.
- D-pad movement between rows and launching Netflix verified.
- Wi-Fi opened the TV’s network settings without changing them.
- Search opened Google’s native typing interface. Submitting Panchayat opened Google’s title page with a Prime Video button; playback was not claimed as tested.
- Home returned to Mountain TV. The small dot paused the helper and retained Google’s original home; reopening Mountain TV resumed it. The original launcher remained enabled.
- Actual device captures verified wallpaper blur, sharp app icons and semicircular Search ends in focused and unfocused states.
- Reboot startup was verified on version 0.4.1 and is retained by the current helper; a new 0.6.2 reboot test is not claimed.

The blur is a cached, spatially aligned copy of the static wallpaper, composited at full opacity before its tint. In one unobstructed dock patch, mean adjacent-pixel luminance difference fell from 2.886 to 0.140 after the blur fix, while an outside wallpaper control patch remained pixel-identical. This is evidence of spatial smoothing, not a frame-rate benchmark.

Long-term vendor behaviour, other TV firmware, every physical remote, spoken recognition, frame pacing and every provider/account flow remain unverified. Google availability results vary by region and account.
