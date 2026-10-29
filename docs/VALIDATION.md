# Validation

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
