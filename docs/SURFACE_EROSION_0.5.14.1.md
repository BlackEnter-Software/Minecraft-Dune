# Exposed Cliff Face Erosion — 0.5.14.2

0.5.14.2 corrects the geometric limitation discovered while testing the 0.5.14.1 surface
pass. A formation mask identifies a geological body; it does not prove whether a column is
beside open air. The surface pass now derives exposure from actual neighboring terrain
heights and applies low-amplitude erosion through the complete exposed wall interval. The
large 3D escarpment, undercut and talus system remains in place.

## Generation order

```text
macro geology
    ↓
3D lithology
    ↓
massif fissures
    ↓
0.5.14 major escarpment / differential erosion
    ↓
shared height-derived face exposure
    ↓
0.5.14.2 whole-face rock erosion
    ↓
talus
    ↓
native dunes
```

`RockFaceExposure` samples short-range and far-range cardinal terrain heights once per X/Z
column. It reports relief, steepness, downhill normal, high/low elevations, exposed vertical
interval, and a bounded estimate of distance behind the face. `RockSurfaceErosionField` then
reuses that sample for every Y in the column. Both fields are deterministic, analytic and
removal-only, never place rock outside the existing macro/fissure envelope, and use no
post-generation world edits.

## Why formation mask was insufficient

A Shield Wall column can retain `rockFormationMask = 1.0` while its neighboring terrain is
140 blocks lower. The former mask-space edge gate called that interior rock and suppressed
side erosion. In 0.5.14.2, the same column is a strong exposed face because its measured
height relief and steepness are high. Formation/province values now provide geological
eligibility and reduced small-rock strength; neighboring heights provide physical exposure.

## Whole-face recession

The short probe ring is at most the configured ordinary retreat plus one block. It locates the
physical edge and prevents the pass from reaching arbitrarily into solid rock. The existing
`erosion.face_probe_distance` is the far ring and measures the full wall relief and normal.
For candidate columns, coherent 3D noise defines a bounded recession demand across
`face_low_y .. face_high_y`. Material resistance is evaluated independently at every Y, so
soft beds recess while granite/deepslate ribs and basalt/blackstone ledges can stand proud.

## What it changes

The surface pass targets four problems seen during 0.5.14 visual testing:

1. long smooth massif faces between major erosion events;
2. clean/smooth fissure walls;
3. smooth foreland boulders;
4. smooth Broken Rock remnants.

It does not make every formation contain a cavern. Large cavities and undercuts remain the
responsibility of `EscarpmentErosionField`.

## Surface JSON

The settings are nested under `terrain.erosion.surface`.

```json
"surface": {
  "enabled": true,
  "strength": 0.34,
  "scale": 18.0,
  "detail_scale": 6.0,
  "max_retreat_blocks": 4,
  "fissure_multiplier": 1.55,
  "small_rock_strength": 0.42,
  "broken_rock_strength": 0.58,
  "lithology_relief_strength": 0.65
}
```

### `enabled`

Turns the ordinary exposed-face surface pass on/off.

Old serialized 0.5.14 profiles that do not contain `surface` decode with this pass disabled,
preventing an automatic world-border morphology change.

### `strength`

Overall surface-recession strength.

- lower: cleaner/smoother exposed rock;
- higher: more recession and edge breakup.

Recommended tuning range: roughly `0.15–0.65`.

### `scale`

Primary horizontal/coherent roughness scale in blocks.

Larger values produce broader recessions. Smaller values produce more rapidly changing faces.

### `detail_scale`

Secondary detail scale in blocks.

This is blended with `scale`; it is not decorative per-block speckle.

### `max_retreat_blocks`

Hard runtime limit used by the surface/fissure-width pass.

The default is `4`. Major 0.5.14 undercuts use their own `max_undercut_blocks`.

### `fissure_multiplier`

Additional erosion applied around existing local fissure walls.

The pass primarily widens/weathers fissures. It does not replace the 0.5.13 fissure depth
calculation.

### `small_rock_strength`

Relative permission for Inner Rock Foreland formations.

- `0`: no surface erosion on foreland rocks;
- `1`: same province permission as the main massif before other gates.

### `broken_rock_strength`

Relative permission for Broken Rock and lower transition remnants.

### `lithology_relief_strength`

How strongly material resistance changes the physical silhouette.

At `0`, lithology has little extra influence on this low-amplitude pass.

As the value increases:

- soft sandstone/tuff/limestone recess farther;
- stone/calcite remain near baseline;
- andesite/diorite/granite remain more prominent;
- basalt/blackstone remain the most resistant.

## Granite

Granite is introduced as another hard coherent intrusive material.

```json
"granite": "minecraft:granite",
"granite_fraction": 0.28
```

`granite_fraction` controls the share of already-qualified intrusive bodies that become
granite. It does not change the overall `intrusion_threshold`.

Granite is classified as `HARD`, alongside andesite and diorite.

## Deepslate basement

Deepslate is introduced as hard ancient basement material rather than random surface texture.

```json
"deepslate": "minecraft:deepslate",
"deepslate_top_y": 72.0,
"deepslate_warp_strength": 8.0
```

`deepslate_top_y` is the average upper elevation of the native deepslate basement. The supplied 72-block value keeps it mostly below ordinary exposed rock while allowing deep fissures and escarpments to reveal it.

`deepslate_warp_strength` vertically warps that contact using a broad coherent field.

The intent is that deepslate becomes visible mainly where faults, fissures or escarpments cut
deeply enough to expose it.

## Fissure behavior

The existing fissure geometry remains authoritative for depth.

0.5.14.2 evaluates a narrow halo outside each fissure's existing half-width. The extra width
is then scaled by:

- surface strength;
- `fissure_multiplier`;
- fracture activation/intersection;
- lithology resistance;
- coherent vertical variation.

Soft units therefore weather into broader cracks while resistant units retain narrower slot
walls.

## Small-rock behavior

Small formations do not receive the full major-cavity algorithm merely because surface
erosion is enabled.

They receive:

- shallow top recession;
- coherent edge trimming;
- material-dependent silhouette relief.

This is intended to make foreland and Broken Rock formations look like weathered remnants
rather than scaled-down smooth hills.

## Performance

Per X/Z column, one shared face sample performs two fixed cardinal probe rings. The far ring
replaces the former neighbor sampling inside `EscarpmentErosionField`; the short ring is the
only additional height work and is bounded by `max_retreat_blocks + 1`. The resulting face
geometry, province permission, 2D pattern and fissure proximity are stored in the terrain
column. Per-Y work remains limited to rock-bearing columns and only evaluates 3D recession
noise for exposed-face or fissure-wall candidates. There is no radius search, iterative
simulation, registry lookup, chunk-local state or chunk-order dependency.

## Deferred

Still deferred after 0.5.14.2:

- full dry/mineralized/collapse cavern generation;
- extremely rare sealed water caverns;
- regional wind/shelter and sand-supply fields;
- dynamic sand transport;
- physical collapse simulation;
- final texture-art pass;
- complete mesa/butte lifecycle.
