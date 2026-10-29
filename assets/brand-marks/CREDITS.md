# Brand marks

These identify the user's streaming apps. The four SVGs retain the authentic Simple Icons path geometry, with a white display fill for colored TV cards. Android `brand_*.xml` drawables use the same paths and 24 × 24 viewport. No brand name is rendered with a substitute font.

| App | Shared preview asset | Source and provenance | Source brand color |
| --- | --- | --- | --- |
| YouTube | `youtube.svg` | [Simple Icons SVG](https://github.com/simple-icons/simple-icons/blob/develop/icons/youtube.svg); [YouTube brand resources](https://www.youtube.com/howyoutubeworks/resources/brand-resources/#logos-icons-and-colors) | `#FF0000` |
| YouTube Music | `youtubemusic.svg` | [Simple Icons SVG](https://github.com/simple-icons/simple-icons/blob/develop/icons/youtubemusic.svg); [Google partner brand resources](https://partnermarketinghub.withgoogle.com/#/brands) | `#FF0000` |
| Prime Video | `prime.svg` | [Simple Icons 14.0.0 SVG](https://github.com/simple-icons/simple-icons/blob/14.0.0/icons/primevideo.svg); [Amazon brand guidelines cited by that release](https://videodirect.amazon.com/home/help?topicId=GT7W7GJBTDJW6Z8W#G8T2JFQZXPMHJLRZ) | `#1F2E3E` |
| Apple TV | `appletv.svg` | [Simple Icons SVG](https://github.com/simple-icons/simple-icons/blob/develop/icons/appletv.svg); [underlying logo source cited by Simple Icons](https://en.wikipedia.org/wiki/File:Apple_TV_(logo).svg) | `#000000` |
| Sony LIV | `sonyliv.png` | Unchanged publisher artwork from its [Google Play listing](https://play.google.com/store/apps/details?id=com.sonyliv&hl=en), copied from `../preview-logos/sonyliv.png` | Original multicolor artwork |
| JioHotstar | `hotstar.png` | Unchanged publisher artwork from its [Google Play listing](https://play.google.com/store/apps/details?id=in.startv.hotstar&hl=en), copied from `../preview-logos/hotstar.png` | Original blue/pink gradient and star |

Source color/provenance metadata for the four vectors comes from [Simple Icons 14.0.0 metadata](https://github.com/simple-icons/simple-icons/blob/14.0.0/_data/simple-icons.json); all vector display fills here are white. Prime Video was fetched from that tagged release because its SVG is absent from the current development path. No verified standalone vector was obtained for Sony LIV or JioHotstar, so their original publisher PNGs are retained instead of redrawing their artwork.

`youtube-play.svg` and Android `brand_youtube_play.xml` isolate the play triangle from the YouTube SVG's original cutout subpath (`M9.545 15.568V8.432L15.818 12l-6.273 3.568z`). It is uniformly scaled and translated into a 24 × 24 viewport, preserving its proportions, and filled white for the red card. The full original mark remains in `youtube.svg`.

Simple Icons artwork is distributed under [CC0 1.0](https://github.com/simple-icons/simple-icons/blob/develop/LICENSE.md), copied locally as `LICENSE-SIMPLE-ICONS-CC0.txt`. Brand trademarks and publisher app artwork remain their owners' property; the app-icon PNGs are not represented as CC0. Exact source URLs and native resource names are in `manifest.json`.
