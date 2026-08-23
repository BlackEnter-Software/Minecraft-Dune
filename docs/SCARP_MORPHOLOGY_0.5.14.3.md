# Scarp & Fault-Wall Morphology - 0.5.14.3

0.5.14.3 corrects the structural precursor underneath the 0.5.14.x erosion stack.

The 0.5.14.2 face detector works from real neighboring heights, but the macro generator still
used broad province fades as physical mountain slopes. That produced two visible artifacts:

- the outer Shield Wall remained a broad smooth hillside, often too gentle to become a strong
  exposed-face candidate;
- the inner Shield Wall and regional faults could show an erosion "front" above a smooth
  lower boot/shoulder.

`ScarpMorphologyField` separates **geographical province width** from **physical scarp width**.

## Generation order

```text
broad province envelopes
        |
        v
ScarpMorphologyField structural precursor
        |
        v
MacroGeologyField physical rock height
        |
        v
RockFaceExposure
        |
        v
EscarpmentErosionField + RockSurfaceErosionField
        |
        v
lithology relief / undercuts / talus
```

The erosion algorithms remain removal-only. This pass gives them an appropriately steep
starting wall.

## JSON

The active profile adds structural controls directly to the existing `massif` and `faults`
groups:

```json
"massif": {
  "...": "...",
  "scarp_morphology_enabled": true,
  "inner_scarp_width": 36.0,
  "outer_scarp_width": 48.0
},
"faults": {
  "...": "...",
  "morphology": {
    "wall_width": 14.0,
    "toe_depth": 4.0
  }
}
```

### `massif.scarp_morphology_enabled`

Enables the 0.5.14.3 structural precursor.

Missing data decodes disabled so a serialized 0.5.14.2 generator retains its old morphology
instead of changing at newly generated chunk borders.

### `massif.inner_scarp_width`

Horizontal width in blocks over which the physical Shield Wall rises from the basin side.

Smaller = more vertical.
Larger = broader inner ramp.

The broad `massif.start_radius -> massif.full_radius` interval still exists for province and
geological weighting; this value controls only physical rock height.

### `massif.outer_scarp_width`

Horizontal width in blocks over which the physical Shield Wall falls beginning at
`massif.outer_start_radius`.

Smaller = steeper outer escarpment.
Larger = broader outer slope.

The existing `massif.outer_start_radius -> massif.outer_end_radius` interval remains the broad
geographical/faulted-margin transition.

### `faults.morphology.wall_width`

Horizontal width in blocks over which a regional fault rises from the guaranteed full-depth
core to surrounding rock.

`faults.core_width` still defines the absolute-floor core from 0.5.12.
`faults.outer_width` remains the broader structural influence zone.

### `faults.morphology.toe_depth`

Maximum lowering, in blocks, applied to the remaining outer fault influence after the steep
physical wall.

This creates a shallow toe/shoulder instead of a tens-of-blocks-high percentage-depth ramp.
Set to `0` for no structural toe; erosion/talus can still produce debris at the base.

The complete `faults.morphology` object is optional. When it is absent, the codec uses a
75-block wall width and zero toe depth, matching the legacy 0.5.14.2 fault transition. Keeping
the two new controls in their own codec also keeps `FaultSettings.CODEC` within
DataFixerUpper's 16-field `RecordCodecBuilder.group` limit.

## Source-profile defaults

```text
inner_scarp_width = 36
outer_scarp_width = 48
fault wall_width  = 14
fault toe_depth   = 4
```

With the current source profile, the Shield Wall can approach 200 blocks of added relief.
These defaults are intentionally steep and normally unclimbable by ordinary Minecraft
movement.

If the first visual test is too vertical, increase the widths before changing erosion strength.

## Regional faults

The old fault cross-section interpolated across the complete:

```text
core_width -> outer_width
30 blocks -> 105 blocks
```

which allowed roughly 75 blocks of smooth depth ramp.

0.5.14.3 instead uses:

```text
core_width
    guaranteed absolute floor
        |
        v
wall_width
    steep physical canyon wall
        |
        v
remaining outer_width
    weak toe/influence only
```

Rocky/sandy absolute fault-floor semantics remain unchanged.

## Deferred

Still deferred to 0.5.15 and later:

- subsurface cave networks;
- limestone cavern dissolution;
- sealed water caverns;
- dynamic collapse;
- regional wind/shelter/sand-supply fields.
