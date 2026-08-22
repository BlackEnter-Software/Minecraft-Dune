# Rock Surface Erosion — 0.5.14.1

0.5.14.1 is a refinement of the 0.5.14 escarpment/lithology release. The large 3D
escarpment, undercut and talus system remains unchanged. This pass adds the missing
low-amplitude erosion that should be visible on ordinary exposed rock between those major
events.

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
0.5.14.1 ordinary rock-surface erosion
    ↓
talus
    ↓
native dunes
```

`RockSurfaceErosionField` is deterministic, analytic and removal-only. It never places rock
outside the existing macro/fissure envelope and uses no post-generation world edits.

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

Turns the 0.5.14.1 surface pass on/off.

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

0.5.14.1 evaluates a narrow halo outside each fissure's existing half-width. The extra width
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

The surface pass avoids the four macro-neighbor probes used by the major escarpment system.

Per X/Z column it precomputes:

- province permission;
- formation-mask edge gate;
- coarse/detail 2D noise;
- fissure proximity.

Per-Y work is limited to rock-bearing columns and only performs additional 3D noise when a
column is close to a fissure or an exposed formation edge.

## Deferred

Still deferred after 0.5.14.1:

- full dry/mineralized/collapse cavern generation;
- extremely rare sealed water caverns;
- regional wind/shelter and sand-supply fields;
- dynamic sand transport;
- physical collapse simulation;
- final texture-art pass;
- complete mesa/butte lifecycle.
