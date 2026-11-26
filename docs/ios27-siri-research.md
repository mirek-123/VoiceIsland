# iOS 27 / iPadOS 27 / macOS 27 Siri orb: appearance research

Purpose: guide a pixel-faithful Android re-creation (overlay around a camera cutout). Clean-room: appearance and measurements only; no Apple code, images, sounds or other assets are reused. Frames extracted for measurement live only in `C:/Users/Mirecek/AppData/Local/Temp/siribuild/research/` (not in the project).

Evidence tags used throughout:

- **OBSERVED** = seen in Apple's own marketing assets, or measured by me from a local clip (clip name + time given).
- **REPORTED** = secondary press / third-party statement (URL given).
- **INFERRED** = my reasoning from the above; no source states it.
- **OFFICIAL** = Apple documentation text (URL given).

Date of research: 2026-11-10. Apple's pages name the product "Siri AI" (iOS 27, iPadOS 27, macOS 27 "Golden Gate").

---

## 0. Bottom line (read this first)

1. **No Apple primary source gives numeric dimensions, colours or timings for the Siri orb.** The Siri HIG page was "Revised for Siri AI" on June 8 2026 but contains no orb visual spec; WWDC26 session 121 is a promo video. All numbers below are measured from footage.
2. The orb is **not a uniformly dark glass ball.** Measured on the MacStories release-build iPhone clip it is about **one third near-black (the upper ~40 %) and two thirds clear refractive glass (the lower ~60 %)**, with a **white-cored, thin, full-width, S-shaped chromatic ribbon** across the middle (centre at ≈55 % of orb height), a **bright rim on the lower half only**, and a faint **light spill below it**.
3. Apple's own brand artwork for the same orb (apple.com Apple Intelligence page, Siri AI hero video) shows a **very dark clear sphere (interior luminance ≈ 14-22 / 255) with a thin ribbon whose core is ≈ 8 % of the sphere diameter and has chromatic dispersion** (blue/cyan on one edge, orange/amber on the other).
4. Biggest corrections to the previously known numbers: measured orb width is **0.423 of screen width** (brief said ≈ 0.41, same ballpark), height/width **0.755** (brief: 0.74, ok), but the orb top is only **0.026 × W above the island top** (brief: 0.06) and sits **≈ 0.009 × W below the physical screen top**. The brief's "slight overshoot" in the appear animation was **not found** in the orb outline (monotone approach, then slow breathing).

---

## 1. Primary sources found and what each actually says

| Source | URL | Relevant content |
|---|---|---|
| Apple Newsroom, WWDC26 press release (Jun 8 2026) | https://www.apple.com/newsroom/2026/06/apple-unveils-next-generation-of-apple-intelligence-siri-ai-and-more/ | OFFICIAL: Siri AI "entirely new version of Siri", Liquid Glass personalisation slider "from ultra-clear to fully tinted", "app icons sharper". No orb description. |
| Apple Newsroom, "Siri AI ... is here" (Sep 14 2026) | https://www.apple.com/newsroom/2026/09/siri-ai-a-profoundly-more-capable-and-personal-assistant-is-here/ | OFFICIAL: invocation = "Hey Siri", side button, or "swipe down from the Dynamic Island"; "On iPad and Mac, Siri AI is integrated into Spotlight"; "On Apple Vision Pro ... a 3D visualization that users can place anywhere in their space". Hero video `https://www.apple.com/newsroom/videos/2026/autoplay/09/apple-siri-ai/large_2x.mp4` (1920×1080, 5.47 s) shows the brand orb (see §3). |
| apple.com iOS 27 page | https://www.apple.com/os/ios/ | OFFICIAL: "Updates to Liquid Glass ensure exceptional readability with more uniform refraction and improved contrast ... a new slider lets you ... customize how Liquid Glass looks, from ultraclear to fully tinted." |
| apple.com Apple Intelligence page | https://www.apple.com/apple-intelligence/ | OBSERVED: hero images `.../highlights_siri_startframe__f9hm5m83116q_large.jpg` and `.../highlights_siri_endframe__bd109g1f3l4y_large.jpg`; Apple's own alt text on the end frame: **"The wavy gradient of Siri Orb"**. |
| Apple HIG: Siri | https://developer.apple.com/design/human-interface-guidelines/siri | OFFICIAL: "people interact with Siri ... using their voice, swiping down from the Dynamic Island, or in the Siri app". Content/voice guidance only; no orb visuals. |
| Apple HIG: Materials | https://developer.apple.com/design/human-interface-guidelines/materials | OFFICIAL: Liquid Glass has *regular* (blurs and adjusts luminosity of background; darker over dark content, lighter over light) and *clear* variants ("highly translucent"); for clear glass over bright content "consider adding a dark dimming layer of 35 % opacity". Reduced Transparency / Increased Contrast change the look. |
| Apple HIG: Color | https://developer.apple.com/design/human-interface-guidelines/color | OFFICIAL: "By default, Liquid Glass has no inherent color, and instead takes on colors from the content directly behind it." Tint sparingly. Small elements flip light/dark with content; larger elements are "more opaque". |
| Apple HIG: Motion | https://developer.apple.com/design/human-interface-guidelines/motion | OFFICIAL: Liquid Glass movement responds to touch "with greater emphasis"; "aim for brevity and precision in feedback animations"; "make motion optional". No durations. |
| WWDC25 session 219 "Meet Liquid Glass" | https://developer.apple.com/videos/play/wwdc2025/219/ | OFFICIAL (transcript): "digital meta-material that dynamically bends and shapes light"; primary visual definition is **Lensing**; "Instead of fading, Liquid Glass objects materialize in and out by gradually modulating the light bending and lensing"; "Light sources ... produce highlights that respond to geometry ... light travel[s] around the material, defining its silhouette"; larger glass has "more pronounced lensing and refraction effects, and a softer scattering of light"; shadows adapt opacity to content behind; Reduced Transparency = "frostier", Increased Contrast = "predominantly black or white ... contrasting border", Reduced Motion = "decreases the intensity of some effects and disables any elastic properties". |
| WWDC26 session 121 | https://developer.apple.com/videos/play/wwdc2026/121/ | Promo only ("The Siri AI glow-up hits the small screen"). No design information. |
| Apple Support: Liquid Glass look setting | https://support.apple.com/en-euro/guide/iphone/iphd6804774e/27/ios/27 | OFFICIAL: Settings > Appearance > Liquid Glass slider (right = more tint, left = more clear); disabled while Reduced Transparency / Increased Contrast is on. |

