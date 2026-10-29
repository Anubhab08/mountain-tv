# Asset sources

Downloaded 25 October 2026. All assets are bundled locally, so the launcher and preview do not fetch them during use.

## Google Sans

`GoogleSans.ttf` is the original variable source from the official Google Fonts repository. `GoogleSans-Regular.ttf` and `GoogleSans-Medium.ttf` are static instances generated with fontTools 4.60.1 at weights 400/500, optical size 18 and grade 0. Both the preview and Android app use these same static instances for consistent rendering on Android 10:

- Source: https://github.com/google/fonts/tree/main/ofl/googlesans
- Binary: https://raw.githubusercontent.com/google/fonts/main/ofl/googlesans/GoogleSans%5BGRAD,opsz,wght%5D.ttf
- Copyright 2025 The Google Sans Project Authors (https://github.com/googlefonts/googlesans)
- License: SIL Open Font License 1.1, included in `FONT-LICENSE-OFL.txt`.

## Mountain photograph

`mountains.jpg` is an Unsplash mountain landscape, resized by Unsplash's image server to 2560 pixels wide. The preview adds a dark CSS overlay without changing the source image.

- Source image: https://images.unsplash.com/photo-1464822759023-fed622ff2c3b
- Download: https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=2560&q=90
- License: https://unsplash.com/license

The Unsplash License permits downloading, copying, modifying, distributing, and using photographs for commercial and noncommercial purposes. The image cannot be sold without significant modification or collected to replicate a competing image service. No photographer name is asserted because it could not be independently confirmed from the source page.

## App icons — browser design preview only

Unmodified icon images from the app publishers' official Google Play listings, downloaded for identifying the apps in this private local preview. These icons and names remain the property and trademarks of their respective owners; no ownership or endorsement is claimed, and no general redistribution license is asserted. These square icons were used by the retired browser search panel and are retained only as source assets. The home dock and native launcher now use bundled brand marks described below; installed packages are still detected through Android PackageManager.

- YouTube: https://play.google.com/store/apps/details?id=com.google.android.youtube.tv
- Prime Video: https://play.google.com/store/apps/details?id=com.amazon.amazonvideo.livingroom
- Sony LIV: https://play.google.com/store/apps/details?id=com.sonyliv
- YouTube Music: https://play.google.com/store/apps/details?id=com.google.android.apps.youtube.music
- JioHotstar: https://play.google.com/store/apps/details?id=in.startv.hotstar
- Apple TV: https://play.google.com/store/apps/details?id=com.apple.atve.androidtv.appletv

The exact downloaded image URLs are preserved in `preview-logos/sources.json`.

## Interface graphics

The small mountain, search, Wi-Fi, navigation, check, and close symbols are original inline SVGs created for this project. No third-party icon package is used.

## Home dock brand marks

See `brand-marks/CREDITS.md` and its source manifest for the SVG marks and publisher Sony LIV/JioHotstar artwork. Native Prime Video and Apple TV marks are rasterized from the same white SVG paths at 384 pixels wide to avoid long-vector rendering on the TV. JioHotstar uses the original star silhouette in white over its brand gradient. All app trademarks remain with their owners.

## Continuous corner geometry

60% corner smoothing is adapted from [figma-squircle](https://github.com/phamfoo/figma-squircle/blob/main/src/draw.ts), copyright 2021 Tien Pham, under the MIT license included in `FIGMA-SQUIRCLE-LICENSE.txt`. Both native and browser implementations cache geometry on size changes.

## Additional installed app banners

See `installed-banners/CREDITS.md`. Netflix, ZEE5, Spotify, Google Play Movies, Live TV and Play Store artwork comes from the installed publishers’ app resources.
