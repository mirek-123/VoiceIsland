# 002 — Pop the slider's glass lens only when a real drag starts

- **Status**: TODO
- **Commit**: no-git (project is not a git repository), 2026-11-17. Drift check: `GlassSlider.java` sha256 starts `bf8d370a0acef6cf`.
- **Severity**: MEDIUM
- **Category**: Purpose & frequency / Physicality (press feedback that fires on the wrong gesture)
- **Estimated scope**: 1 file (`app/src/main/java/com/mirecek/siriisland/GlassSlider.java`), ~6 changed lines

## Problem

Every settings page is a vertical `ScrollView` (`Ui.scroll`) with `GlassSlider` rows in it (`Ui.sliderRow`). `GlassSlider` springs the thumb into the big 67x33 dp glass lens on **ACTION_DOWN**. A finger that lands on a slider only to scroll the page therefore makes the lens pop up, then collapse again on ACTION_CANCEL. That is wasted, distracting motion on a gesture that is not a slider drag.

The framework `SeekBar` already handles this: inside a scrolling container it waits until the finger moves past the touch slop before it calls `setPressed(true)`. `GlassSlider` ignores that and drives its own state from the raw touch action.

```java
// GlassSlider.java:117-127 — current
@Override
public boolean onTouchEvent(MotionEvent e) {
    boolean handled = super.onTouchEvent(e);
    switch (e.getActionMasked()) {
        case MotionEvent.ACTION_DOWN -> press.target = 1f;
        case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> press.target = 0f;
        default -> { }
    }
    postInvalidateOnAnimation();
    return handled;
}
```

## Target

`press.target` follows `isPressed()`, which `SeekBar` sets when a drag really starts and clears when it ends or is cancelled.

```java
// GlassSlider.java:117-127 — target
@Override
public boolean onTouchEvent(MotionEvent e) {
    boolean handled = super.onTouchEvent(e);
    // SeekBar presses itself at once outside a scrolling container, and after the touch slop inside one,
    // so scrolling the page past a slider no longer pops the lens.
    press.target = isPressed() ? 1f : 0f;
    postInvalidateOnAnimation();
    return handled;
}
```

All spring constants in `onDraw` (press 32/0.5 in, 24/0.32 out) stay as they are. This plan only changes when the target flips.

Known trade-off to accept: a quick tap on the track (finger up before the slop) now moves the thumb with its normal spring but shows no lens. A dpad or keyboard centre press can also set pressed and show the lens; that is fine.

## Repo conventions to follow

- Gesture state in this class lives in the three `Spring` fields (`press`, `thumb`, `stretch`) and `postInvalidateOnAnimation()` is how a redraw is requested. Keep both.
- `onDraw` already keeps redrawing until `press.settled(...)` (`GlassSlider.java:192`), so no extra invalidate is needed.

## Steps

1. In `GlassSlider.java`, replace lines 117-127 (the whole `onTouchEvent`) with the target version above.
2. Do not edit any other line. In particular leave the imports alone; `MotionEvent` is still used by the signature.

## Boundaries

- Do NOT touch `onDraw`, the spring constants, `LENS`, or `Ui.sliderRow`.
- Do NOT add a delayed press (`postDelayed`) or touch-slop code of your own; `SeekBar` owns that.
- Do NOT add dependencies.
- If lines 117-127 do not match the excerpt above (drift since the stamp), STOP and report instead of improvising.

## Verification

- **Mechanical**: `gradle :app:assembleDebug :app:testDebugUnitTest` (no `gradlew` in the repo; use the local Gradle install). Expected: success.
- **Feel check** (device or emulator; set "Animator duration scale" to 10x if you want to watch the springs in slow motion):
  - Open Settings > Glass (or Look). Put a finger on a slider's track and scroll the page vertically. The thumb must stay the white capsule; the lens must never appear.
  - Press the thumb and drag sideways. The lens must grow once the finger has moved a few millimetres, and stretch with drag speed, as before.
  - Release. The lens wobbles back into the capsule as before.
  - Drag the slider, then without lifting scroll vertically so the parent takes the touch. The lens must collapse (ACTION_CANCEL path) and not stay stuck open.
  - Tap once on the track. The thumb glides to the tap position; no lens flash.
  - Turn TalkBack on and adjust the slider with swipe up/down. Values still change.
- **Done when**: `press.target` is set only from `isPressed()`, the build passes, and vertical scrolling across a slider never shows the lens.
