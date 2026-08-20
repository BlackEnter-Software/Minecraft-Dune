# Minecraft: Dune 0.5.12 — Fault Floor Consistency

0.5.12 is a focused correction to the native fault-depth model.

## Problem

In 0.5.11, fault depth was applied proportionally to the original rock height:

```text
new height = lerp(original height, low fault floor, fault mask * 0.96)
```

Even a nominally full fault retained 4% of the original massif height. On a ~200-block wall
that residual alone could leave several additional blocks, and slightly sub-core masks could
leave much larger shelves. The sandy-floor selection was also thresholded twice.

This made fault depth vary too strongly with the height of the rock it crossed.

## Changes

- Fault cores now target an **absolute structural floor height**.
- `core_width` now literally represents the half-width of the full-depth core when the radial
  fault gate is fully active.
- Removed the old `0.96` residual-depth multiplier.
- Added `faults.rocky_floor_height`.
  - default: `4.0`
  - fully rocky core target: approximately Y68
  - fully sandy core target: Y64
- Sandy-floor noise now chooses the floor state independently from the carve depth.
- Removed the previous second sand-floor threshold stage.
- Strong sand-floor segments snap to a true sand target; strong rocky segments snap to the
  configured rocky target.
- At fault intersections, the floor metadata now follows the fault that supplies the dominant
  carve mask at that X/Z column instead of mixing maxima from unrelated faults.
- Fault centerline geometry, widths, warp parameters, count, radial activation window, massif
  tuning, foreland tuning, broken-rock tuning, sand passes and native dunes are otherwise
  unchanged.
- The current user-tuned `arrakis_dev.json` from GitHub was used as the source profile. 0.5.12
  changes only `profile_version` and adds `rocky_floor_height` to that profile.
- Project version bumped to 0.5.12.

## Intended cross-section

With `core_width = 30`, `outer_width = 105`, and `rocky_floor_height = 4`:

```text
surrounding massif
██████████\                         /██████████
██████████ \                       / ██████████
██████████  \_____________________/  ██████████
                    ~Y68
             full rocky fault core

or, on a sand-floor segment:

██████████  \_____________________/  ██████████
                    Y64
```

The outer shoulders still interpolate smoothly. The central depth no longer depends on the
original mountain height.
