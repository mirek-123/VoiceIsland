# 003 — Make the glass slider honour Reduce Motion

- **Status**: TODO
- **Commit**: no-git (project is not a git repository), 2026-11-17. Drift check: `GlassSlider.java` sha256 starts `bf8d370a0acef6cf` (run plan 002 first and expect the hash to change; the excerpts below are what matter).
- **Severity**: MEDIUM
- **Category**: Accessibility
- **Estimated scope**: 1 file (`app/src/main/java/com/mirecek/siriisland/GlassSlider.java`), ~12 changed lines

## Problem

The bubble honours Reduce Motion (`Prefs.reduceMotion(c)`: Android's animator duration scale is 0): jelly release uses `ζ 1` (`StrandsMotion.JELLY_ZETA_REDUCED`), pulses stop, and the clock runs at half speed (`IslandView.java:446,483,499`). `GlassSlider` ignores it. It drives its own springs, not `ValueAnimator`, so the system setting has no effect and the slider still wobbles:

- press spring `ζ 0.5` overshoots about 16 % (the code comment at line 144 says about 10 %, which is stale)
- release spring `ζ 0.32` overshoots about 35 %, so the capsule shrinks below its resting size then rebounds
- speed stretch (`stretch`) deforms the lens with drag speed

```java
// GlassSlider.java:140-146 — current
thumb.target = target;
thumb.step(dt, 55f, 0.9f);
stretch.target = Math.min(1f, Math.abs(thumb.v) / (900f * dp));
stretch.step(dt, 30f, 0.7f);
// Press: snappy with ~10 % overshoot; release: softer, so the capsule wobbles back.
if (press.target > 0.5f) press.step(dt, 32f, 0.5f);
else press.step(dt, 24f, 0.32f);
```

## Target

Reduced motion means fewer and gentler movements, not none. Keep the lens grow/shrink (it is the press feedback), but with a critically damped spring and no speed stretch. Reuse the existing reduced-motion damping constant so the slider matches the bubble.

```java
// GlassSlider.java:140-146 — target
thumb.target = target;
thumb.step(dt, 55f, 0.9f);
stretch.target = reduceMotion ? 0f : Math.min(1f, Math.abs(thumb.v) / (900f * dp));
stretch.step(dt, 30f, 0.7f);
if (reduceMotion) {
    // No overshoot: the lens grows and shrinks without wobbling.
    press.step(dt, press.target > 0.5f ? 32f : 24f, StrandsMotion.JELLY_ZETA_REDUCED);
} else if (press.target > 0.5f) {
    // Press: snappy with ~16 % overshoot; release: softer (~35 %), so the capsule wobbles back.
    press.step(dt, 32f, 0.5f);
} else {
    press.step(dt, 24f, 0.32f);
}
```

New field and wiring:

```java
// GlassSlider.java — add directly below `private long lastFrame;` (line 96)
/** Android "Remove animations" (animator duration scale 0): critically damped springs, no speed stretch. */
private boolean reduceMotion;
```

```java
// GlassSlider.java — add this method directly above `onTouchEvent` (after valueX())
@Override
protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    reduceMotion = Prefs.reduceMotion(getContext());
}
```

(`onAttachedToWindow` runs every time a settings page is shown, so the flag is re-read cheaply and never inside `onDraw`.)

## Repo conventions to follow

- `Prefs.reduceMotion(Context)` is the single source of truth; the bubble caches it at `applyLook` (`IslandView.java:195`) and never reads it per frame. Do the same.
- Spring damping for "no overshoot" is `StrandsMotion.JELLY_ZETA_REDUCED` (`StrandsMotion.java:11`, value `1f`). Reuse it instead of a literal.
- Exemplar: `IslandView.java:446` `float zeta = reduceMotion ? StrandsMotion.JELLY_ZETA_REDUCED : StrandsMotion.JELLY_ZETA;`.

## Steps

1. Add the `reduceMotion` field below `private long lastFrame;` (line 96).
2. Add the `onAttachedToWindow` override above `onTouchEvent`.
3. Replace lines 140-146 of `onDraw` with the target block above. Keep `thumb.step(dt, 55f, 0.9f)` unchanged; its overshoot is under 1 %.
4. If plan 002 has already been applied, `onTouchEvent` has changed; do not touch it here.

## Boundaries

- Do NOT touch `LENS`, the drawing code from line 148 on, `Ui.java`, or `Prefs.java`.
- Do NOT remove the lens grow/shrink in Reduce Motion; opacity and size feedback stay.
- Do NOT add dependencies.
- If the excerpts do not match (drift), STOP and report instead of improvising.

## Verification

- **Mechanical**: `gradle :app:assembleDebug :app:testDebugUnitTest` (no `gradlew` in the repo; use the local Gradle install). Expected: success.
- **Feel check** (device or emulator):
  - Normal settings: drag a slider and release. The lens still springs in with a small overshoot and wobbles back as before (nothing changed).
  - Developer options > "Animator duration scale" = off, then reopen the settings screen and the Glass page. Press and drag: the lens grows smoothly with no overshoot and no speed stretch. Release: it shrinks back to the capsule with no rebound below the resting size.
  - In both modes the thumb still follows the finger and the value text still updates.
  - Slow-motion check: with the scale set back to 10x, release in normal mode must still show the wobble (proof the normal path is intact).
- **Done when**: with the animator scale off the lens never overshoots or stretches, in normal mode it is unchanged, and the build passes.