Searched and not found: any WWDC26 session dedicated to the Siri orb / Siri visual design; any HIG page with orb dimensions, colours or timings; any Apple page describing the Mac orb (macOS puts Siri in Spotlight; Apple only describes the iPad/Mac behaviour as "integrated into Spotlight").

---

## 2. Secondary (REPORTED) descriptions

| Statement | Source |
|---|---|
| Old edge-glow is gone; "a floating dark orb made out of Liquid Glass that, on iPhone, smoothly descends from the Dynamic Island"; "pulses slightly to indicate it's listening"; "multicolor waveform (the only place where Siri kept its former colors) reacts in real-time"; "dynamically shrink into a 'thinking' activity with a loading indicator, then expand again into a dark, translucent overlay" | https://www.macstories.net/stories/ios-and-ipados-27-review/5/ |
| iPad: orb "'comes out' of the device's bezel like a drop of liquid that lands at the top of the screen and, since it's made of glass, beautifully refracts whatever is underneath"; can be triggered from anywhere in the status bar | same MacStories page |
| "glowing pill-shaped orb that expands directly from the Dynamic Island"; the pill shape "hides" the Dynamic Island cutouts; Apple uses a **circular** orb "in most places" (Siri app icon, promo artwork, iPad, Mac, Vision Pro); circle speculated for narrower iPhone 18 island | https://www.macrumors.com/2026/06/16/iphone-18-could-make-siri-a-circle/ |
| "a glassy new Siri bubble with bright colors" | https://www.macrumors.com/2026/06/10/ios-27-siri-ai-hands-on/ |
| Vision Pro: "a floating orb hovering around your field of vision" | https://www.theverge.com/tech/942416/apple-siri-ai-update-wwdc |
| Leaked assets: gradient colour scheme "most dominant hues being orange, pink, blue, and purple" (this describes a separate asset, not the shipped orb) | https://gadgets.beebom.com/news/ios-27-leak-shows-more-siri-ai-animations-apple-did-not-demo |
| Reduce Motion replaces the orb with "a static colored dot" (unverified, low-quality source) | https://iphonefilez.com/siri-dynamic-island-orb-iphone.html |
| Third-party WebGL recreation (design vocabulary only, not evidence of Apple's numbers): idle = "quiet white waveform inside glass", speaking = low/mid/high audio bands "ripple through it in color", thinking = dots, reply condenses from blur | https://www.z1han.com/works/siri27 |

---

## 3. Apple's own brand-orb artwork (OBSERVED, primary)

Files: `research/apple_orb_start.jpg` (1260×680), `research/apple_orb_hero.jpg` (1260×680), `research/apple_siri_ai_hero.mp4` (from Apple Newsroom URL above). These are the standalone "Siri orb" (icon/brand version), not the in-UI iPhone version, but they are Apple's clearest statement of the material and ribbon.

**apple_orb_start.jpg (glass sphere state, Apple apple-intelligence page hero start frame):**
- Sphere bbox x 485-776, y 239-525 → **diameter 291 × 286 px**, perfect circle, centred in a black field (bg luminance ≈ 14).
- **Interior is very dark and clear**: mean luminance by radius (ribbon band excluded) = 91 @0.20R, 59 @0.30R (glow around ribbon), 33 @0.50R, 24 @0.65R, **22 @0.75R (minimum)**, 26 @0.85R, 37 @0.90R, **65 @0.95R (rim)**, 14 outside. So the glass is ≈ 8-9 % luminance (22/255) at its darkest, brighter toward the rim; rim band ≈ 3-4 % of the radius thick.
- **Rim brightness is strongly non-uniform** (max luminance within r 0.9-1.04 R by angle; 0° = right, 90° = bottom, 270° = top): top (270°) 36; upper-left (225°) 86; left (180°) 214; lower-left (160°) 203, (135°) 132; bottom (90°) 136; lower-right (45°) 195; right-ish (20°) 249, (0°) 166; upper-right (340°) 52, (315°) 37. → the rim reads on the sides and lower half, nearly absent across the top.
- **Rim colour is chromatically dispersed**: left rim warm white `#d9d6cf`; lower-left mint-cyan `#8edecc`; below that blue `#548dbb`; bottom grey-teal `#7c8b8c`; lower-right warm yellow `#cec594`; right near-white `#f5fafb`.
- **Ribbon** (vertical profile at the centre column): peak 254-255 (clipped white) over rows 390-399; half-max thickness ≈ 24 px = **0.083 × sphere diameter**; falls to 100 at ≈ +15 px, 62 at ≈ +27 px, 40 at ≈ +48 px (0.165 D) from the core (long soft glow tail). Ribbon centre is **0.11 R below the sphere centre**. Horizontal extent above luminance 40: x 486-775 = **100 % of the sphere diameter**; above half-max: 487-768 (97 %).
- Ribbon colours (sample at peak ±8 px): core `#fefef1` / `#feffff` / `#fffffe`; above the core `#f2d2b7` (peach), `#968476`; below the core `#a9bfd7`, `#d3e9f6` (pale blue), `#8699ae`; far-left tail teal `#79b5b1`/`#68a8a0`.

**apple_orb_hero.jpg (ribbon-only end frame; Apple alt text "The wavy gradient of Siri Orb"):**
- Sphere has dissolved; what remains is an **S-shaped white ribbon with chromatic edges**: ribbon (>100 lum) bbox x 434-846, y 345-469; glow (>30 lum) bbox x 338-927, y 275-535; peak 255; background 0.2; vertical glow halo above ribbon ≈ lum 40 at 80 px above.
- Colour clusters (k-means, lit pixels): white-cream `#f7f2e6` 17 %, warm tan `#ab9d93`/`#dacaba` 24 %, grey `#787777`/`#5b5c5e` 48 % (glow falloff), sky blue `#a3e1f8` 5 %, steel blue `#7698c0` 5 %. Visually: blue/cyan fringe on the upper-left edge, orange/amber fringe on the lower edge, white core line.

**apple_siri_ai_hero.mp4 (Apple Newsroom hero, 30 fps, 5.47 s):** starts on the small glass sphere with its ribbon, then zooms through the sphere into a giant ribbon whose shape keeps morphing (S-curve, lobes) for the remaining ~4 s; ribbon always white-hot at the core with blue (upper) and orange (lower) fringes.

---

## 4. Measurements from the local clips

### 4.1 Clips and provenance

| Clip | Format | Provenance |
|---|---|---|
| `siribuild/ref_iphone.mp4` | 1350×2760, 634 frames, VFR (≈56.1 fps, ts 0-11.28 s), no audio | Per brief: MacStories release-build iPhone clip, https://cdn.macstories.net/images/uploads/2026/09/12/iphone-siri-orb-framed-1789217330629-0b5b7053b1.mp4 (embedded at https://www.macstories.net/stories/ios-and-ipados-27-review/5/). Phone frame included: **screen spans x 72-1277 (1205 px) and starts at y = 69**; all "W" below = 1205 px. |
| `siribuild/ref_ipad.mp4` | 3000×2300, 1224 frames (≈54.4 fps, 22.5 s) | Per brief: MacStories iPad clip https://cdn.macstories.net/images/uploads/2026/09/12/siri-orb-ipad-framed-1789217270098-f5cce35df0.mp4. iPad screen top at y = 118; screen width ≈ 2760 px (left edge approximate). |
| `siribuild/ref_cook.mp4` | 1120×1000, 30 fps, 5.0 s | **Provenance not recorded in the brief**; lower-resolution, over a bright lock screen. Treated as secondary evidence only; where it disagrees with ref_iphone, ref_iphone wins. |

Method: frames decoded losslessly with ffmpeg (`-fps_mode passthrough`, rgb24), true per-frame timestamps from ffprobe; luminance = Rec.709 weights on 8-bit sRGB values. Reference "no orb" frames: ref_iphone t=0.10 s and t=10.6 s. "v" below = vertical position inside the orb, 0 = top, 1 = bottom (orb top y=80, bottom y=465 in ref_iphone at t=1.5 s).

### 4.2 iPhone orb geometry (ref_iphone, listening state)

(Grid-checked overlay on frame t=1.5 s.)

| Quantity | px | × screen width W (1205) | Evidence |
|---|---|---|---|
| Orb width | x 420 → 930 = **510** | **0.423** | ref_iphone t=1.5 s grid read; row-profile rim detect |
| Orb height | y 80 → 465 = **385** | 0.320 | same; bottom rim tracked per frame |
| Height / width | | **0.755** | same |
| Orb centre x | 675 | 0.500 (screen centre 674.5) | |
| Widest row | y ≈ 270 → v = 0.49 | | |
| Orb top below screen top | 80 − 69 = **11** | 0.009 | screen top found at y=69 (t=10.6 s luminance step) |
| Orb top above island top | island top y=111 → **31** | 0.026 | island bbox lum<6 at t=0.05 / 10.6 s: x 408-922, y 111-220 (514×109 px incl. screen-recording pill) |
| Orb bottom below screen top | 465 − 69 = 396 | 0.329 | |
| Island width in clip | 514 | 0.427 | = orb width (island here is the wide variant with the red recording dot) |
| Outline shape | | superellipse, exponent **n ≈ 2.4** (n=2.8 overlay was slightly too square at top corners); top flatter and wider, lower half rounder, slightly drop-shaped | ref_iphone t=1.5 s superellipse overlay |

Cross-check (ref_cook, secondary): orb x 355-768 on a screen ≈ 1050 px wide → 0.39 W; height 275 px → height/width 0.67.

### 4.3 iPad orb geometry (ref_ipad, t=2.4 s)

- Orb is a **circle**: x 1390-1610 (**220 px**), y 180-400 (220 px) → diameter ≈ **0.080 × screen width** (≈ 2760 px).
- Centre x = 1500 (screen centre); centre y ≈ 290 → 172 px (0.062 W) below the screen top (y=118); top of orb is 62 px (0.022 W) below the screen top.
- Content under the lower half is strongly refracted (two lobes of the tiles beneath, magnified and split by the ribbon flare).
- Ribbon is short: ≈ 0.55 of orb diameter wide, white core with blue (left) and orange (right) fringes.
- Soft shadow/glow extends ≈ 57 px below the orb (frame-difference bbox bottom 457 vs visual bottom 400).

### 4.4 Glass: how dark vs see-through (ref_iphone)

**Brightness profile down the orb centre line (x 665-685, luminance 0-255):**

| v | t=1.0 s | t=1.5 s | t=2.4 s | note |
|---|---|---|---|---|
| 0.025 | 13 | 17 | 17 | above island top: wallpaper (≈ 30) already darkened |
| 0.075 | 7 | 8 | 8 | |
| 0.125-0.325 | 0-4 | 0-4 | 0-4 | island hardware black behind the glass |
| 0.375 | 10 | 16 | 15 | |
| 0.425 | 25 | 90 | 67 | ribbon glow starts |
| 0.475 | 51 | 213 | 198 | ribbon |
| 0.525 | 98 | 242 | 237 | ribbon core (peak 254) |
| 0.575 | 218 | 230 | 194 | |
| 0.625 | 193 | 192 | 160 | |
| 0.675 | 102 | 113 | 96 | glow tail |
| 0.725-0.975 | 77, 71, 75, 82, 97, 122 | 96, 99, 111, 125, 135, 158 | 85, 90, 103, 117, 134, 134 | refracted wallpaper/icons: rises toward the bottom rim |

**Transmission of the glass over real content (orb luminance ÷ same pixel with no orb, columns x=470 and x=880, rows below the island):**
- v = 0.375: T ≈ 0.46-0.48; v = 0.425: T ≈ 0.43-0.52; v = 0.475: T ≈ 0.62-0.75; v ≥ 0.525: T ≈ 1.0 (ribbon glow adds light).
- Lower half, away from the rim (x=880): T ≈ 0.93-0.95 (v 0.875-0.975). Near the rim over the icons (x=470): T ≈ 0.86, 0.75, 0.72 (v 0.875, 0.925, 0.975): the glass darkens ≈ 15-28 % toward the rim.
- Whole lower region (below island, bg > 25): at t=1.5 s 82.9 % of the orb pixels have T ≥ 0.75, 13.7 % have 0.35 ≤ T < 0.75, 3.4 % have T < 0.35 (t=1.0 s: 72 / 21 / 7 %; t=2.4 s: 81 / 15 / 4 %).
- **Dark cap**: pixels with luminance < 12 cover **32.1-34.9 % of the orb area**; by 20 %-band of height: 0-20 %: 88-96 % black; 20-40 %: 82-87 %; 40-60 %: 1-4 %; 60-100 %: 0 %.
- Cap colour: RGB ≈ (9,6,9)-(10,10,11) (near black, slight bleed of the purple wallpaper) → with wallpaper ≈ (60,35,65) behind this corresponds to ≈ 85 % black overlay (INFERRED).
- At the very top (centre column x 665-685, y 80-120, wallpaper ≈ 30 behind; island hardware starts at y=111): luminance 26.6, 16.6, 12.0, 9.2, 1.0, 5.0 at y = 80, 88, 96, 104, 112, 120 → transmission ≈ 0.9, 0.55, 0.4, 0.3 at v = 0.00, 0.02, 0.04, 0.06, i.e. overlay opacity ramps from ≈ 0.1 at the top edge to ≈ 0.7 by v ≈ 0.06 (≈ 23 px).
- ref_cook (secondary, bright lock screen, bg lum 100-160): cap luminance 0-3 for v ≈ 0.05-0.42 → overlay ≈ 97-98 %; at v = 0.42 lum 5; ribbon region from v = 0.49; lower half transparent with strong lens flip ("Wed Apr 1" lock-screen text appears mirrored/inverted inside the bottom rim). So the cap is **darker over bright content than over dark** content in the two clips (α ≈ 0.85 over purple wallpaper vs ≈ 0.97 over bright tan).

**Refraction / blur:**
- OBSERVED (ref_iphone t=1.5 s, ref_cook f75, ref_ipad t=2.4 s): the lower 35-40 % of the orb refracts content like a lens: icons beneath appear magnified, displaced upward into the glass and bent along the rim; text near the lower rim appears **flipped/mirrored** (ref_cook). Content is **not blurred**; it is warped and slightly darkened. No frosted-glass blur was observed anywhere in the lower half.
- The upper cap is a dark smoky gradient, not a clear lens.
- **Light spill below the orb**: bg luminance under the orb with vs without orb (columns x 560-790, t=1.5 s): +37 at y=470 (89 → 126), +22 at 490 (101 → 123), +8 at 510 and 530, ≈ +3 at 550-600, ≈ 0 by y ≥ 650; i.e. ≈ +40 % within ≈ 25 px under the rim decaying to ≈ 0 by ≈ 100 px (0.083 W) (INFERRED: bounce light / caustic from the bright lower rim).

**Rim / edge highlight (ref_iphone t=1.5 s, brightest pixel within ±16 px of the outline, outline = superellipse n=2.4; 0° right, 90° bottom, 180° left, 270° top):**

| Angle | rim peak luminance | local interior |
|---|---|---|
| 0° (right) | 109 | 25 |
| 30° | 155 | 70 |
| 60° | 159 | 88 |
| 90° (bottom) | **246** | 110 |
| 120° | 190 | 58 |
| 150° | 203 | 124 |
| 180° (left) | 124 | 39 |
| 210°-330° (whole top arc) | 24-36 | 11-35 → **no distinct rim** |

Rim width: bright core ≈ 3-4 px (≈ 0.007 × orb width) with ≈ 10-12 px inward falloff (row y=330, left edge: 33 → 120 peak → 75, 53, 45). Rim has coloured fringes on the lower-left/lower-right (see §3 for Apple's brand version).

### 4.5 Ribbon / waveform (ref_iphone, listening)

Measurement box x 430-925, y 205-345. Thickness = vertical extent where luminance > max(60, 0.5 × peak) in centre columns x 655-700.

| Time | peak lum | thickness px | thickness / orb width | thickness / orb height | centre y (v) | width px (lum>90) |
|---|---|---|---|---|---|---|
| 0.62 s | 72 | 20 | 0.04 | 0.05 | 293 (0.55) | 436 |
| 0.71 s | 204 | 42 | 0.08 | 0.11 | 296 | 469 |
| 0.82 s | 252 | 41 | 0.08 | 0.11 | 299 | 481 |
| 1.02 s | 254 | 42 | 0.08 | 0.11 | 302 (0.58) | 488 |
| 1.21 s | 253 | 100 | 0.20 | 0.26 | 292 (0.55) | 494 |
| 1.52 s | 251 | 85 | 0.17 | 0.22 | 288 (0.54) | 494 |
| 2.01 s | 254 | 91 | 0.18 | 0.24 | 293 | 494 |
| 2.51 s | 237 | 55 | 0.11 | 0.14 | 284 (0.53) | 486 |
| 3.01 s | 254 | 56 | 0.11 | 0.15 | 301 (0.57) | 485 |
| 3.42 s | 254 | 46 | 0.09 | 0.12 | 301 | 494 |

- Over 0.9-3.5 s thickness min/max/mean = **41 / 102 / 73.7 px**; dominant modulation frequencies 1.54 Hz and 1.92 Hz (plus a slow 0.38 Hz drift); vertical centre drifts 284-302 px (± 9 px).
- Peak luminance is clipped white (**≈ 254**) from 0.82 s onward; width is **≈ 97 % of the orb width** (rim to rim), 436 px at first appearance growing to 494 px by 1.1 s.
- Ribbon fade-in: peak 72 (0.62 s) → 204 (0.71 s) → 252 (0.82 s) ≈ 0.20 s.
- **Silence/idle behaviour (INFERRED, clip has no audio track):** the thinnest state (≈ 41-46 px) occurs just after appearing (0.7-1.0 s, before speech) and again after the voice stops (3.1-3.5 s). So in silence the ribbon is a thin full-width white line-glow ≈ 0.08 × orb width thick, still peak-white, not removed.
- Third-party corroboration of "quiet white waveform at idle": https://www.z1han.com/works/siri27 (REPORTED, low weight).

**Ribbon colour composition** (pixels with V > 0.45 in the box; hue bins on pixels with S ≥ 0.35):

| t | white (S<0.25, V>0.85) % of lit px | coloured % of lit px | red | orange | yellow | green | cyan | blue | violet | magenta |
|---|---|---|---|---|---|---|---|---|---|---|
| 1.0 | 22.0 | 19.0 | 19.9 | 51.9 | 3.5 | 0 | 0 | 18.4 | 0.9 | 5.2 |
| 1.5 | 19.0 | 33.7 | 8.5 | 25.7 | 9.2 | 3.0 | 22.5 | 27.7 | 0 | 3.5 |
| 2.0 | 30.9 | 34.8 | 13.3 | 23.5 | 10.1 | 3.6 | 21.8 | 25.8 | 0 | 1.9 |
| 2.4 | 20.3 | 27.0 | 12.1 | 33.4 | 11.0 | 3.6 | 18.9 | 16.9 | 0.1 | 4.1 |
| 3.2 | 27.4 | 14.8 | 29.3 | 31.1 | 6.4 | 0 | 1.8 | 30.8 | 0 | 0.6 |

(values in the colour columns are % of the *coloured* pixels.) k-means palette (7 clusters, t=1.5-2.4 s): white-cream `#ecf0ef`/`#f8faf9`/`#e3edeb` 16-27 %; cyan `#9eedf5`/`#9ee3ed`/`#a2e1f4` 13-15 %; pale blue `#8aa1d5`/`#8ba4c0`/`#99a2c2` 11-13 %; warm tan/amber `#b69b89`/`#c19177`/`#bf9b77` 9-14 %; brown-orange `#996753`/`#8e645a`/`#8b655a` 11-17 %; pale yellow `#d7d2b1`/`#eadf98`/`#cdcab0` 7-11 %; grey-violet glow `#747085`/`#6d728a`/`#7f7786` 17-22 %.

Spatial layout at t=1.5 s (column samples, ribbon crest): x 480-540 (left tail) amber `#b27c34`/`#a89469`, curved down (peak y 340-349); x 600 cyan-white core `#c4f5fb` with amber below `#a87b48`; x 660-720 white core `#f2faf8`/`#fdfdfc` (peak y 278-287); x 780 cyan `#79e6e6` with red-pink lower edge `#a44156`; x 840 (right tail) pink `#b96d81`/crimson `#ae2c43`, curved down (peak y 340). So the ribbon is an S-shaped band: warm amber at the left tail, blue/violet then cyan on the upper-left flank, **white core centre-right and above**, yellow at ≈ x 740, red/orange at the right tail, with the tails sagging downward. MacStories confirms this is "the only place where Siri kept its former colors" (https://www.macstories.net/stories/ios-and-ipados-27-review/5/).

### 4.6 State timeline (ref_iphone unless stated)

All times are clip time in seconds; the Siri invocation is at ≈ 0.4 s.

| State | Start → end | Evidence / numbers |
|---|---|---|
| Idle island | 0 → 0.41 | island 514×109 px, red recording dot |
| **Appear** | 0.42 → ≈ 0.80 (shape ≈ 97 % by 1.0 s) | 0.463 s: island lower lip starts to bulge; 0.48-0.56: island morphs into an opaque black blob (no glass, no ribbon) growing downward; **0.57-0.64: glass reveals** (icons visibly refracted at the bottom, ribbon first visible at 0.60-0.62, peak 72); 0.71 ribbon peak 204; 0.82 ribbon full. Consistent with Apple: "Liquid Glass objects materialize ... by gradually modulating the light bending and lensing" (WWDC25 219). No fade-in of opacity. |
| Appear: bottom-edge y | 391 @0.57, 403 @0.59, 413 @0.61, 421 @0.62, 432 @0.64, 436 @0.66, 440 @0.68, 442 @0.70, 445 @0.71, 448 @0.75, 451 @0.78, 453 @0.82, 456 @0.89, 458 @1.0, 461 @1.11, 463 @1.19, 465 @1.41-1.5 | progress (island bottom 224 → 465): 69 % @0.57, 86 % @0.64, 93 % @0.75, 97 % @1.0; ≈ exponential with τ ≈ 0.09-0.11 s, slow tail. **No overshoot.** Orb top edge y 82 → 78 over 0.6 → 1.5 s (4 px). |
| **Listening** | ≈ 0.8 → 3.55 | orb bottom edge "breathes" 465 (1.45 s) → 450 (2.6-2.7 s) → 464 (3.4-3.5 s): ≈ 15 px peak-to-peak (3.9 % of height, 1.2 % of W), cycle ≈ 2.0 s (INFERRED: voice-driven); ribbon thickness 41-102 px as §4.5. |
| **Collapse to thinking** | 3.57 → 3.87 (≈ 0.30 s) | 3.58: orb still full; **3.62: ribbon has collapsed into a single bright white dot**, orb outline deflating (bottom 461 @3.60 → 368 @3.71 → 326 @3.80); orb flattens into the wide black pill: pill width 429 @3.55, 399 @3.58, 548 @3.65, 617 @3.69, 679 @3.73, 706 @3.76, 722 @3.80, 758 @3.87, **762 final** (x 294-1055), height y 108-258 (**150 px**, 0.124 W; top at 0.032 W below screen top). Pill width reaches 95 % at ≈ 3.80 (≈ 0.15-0.2 s, ease-out). |
| Dot → spinner | 3.62 → 3.90 | dot centroid (x,y): (664,285) @3.62, (629,282) @3.65, (581,271) @3.69, (532,259) @3.73, (491,243) @3.76, (460,232) @3.80, (437,223) @3.83, (420,220) @3.87, (407,220) @3.94, (397,221) @4.01 → glides left and settles at the spinner centre; blobs break out of it at ≈ 3.87 (frame 3.868 shows a bunch of blobs) and become a ring by 3.92. |
| **Thinking** | 3.87 → 6.70 (2.8 s; network-dependent) | spinner of **6-7 pure-white dots**, ring radius **44 px** (0.037 W), centre (390, 205) → 318 px from the screen's left edge (0.264 W), dot diameters **12-26 px** (0.010-0.022 W) tapering around the ring (comet-tail style, largest 26 px, smallest 12 px), soft glow; **rotates clockwise, period 0.96-0.97 s** (angular rate 6.46 rad/s, fit residual 0.15 rad; autocorrelation peak 0.96 s). Pill unchanged (762×150) all the while. Background pill is black with a faint thin light edge line. |
| **Response** | spinner gone 6.70; panel grows 6.66 → 6.92 (≈ 0.26 s); text visible 6.93-7.0 | pill expands downward into a dark translucent glass panel (mean luminance of top region 28 → 12 between 6.65 and 6.86 s) whose lower area refracts the icons behind it (ghosted icon row visible at 6.78-6.90 s, blurry/darkened), then the app snippet card (Weather) fades in at ≈ 7.0. Panel is static for ≈ 2.2 s. |
| **Dismiss** | 9.13 → 9.23 (0.10 s) | mean absolute diff to idle frame: 58.1 (constant) → 56.3 @9.13 → 19.5 @9.18 → 6.8 @9.23 → 4.4 @9.34. Island/status bar then re-appear (ghosted at 9.4, normal by ≈ 9.6-9.7). |
| iPad **appear** (ref_ipad) | 1.14 → 1.38 main drop, 1.38 → 1.9 settle | no orb at 1.14; at 1.20 it is emerging from the screen top (bbox top 118 = screen top, bottom 328); bottom edge (diff bbox) 328 @1.20 → 409 @1.26 → 431 @1.32 → 443 @1.38 → 451 @1.56 → 457 @1.92-2.04, monotone, no bounce. |
| iPad **dismiss** | 3.50 → 3.62 (≈ 0.12 s) | bottom 440 @3.50 → 321 @3.55 → 184 @3.60 → gone at 3.65: the orb retreats **upward** into the bezel. |

---

## 5. Answers by question

**(a) Shape, size, placement**
- iPhone: a rounded **superellipse "bubble" (n ≈ 2.4)**, 0.423 × screen width wide and 0.755 × as tall, horizontally centred, top flush with the screen top (11 px = 0.009 W below it; 0.026 W above the island top) and 0.329 W tall below the screen top at the bottom. The orb grows out of the Dynamic Island (OBSERVED ref_iphone 0.46-0.64 s; REPORTED MacRumors "pill-shaped orb that expands directly from the Dynamic Island", used to hide the cutout). An earlier/other build (ref_cook) shows ≈ 0.39 W wide, height/width 0.67 (lower confidence).
- iPad: a **circle** ≈ 0.080 × screen width in diameter, centred, whose top is 0.022 W below the screen top, that drops out of the top bezel (REPORTED MacStories; OBSERVED ref_ipad 1.14-1.9 s) and retreats upward when dismissed.
- Mac: no Apple description of an orb UI. Apple says Siri AI on Mac is "integrated into Spotlight" and via context menus (OFFICIAL Sep 14 newsroom); the circular orb is the Siri **app icon** and promo mark (REPORTED MacRumors Jun 16). Vision Pro: floating 3D orb (REPORTED The Verge; OFFICIAL "3D visualization" in newsroom).

**(b) Glass / material** - see §4.4. Summary: upper ≈ 40 % is a near-opaque dark cap (≈ 85-97 % black) that merges with the island; the lower ≈ 60 % is **clear, un-blurred, strongly lensing glass** (T ≈ 0.75-0.95, darker toward the rim) that magnifies/flips what is beneath it; a bright **bottom-weighted rim** (peak luminance 246 at the bottom, ≈ 110-200 at the sides, none across the top); **light spill** below it; chromatic dispersion on the rim and ribbon. This matches Apple's wording for Liquid Glass: lensing as the primary visual cue; highlights that "respond to geometry ... defining its silhouette"; objects "materialize" by modulating lensing instead of fading (WWDC25 219); and "takes on colors from the content directly behind it" (HIG Color). iOS 27 adds "more uniform refraction and improved contrast" and the clear↔tinted slider (apple.com/os/ios).

**(c) Waveform / ribbon** - see §4.5 and §3. White-hot (clipped 254-255) S-shaped ribbon, full orb width, centre at ≈ 55 % of orb height, thickness 0.08 × orb width (quiet) to 0.20 × (speaking), chromatic: blue/cyan flank (upper-left), amber/orange/red tail (right/lower), yellow joint; ≈ 20-30 % of lit ribbon pixels are white, ≈ 15-35 % are saturated colour; the modulation is fast (≈ 1.5-1.9 Hz components).

**(d) States and transitions** - see §4.6 table.

**(e) Official colour / material guidance** - the only Apple-official statements are the Liquid Glass ones in §1 (HIG Materials/Color, WWDC25 219, Support slider). Practical consequences: (1) glass has no colour of its own; (2) the ribbon is the only coloured element (MacStories); (3) regular glass adapts light/dark with the content behind; (4) clear glass over bright content wants a dark dimming layer (Apple's number: **35 % black**); (5) Reduced Transparency (frostier), Increased Contrast (black/white + border) and Reduced Motion (reduced intensity, no elastic effects) modify the material; the Liquid Glass slider is disabled when Reduced Transparency or Increased Contrast is on. Apple publishes **no RGB values** for the orb.

---

## 6. Spec for re-creation (numbers for a renderer)

Conventions: `Wo` = orb width, `Ho` = orb height = 0.755 Wo; `W` = screen width; `v` = 0 at orb top, 1 at orb bottom; luminance 0-255; angles 0° = right, 90° = bottom, 270° = top. Tags: [P] = primary footage measurement (ref_iphone / ref_ipad), [S] = secondary clip (ref_cook), [A] = Apple marketing artwork, [O] = Apple official text, [R] = REPORTED press, [I] = inferred / tuning value, not measured.

### 6.1 Geometry (iPhone-style orb)

| Parameter | Target | Evidence |
|---|---|---|
| Wo | **0.42 W** (acceptable range 0.39-0.43) | [P] ref_iphone t=1.5 s: 510/1205; [S] ref_cook 0.39 |
| Ho | **0.755 Wo** (0.67 in ref_cook) | [P] 385/510 |
| Centre x | screen centre | [P] 675 vs 674.5 |
| Top edge | **0.009 W below the physical top of the screen**; i.e. top flush with the screen edge. On Android with a cutout, anchor the orb top at the screen top and let the cutout sit at v ≈ 0.08-0.36 inside the dark cap (island top at v = 0.08 in the iPhone clip: (111-80)/385) | [P] t=1.5 s |
| Cutout/island inside orb | island black spans v 0.08-0.36, width ≈ 1.0 Wo in this clip (wide island) | [P] |
| Outline | superellipse \|x/a\|^2.4 + \|y/b\|^2.4 = 1 with a = Wo/2, b = Ho/2, centre at v = 0.49; keep the lower half a bit rounder (slight drop shape) | [P] overlay |
| Breathing (listening) | bottom edge moves ±0.0195 Ho (±7 px of 385), full cycle ≈ 2.0 s | [P] 465/450/464 px at 1.45/2.65/3.45 s; [I] voice-driven |
| iPad/large-screen orb | circle, **0.080 × screen width** diameter, centre 0.062 W below top | [P] ref_ipad t=2.4 s |

### 6.2 Glass / opacity profile (dark overlay alpha as a function of v; composite black `rgb(8,6,9)` over the live backdrop)

| v | alpha (black overlay) | Evidence |
|---|---|---|
| 0.00 | 0.10 | [P] centre col y=80: lum 26.6 vs bg ≈ 30 |
| 0.02 | 0.45 | [P] y=88: 16.6/30 |
| 0.04 | 0.60 | [P] y=96: 12/30 |
| 0.06 | 0.70 | [P] y=104: 9/30 |
| 0.08 | 0.85 | [P] island hardware begins (y=111); cap lum 6-9 |
| 0.10 – 0.33 | **0.85** over dark/medium content; raise to **0.95-0.97** when the backdrop luminance is > 100 | [P] cap lum 6-9 over wallpaper; [S] cap lum 0-3 over bright lock screen; [I] |
| 0.375 | 0.53 | [P] T = 0.46-0.48 |
| 0.425 | 0.50 | [P] T = 0.43-0.52 |
| 0.475 | 0.30 | [P] T = 0.62-0.75 |
| 0.525 | 0.00 (ribbon light added on top) | [P] T ≈ 1.0 |
| 0.55 – 0.85 | 0.00-0.05 (clear lens) | [P] x=880: T = 0.93-0.95 |
| 0.875 – 1.0 | 0.05 → 0.25 toward the bottom rim (edge darkening) | [P] x=470: T = 0.86, 0.75, 0.72 |

Cap area ≈ 33 % of the orb area; lower 60 % is see-through [P]. Cap should not look glossy/blurred: it is a smooth dark gradient.

Lens (lower region, v ≥ 0.5): **no blur**; sample the backdrop through a lens displacement field that is zero at v = 0.5 and grows toward the rim, with a mirrored/flipped band at the very bottom edge (text appears inverted there). **Observed:** icons beneath the lower half are displaced upward into the glass and bent along the rim [P] ref_iphone t=1.5 s; "Wed Apr 1" lock-screen text appears mirrored inside the bottom rim [S] ref_cook; Apple: "more pronounced lensing and refraction effects" on larger glass [O WWDC25 219]. **Not measured [I]:** displacement magnitude (start tuning at ≈ 0.15-0.20 Ho at the rim) and flip-band height (start at ≈ 6-8 % of Ho). (If a real-time lens is impractical, use a precomputed displacement map; do **not** substitute blur.)

If you render the standalone brand orb instead (Apple artwork), the glass interior is ≈ 22/255 luminance at 0.75 R, 33 at 0.5 R, 65 at the rim [A].

### 6.3 Rim and light spill

| Parameter | Target | Evidence |
|---|---|---|
| Rim core width | 0.007 Wo (≈ 3.5 px at Wo=510) bright core + 0.02 Wo inward falloff | [P] row y=330 left edge |
| Rim peak luminance by angle (0° right) | 0°: 110; 30°: 155; 60°: 160; **90°: 245**; 120°: 190; 150°: 200; 180°: 125; **top arc 210°-330°: ≤ 35 (no rim)** | [P] §4.4 table |
| Rim hue | mostly neutral white `#f5fafb` / `#d9d6cf`; dispersion fringes: lower-left mint-cyan `#8edecc` → blue `#548dbb`, lower-right warm yellow `#cec594` | [A] apple_orb_start.jpg angular samples |
| Light spill under the orb | additive white-lavender glow below the bottom rim: +40 % of local backdrop luminance at 0-25 px (0.05 Wo), linear fade to 0 by 100 px (0.20 Wo) | [P] t=1.5 s columns x 560-790 |

### 6.4 Ribbon

| Parameter | Target | Evidence |
|---|---|---|
| Vertical centre | **v = 0.55** (range 0.53-0.58) | [P] cy 284-302 px |
| Horizontal extent | **0.97 Wo** at full size (starts 0.86 Wo at first appearance) | [P] 494 px / 436 px |
| Core thickness (half-max) | quiet **0.08 Wo** (41 px); speaking mean **0.145 Wo** (74 px); loud **0.20 Wo** (102 px) | [P] 41/73.7/102 px |
| Modulation | thickness follows the voice envelope; typical components 1.5-1.9 Hz plus slow drift; centre y jitter ±0.018 Wo (±9 px) | [P] FFT |
| Core colour | **#FFFFFF**, luminance 254 (clipped), soft glow falling to 100 lum at ≈ 0.05 Wo and to 40 lum at ≈ 0.165 Wo from the core (above/below) | [P] peak 254; [A] profile 254 → 100 @ 15 px → 62 @ 27 px → 40 @ 48 px of a 291 px sphere (scale by sphere diameter 291 → Wo) |
| Colour stops along x (left → right), over the white core | 0.00-0.10 Wo amber `#B27C34`; 0.10-0.30 violet-blue `#8AA1D5`; 0.30-0.45 cyan `#9EE3ED` (upper edge); 0.40-0.60 white `#F8FAF9`; 0.62-0.72 yellow `#EADF98`; 0.72-0.88 red-orange `#C19177` → `#AE2C43`; 0.88-1.00 pink-red `#B96D81`; upper edges bias blue/cyan, lower edges bias orange/amber (chromatic dispersion) | [P] k-means + column samples t=1.5-2.4 s; [A] apple_orb_hero.jpg |
| Composition (lit pixels) | white ≈ 22-27 %, coloured ≈ 15-35 % (of coloured: orange 24-34 %, blue 17-28 %, cyan 19-22 %, red 8-13 %, yellow 9-11 %, green 3 %, magenta 2-5 %, violet 0 %) | [P] §4.5 |
| Shape | S-shaped, tails sagging down 0.10-0.15 Ho, centre lobe slightly above the tails | [P] peak y 278-287 centre, 340-349 tails (t=1.5 s) |
| Fade-in | 0.60 → 0.82 s after invocation (peak 72 → 204 → 252) | [P] |
| Blend | additive (screen) over the glass | [I] |

### 6.5 Timings (t measured from clip time; invocation ≈ 0.42 s ⇒ subtract 0.42 for relative times)

| Phase | Start (rel. to invocation) | Duration | Curve / numbers | Evidence |
|---|---|---|---|---|
| Island → black blob | 0.00 | 0.14 s | opaque black, no glass, grows from island | [P] 0.46-0.56 s |
| Glass reveal (not a fade) | 0.14 | 0.25 s | reveal refraction + ribbon; ribbon alpha 0.1 → 0.8 → 1.0 | [P] 0.60-0.82 s; [O] WWDC25 219 |
| Orb height progress (clip-time offsets measured from 0.46 s, not from the invocation time) | +0.11 s: 69 %; +0.18 s: 86 %; +0.29 s: 93 %; +0.54 s: 97 %; ≈ +1.0 s: 100 % | ≈ 1.0 s settle | `1-exp(-t/0.1)` with slow tail; **no overshoot** | [P] bottom-rim track |
| Listening | 0.4 | until user stops | breathing ±0.0195 Ho / 2.0 s; ribbon per §6.4 | [P] |
| Collapse: ribbon → dot | 3.15 | 0.05 s | ribbon shrinks to a white dot ≈ 0.09 Wo (46 px, lum>215 area at 3.62 s) that shrinks to ≈ 0.06 Wo (30 px) while it glides | [P] 3.58-3.65 s |
| Orb → pill | 3.20 | 0.25 s | pill width 0.63 W (762/1205), height 0.124 W (150 px), top at 0.032 W below screen top; width 548 @+0.03, 679 @+0.11, 722 @+0.18, 758 @+0.25 | [P] §4.6 |
| Dot glide | 3.20 | 0.40 s | dot travels from orb centre to spinner centre (0.264 W from screen left, y = 0.113 W below screen top) while turning into blobs at +0.25 s | [P] |
| Thinking spinner | 3.45 | until response (2.8 s here) | **6-7 white dots**, ring radius 0.037 W (44 px), dot diameter 0.010-0.022 W tapering 26 → 12 px, **clockwise**, **period 0.97 s** | [P] ref_iphone 4.1-6.5 s |
| Response expand | 6.24 | 0.26 s | pill → dark translucent glass panel, content fades in ≈ +0.30 s | [P] 6.66-7.0 s |
| Response hold | 6.5 | as long as needed | static | [P] |
| Dismiss | 8.71 | **0.10 s** (fast fade/collapse), then island/status bar return over ≈ 0.4 s | | [P] 9.13-9.23 s |
| iPad drop | n/a | 0.24 s main + ≈ 0.5 s settle; retreat 0.12 s upward | bottom 328 → 443 px in 0.18 s | [P] ref_ipad |

### 6.6 Thinking pill and response panel

| Parameter | Target | Evidence |
|---|---|---|
| Pill size | 0.632 W × 0.124 W (762 × 150 px at W = 1205), x-centred, top 0.032 W below screen top, fully rounded ends | [P] t=4.5 / 5.5 s lum<8 bbox: x 294/296-1054/1055, y 108-258 |
| Pill fill | black `#000`; a faint thin light edge line is visible in frames (not measured) | [P] visual only |
| Response panel | dark translucent glass (≈ 85-90 % black [I]), area below the cutout shows backdrop refracted/darkened/ghosted during the 0.26 s expansion; then content cards | [P] 6.66-7.0 s frames; [R] MacStories "dark, translucent overlay" |

### 6.7 Accessibility variants (design intent, cite before shipping)

- Reduced Motion: reduce intensity, no elastic/ripple effects (Apple: https://developer.apple.com/videos/play/wwdc2025/219/); a static coloured dot has been reported but is unverified (https://iphonefilez.com/siri-dynamic-island-orb-iphone.html).
- Reduced Transparency: glass "frostier" (WWDC25 219) → raise the lower-half alpha from ≈ 0.05 to ≈ 0.5-0.6 (value is [I]).
- Increased Contrast: predominantly black/white with a contrasting border (WWDC25 219).

### 6.8 What is still uncertain

- Exact RGB values of the orb were **not published by Apple**; all colours here are sampled from compressed (H.264/JPEG) footage and will carry ± a few levels of error.
- ref_cook.mp4 provenance unknown; used only as a cross-check. Differences vs the MacStories clip (orb ratio 0.67 vs 0.755, cap darker over bright content) may reflect a different build, device or the Liquid Glass adaptive behaviour.
- The clip has no audio, so the silence behaviour of the ribbon is INFERRED from its thinnest observed state; verify against a recording with a known silent segment if available.
- Orb outline measured by eye on a gridded frame (±4 px); superellipse exponent is a best fit.
- Mac orb appearance: no primary footage found.

---

## 7. Local artefacts produced (not in the project)

`C:/Users/Mirecek/AppData/Local/Temp/siribuild/research/`: `apple_orb_start.jpg`, `apple_orb_hero.jpg`, `apple_siri_ai_hero.mp4`, `hero_tile.png`, `iph_tile.png`, `iph_sheet.png`, `iph_orb_1.5.png`, `iph_orb_2.4.png`, `grid15.png`, `ov1.png`, `appear_sheet.png`, `think_sheet.png`, `resp_sheet.png`, `cook_tile.png`, `cook_f30_mid.png`, `ipad_tile.png`, `ipad_sheet.png`, `ipad_2.4_crop.png`, and per-time frame PNGs `iph_t*.png`.
