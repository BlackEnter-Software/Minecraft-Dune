# Structural Face Erosion Coupling - 0.5.14.4

0.5.14.4 connects the 0.5.14.3 structural scarp geometry to the existing 0.5.14.2
height-derived erosion system.

## Problem

0.5.14.3 correctly separated physical scarp width from broad province width. That made the
outer Shield Wall work well, but exposed two stale permissions in the erosion layer:

1. Inner Shield Wall erosion still used the broad `massifWeight`.
   With the source profile's 36-block inner scarp, the physical wall completed before the old
   150-block province ramp had enough weight to authorize erosion.

2. Regional fault erosion still treated most of `faultCarveMask` as protected floor.
   After 0.5.14.3, that same mask describes the physical 14-block fault wall, so the wall was
   accidentally protected from erosion together with the absolute floor.

## Fix

`ScarpMorphologyField` now exposes two coupling helpers.

### Physical massif erosion permission

Erosion permission is the maximum of:

- the broad geographical massif weight; and
- the 0.5.14.3 physical massif envelope.

The broad province remains useful for classification and regional geology. The physical
envelope authorizes erosion wherever the actual structural wall exists.

`RockFaceExposure` still decides whether a particular column is an exposed face from actual
neighboring heights. The physical massif value is permission, not a cliff detector.

### Fault floor versus wall permission

The fault depth mask is converted to a continuous erosion permission:

```text
fault carve <= 0.90       full erosion permission
0.90 .. 0.995             smoothly reduced permission
fault carve >= 0.995      protected structural floor/core
```

This keeps the full 0.5.12 absolute fault core protected while allowing nearly the complete
0.5.14.3 physical fault wall to receive the normal erosion stack.

In a fully active 14-block fault wall this means only the innermost roughly 1-3 blocks blend
toward floor protection; the rest of the wall can weather.

## JSON

No new tuning fields were added in 0.5.14.4.

The existing values remain authoritative:

```json
"massif": {
  "inner_scarp_width": 36.0,
  "outer_scarp_width": 48.0
},
"faults": {
  "morphology": {
    "wall_width": 14.0,
    "toe_depth": 4.0
  }
}
```

The profile version is bumped to `5144`.

Do not widen the inner scarp merely to make erosion activate. 0.5.14.4 is specifically meant
to make the existing 36-block scarp erode correctly. After visual confirmation, the scarp
widths can be tuned for aesthetics.

## Deferred

0.5.14.4 deliberately does not address:

- occasional unsupported/floating resistant remnants;
- eight-direction face probes;
- subsurface geology/caves;
- water caverns.

Those should be handled separately after face coverage is stable.
