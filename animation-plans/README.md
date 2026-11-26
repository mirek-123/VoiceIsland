# Siri Island animation plans

Stamp: no-git (project is not a git repository), 2026-11-17. Source drift is detected with the sha256 prefixes inside each plan.

| # | Plan | Severity | Status |
| --- | --- | --- | --- |
| 001 | [Close morph ease-out and distance-scaled morph duration](001-island-close-ease-out-and-distance-scaled-morph.md) | MEDIUM | TODO |
| 002 | [Slider lens only on a real drag](002-slider-lens-only-on-real-drag.md) | MEDIUM | TODO |
| 003 | [Slider honours Reduce Motion](003-slider-reduce-motion.md) | MEDIUM | TODO |

## Order and dependencies

1. 001 first: it is the highest-frequency surface (every open/close) and touches only `IslandView.java`.
2. 002 then 003: both edit `GlassSlider.java` (002 changes `onTouchEvent`, 003 changes `onDraw` and adds a field). The hunks do not overlap, but run them one after the other so line numbers do not drift.
3. 001 is independent of 002/003.

## Not planned (see the audit table for details)

- PreviewHeader collapse animates `LayoutParams.height` (`PreviewHeader.java:95-102`): profile with `adb shell dumpsys gfxinfo com.mirecek.siriisland framestats` while pushing a sub-page before deciding on a fix.
- Press feedback for `Ui.Segments` and `Ui.Cards` (`Ui.java:316-340`, `394-418`): add a `StateListAnimator` with pressed `scaleX/scaleY` 0.97 over 160 ms `cubic-bezier(0.23, 1, 0.32, 1)`.
- `SettingsNav.pop()` snaps the revealed page (`SettingsNav.java:81-82`): only set the start position when the page is `INVISIBLE`.
- Spring-driven morph per spec §2 (critically damped, response 0.35 s) to carry velocity through interruptions; larger change in `IslandView.onDraw` that must also keep Reduce Motion instant.
- Shared motion tokens (`PathInterpolator(0.32, 0.72, 0, 1)` and 350 ms are typed in both `SettingsNav.java:28-29` and `PreviewHeader.java:98-99`).
