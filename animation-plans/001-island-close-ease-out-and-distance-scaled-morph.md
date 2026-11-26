# 001 — Make the bubble's close morph ease-out and scale morph duration by remaining distance

- **Status**: TODO
- **Commit**: no-git (project is not a git repository), 2026-11-17. Drift check: `IslandView.java` sha256 starts `8a9090cd2de90f3b`.
- **Severity**: MEDIUM
- **Category**: Easing & duration (+ Interruptibility)
- **Estimated scope**: 1 file (`app/src/main/java/com/mirecek/siriisland/IslandView.java`), ~6 changed lines

## Problem

The bubble opens a few to tens of times a day, and every open ends in a close. Two things in the morph are off:

1. **The close morph uses an ease-in curve.** It starts slowly, right when the user is watching the bubble leave. AUDIT rule: exits use ease-out; `ease-in` on UI is always a finding.

```java
// IslandView.java:59-60 — current
private static final PathInterpolator OPEN_CURVE = new PathInterpolator(0.2f, 0f, 0f, 1f);
private static final PathInterpolator CLOSE_CURVE = new PathInterpolator(0.4f, 0f, 1f, 1f);
```

2. **The morph duration ignores the distance left to travel.** `animateMorph` always runs the full `ms` from the *current* progress. When the service flickers (Xiao Ai closes and reopens, so `close()` then `open()` land mid-morph), a morph that has only 0.3 left to go replays the whole 160 ms or 350 ms. With the ease-in close curve this shows as a slow crawl. The interrupted tween also starts again from zero speed.

```java
// IslandView.java:318-326 — current
private void animateMorph(float to, long ms, android.animation.TimeInterpolator interp, Runnable end) {
    cancel(morphAnim);
    morphAnim = ValueAnimator.ofFloat(progress, to);
    morphAnim.setDuration(ms);
    morphAnim.setInterpolator(interp);
    morphAnim.addUpdateListener(a -> {
        progress = (float) a.getAnimatedValue();
        invalidate();
    });
```

Settled decisions you must NOT change (spec `docs/superpowers/specs/2026-11-17-strands-bubble-design.md` §2): the open morph has no bounce; `OPEN_CURVE` stays; `MORPH_OPEN_MS` 350 and `MORPH_CLOSE_MS` 160 stay; the close order stays "strands fade 200 ms, then the morph back"; the Strands intro still starts at morph progress 0.6.

## Target

```java
// IslandView.java:59-60 — target
private static final PathInterpolator OPEN_CURVE = new PathInterpolator(0.2f, 0f, 0f, 1f);
/** Exit: strong ease-out, so the bubble leaves fast and settles into the camera hole. */
private static final PathInterpolator CLOSE_CURVE = new PathInterpolator(0.23f, 1f, 0.32f, 1f);
```

```java
// IslandView.java:318-326 — target
private void animateMorph(float to, long ms, android.animation.TimeInterpolator interp, Runnable end) {
    cancel(morphAnim);
    // The duration follows the distance still to travel, so reversing mid-morph never replays the full time.
    long scaledMs = Math.max(1L, Math.round(ms * Math.abs(to - progress)));
    morphAnim = ValueAnimator.ofFloat(progress, to);
    morphAnim.setDuration(scaledMs);
    morphAnim.setInterpolator(interp);
    morphAnim.addUpdateListener(a -> {
        progress = (float) a.getAnimatedValue();
        invalidate();
    });
```

Everything below line 326 in `animateMorph` (the `end` listener and `morphAnim.start()`) stays as is. Full-range morphs (0 to 1 and 1 to 0) keep exactly 350 ms and 160 ms.

## Repo conventions to follow

- Motion constants live next to their use. `IslandView` has the two `PathInterpolator` constants and `StrandsMotion` has the durations. Do not add a new class.
- Exemplar of a distance-aware duration already in this file: `close()` scales the strand fade by how visible the strands are, `Math.round(full * intro)` (`IslandView.java:265`). Mirror that style.
- Durations pass through `ValueAnimator`, so Android's `animator_duration_scale` (Reduce Motion = 0) still makes the morph instant. Do not add any `reduceMotion` branch here.

## Steps

1. In `IslandView.java`, replace line 60 with the two-line version above (a one-line javadoc, then `CLOSE_CURVE = new PathInterpolator(0.23f, 1f, 0.32f, 1f)`). Leave line 59 untouched.
2. In `IslandView.java`, in `animateMorph` (line 318), insert the `scaledMs` line after `cancel(morphAnim);` and change `morphAnim.setDuration(ms);` to `morphAnim.setDuration(scaledMs);`.
3. Do not edit any other line.

## Boundaries

- Do NOT touch `StrandsMotion.java`, `OPEN_CURVE`, `MORPH_*_MS`, `INTRO_*`, `close()` or `open()`.
- Do NOT replace the tween with a spring in this plan. That is a possible follow-up (see README, "Not planned").
- Do NOT add dependencies or new files.
- If lines 59-60 or 318-326 do not match the excerpts above (drift since the stamp), STOP and report instead of improvising.

## Verification

- **Mechanical**: from the project root run the project's usual Gradle build, for example `gradle :app:assembleDebug :app:testDebugUnitTest`. There is no `gradlew` in the repo, so use the local Gradle install. Expected: build succeeds and the existing unit tests (`StrandsMotionTest`, `GlassProfileTest`, `ThemesTest`, `FrameBudgetTest`) pass. None of them touch `IslandView`.
- **Feel check** (needs the device or emulator, with Developer options "Window animation scale" and "Animator duration scale" set to 10x to slow it down):
  - Open and close the bubble through Xiao Ai or "Preview on screen". The close should leave fast and ease into the camera hole, not start slowly and accelerate.
  - Start a close, then trigger an open halfway through the shrink. The reopen should take a proportionally shorter time, not a full 350 ms.
  - Start an open and close it before the intro finishes. The shrink is short and smooth, with no pause.
  - Set Animator duration scale to "off" (Reduce Motion): open and close still show only the 150 ms intro fade and no morph.
  - Compare against the spec's "no bounce" rule: there must be no visible overshoot at the end of the open.
- **Done when**: `CLOSE_CURVE` is `(0.23, 1, 0.32, 1)`, `animateMorph` sets `scaledMs`, the build passes, and the feel check above shows no pause or crawl on close.
