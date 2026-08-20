# Arrakis terrain profile — parameter reference (0.5.13)

The native Arrakis generator reads its terrain parameters from:

```text
src/main/resources/data/minecraftdune/worldgen/world_preset/arrakis_dev.json
```

The `terrain` object is serialized into the world's chunk-generator data. For clean visual
comparisons after changing world-generation parameters, create a new Arrakis Dev world or
regenerate the affected region files while the world is closed.

## 0.5.13 lithology and fracture sections

The profile now includes two optional, backwards-decodable objects:

```json
"lithology": {
  "...": "3D units, strata, intrusions, rare bodies, sheets, mineral bands, materials and talus"
},
"fractures": {
  "...": "through-going massif fissures, finite branches and variable mineralization"
}
```

Older serialized profiles that omit either object receive the 0.5.13 defaults. Existing
generated chunks are not rewritten. The complete explanation of every new JSON field,
material role, resistance class, optional Create fallback, diagnostic, and future erosion /
cavern hook is in [Lithology and fracture framework](LITHOLOGY_AND_FRACTURES.md).

`profile_version` is `513` for the source 0.5.13 profile.

## 0.5.12 fault-floor correction

Fault depth is now defined by an **absolute target floor elevation**. A fully active fault
core no longer retains a residual percentage of the original mountain height.

With the default:

```json
"rocky_floor_height": 4.0
```

the two principal full-depth states are:

```text
rocky core -> Y64 + 4 = approximately Y68
sandy core -> Y64
```

Fault start/end radial fades and the `core_width -> outer_width` wall transition remain
continuous.

## General rules

- Values ending in `_radius`, `_scale`, `_width`, `_height`, or `spacing` are measured in
  Minecraft blocks unless stated otherwise.
- Noise threshold values are **not percentages**. The underlying centered noise is roughly in
  the range `-1 .. +1`.
- For threshold pairs:
  - below `*_threshold_low` -> no formation from that field;
  - between low and high -> formation fades in;
  - above `*_threshold_high` -> full-strength formation mask.
- Raising both thresholds produces **less / smaller-area rock**.
- Lowering both thresholds produces **more / larger-area rock**.
- A narrow `high - low` interval gives sharper, more discrete formation boundaries.
- A wide interval gives softer fringes.

---

## `basin`

### `pure_sand_radius`
Exact unwarped radius around `(0,0)` that is forced to remain flat sand.

Inside this radius the geology function returns immediately, so no foreland rock, massif,
fault, broken-rock, or native dune field can appear.

### `transition_end_radius`
Outer end of the basin province fade. This affects the reported/derived province strength
outside the exact pure-sand reservation.

---

## `foreland`

The foreland is the field of eroded fragments between the basin and the main massif.

### `end_radius`
Outer end of foreland influence. The current code fades the foreland over approximately the
last 100 blocks before this value.

### `large_scale`
Main spatial scale of the larger foreland knobs/shelves.

This is a noise wavelength/control scale, **not the literal diameter of each rock**.
Larger values produce broader, slower-changing rock clusters.

### `detail_scale`
Secondary irregularity added to the large formations.

Lower values add finer edge variation. Higher values make the detail vary more broadly.

### `micro_scale`
Spatial scale of the separate micro-rock population.

Lower values -> smaller/faster-changing micro-rock clusters.
Higher values -> broader micro-rock clusters.

### `large_threshold_low` / `large_threshold_high`
Density/footprint control for the large foreland field.

Example:

```text
low  = 0.08
high = 0.40
```

is fairly permissive. More of the positive noise becomes rock.

To reduce the number/footprint of large rocks, raise both values.

### `micro_threshold_low` / `micro_threshold_high`
Density/footprint control for micro-rock.

These are usually higher than the large-rock thresholds because micro-rock can otherwise
cover too much of the sand.

Example:

```text
0.45 / 0.52
```

means only relatively strong positive micro-noise peaks become rock.

### `large_min_height` / `large_max_height`
Target vertical range for well-developed large foreland formations.

These are not strict final minimum/maximum values because formation-mask strength scales the
result near the edges.

### `micro_max_height`
Upper target height of micro-rock. The current implementation starts the micro relief around
1.5 blocks and interpolates toward this value.

### `inner_height_scale` — NEW in 0.5.11
Controls how short the **large** foreland fragments are near the inner basin edge.

```text
0.20 -> about 20% of configured large-rock height near the basin
0.50 -> about 50%
1.00 -> no radial height growth
```

The multiplier smoothly approaches `1.0` toward `massif.start_radius`.

The micro-rock height also grows toward the massif, but its inner multiplier is clamped so
micro-rock does not disappear entirely.

### `inner_threshold_boost` — NEW in 0.5.11
Shrinks the footprint/density of the large fragments near the inner edge.

Near the basin, this value is added to **both** large thresholds. The added amount fades to
zero toward the massif.

Example with:

```text
large thresholds      = 0.08 / 0.40
inner_threshold_boost = 0.20
```

near the basin the effective thresholds are approximately:

```text
0.28 / 0.60
```

Only the strongest peaks survive, so the first boulders are smaller and rarer. Near the
massif the effective thresholds return to `0.08 / 0.40`, allowing larger remnants.

### `growth_power` — NEW in 0.5.11
Shapes how quickly foreland rocks grow from the basin toward the massif.

The base growth progress is calculated from:

```text
basin.pure_sand_radius -> massif.start_radius
```

Interpretation:

```text
1.0  = neutral smooth progression
>1.0 = stays small for longer, grows mainly near the massif
<1.0 = grows earlier
```

Useful experimental range:

```text
0.6 .. 2.0
```

---

## `massif`

### `start_radius`
Where the main Shield Wall / massif begins to activate.

### `full_radius`
Where the inner activation reaches full strength.

### `outer_start_radius` / `outer_end_radius`
Controls the main massif's outer termination. A short interval produces a more abrupt edge;
a wider interval creates a longer fade.

### `max_added_height`
Hard cap for rock relief above the base Y=64 surface.

### `continuity_low` / `continuity_high`
Threshold pair for the broad massif continuity field.

Lower values make the massif more connected/continuous.
Higher values create more holes and separation.

### `shape_low` / `shape_high`
Maps massif mask strength into vertical relief.

A narrower interval generally produces a more abrupt height response; this is **not yet**
the final escarpment-steepness/overhang system.

---

## `faults`

### `count`
Number of main seeded fault traces.

### `start_radius` / `full_radius`
Radial activation of the fault network.

### `fade_start_radius` / `end_radius`
Outer fade/deactivation of the fault network.

### `core_width`
Half-width, in blocks, of the guaranteed deepest fault core.

For example:

```text
core_width = 30
```

means the full-depth central floor is approximately 60 blocks wide when the radial fault
gate is fully active.

From 0.5.12 this has literal depth semantics: inside the core, the generator targets the
configured structural floor instead of merely removing a percentage of the original massif
height.

### `outer_width`
Half-width where the fault carve has faded completely back into unaffected terrain.

Between `core_width` and `outer_width`, the terrain interpolates from the structural floor
toward the surrounding rock. This controls wall/shoulder width; it does **not** change the
target floor elevation.

### `broad_warp_scale` / `broad_warp_strength`
Large meander of the fault centerline.

- `scale` -> distance over which the bend changes;
- `strength` -> lateral displacement in blocks.

### `medium_warp_scale` / `medium_warp_strength`
Smaller/faster centerline meander.

### `sine_warp_scale` / `sine_warp_strength`
Additional smooth periodic bending.

### `sandy_floor_threshold`
Controls whether a fault segment has a rocky structural floor or is filled/cut to the base
sand.

The along-fault floor noise is evaluated independently from fault depth:

```text
lower threshold  -> more sand-floor segments
higher threshold -> more rocky-floor segments
```

0.5.12 removes the old double thresholding. The sand/rock decision now changes the target
floor material/elevation but does not weaken the depth of the fault itself.

### `rocky_floor_height` — NEW in 0.5.12
Absolute height, in blocks, of a fully carved **rocky** fault floor above the base Arrakis
sand surface at Y=64.

Example:

```json
"rocky_floor_height": 4.0
```

means a full rocky fault core targets approximately Y=68 regardless of whether the
surrounding massif is 80 blocks or 200 blocks high.

A fully sandy fault segment targets Y=64.

This is the main 0.5.12 correction. Previously the generator retained a percentage of the
original mountain height, so the same fault could become unexpectedly shallow when it
crossed a tall part of the massif.

---

## `sand_passes`

Two broad, seeded sandy corridors through the main rock belt.

### `start_radius` / `full_radius`
Where the pass carve activates inward.

### `fade_start_radius` / `end_radius`
Where it fades outward.

### `primary_core_width` / `primary_outer_width`
Strong/full center and total fading width of the first corridor.

### `secondary_core_width` / `secondary_outer_width`
Equivalent widths for the second corridor.

---

## `broken_rock`

The detached outliers beyond the main massif.

### `start_radius`
Massif-facing start of broken-rock influence.

### `full_radius`
Where the broken-rock **province weight/density envelope** reaches full strength.

Important: from 0.5.11, size decay does **not** wait until this radius.

### `outer_fade_start_radius`
Where the overall broken-rock province begins its final outer fade.

### `outer_radius`
Outer end of the main broken-rock province.

### `large_scale`
Spatial scale of the large detached remnants.

### `detail_scale`
Secondary irregularity of the large-remnant field.

### `micro_scale`
Spatial scale of smaller outer remnants.

### `max_height_inner`
Maximum target height of large remnants near the massif-facing side.

### `max_height_outer`
Maximum target height of large remnants near the outer side.

### `micro_max_height`
Outer micro-remnant height cap.

### `size_decay_power` — NEW in 0.5.11
Controls the radial progression from large near-massif remnants toward small outer remnants.

Progress now starts at:

```text
broken_rock.start_radius
```

and reaches full decay at:

```text
broken_rock.outer_radius
```

Interpretation:

```text
1.0  = neutral smooth decay
>1.0 = keeps large remnants longer, then shrinks later
<1.0 = starts shrinking faster near the massif
```

The same progress changes several things together:

- maximum large-remnant height decreases;
- large-remnant threshold rises, shrinking footprints;
- large-remnant mask strength decreases;
- micro-remnants gain relative importance outward;
- micro-remnants eventually shrink/fade too.

This makes the field read as debris/remnants eroded progressively away from the main massif.

---

## `outer_transition`

### `start_radius` / `full_radius`
Where the mixed sand-rock transition activates and reaches full influence.

### `fade_start_radius` / `outer_radius`
Where the low remnant field fades away.

### `open_erg_start_radius` / `open_erg_full_radius`
Where the open-erg weight begins and reaches full strength.

---

## `native_dunes`

### `max_height`
Maximum native dune relief at suitability `1.0`.

### `spacing`
Approximate crest-to-crest spacing of the native transverse dune field.

### `spacing_variation`
Amount of phase/spacing warping.

Higher -> less regular parallel dune spacing.
Lower -> more uniform dune spacing.

### `ridge_sharpness`
Raises the normalized ridge profile to a power.

Higher -> narrower/sharper crest zone.
Lower -> broader rounder dune body.

### `valley_cutoff`
Suppresses weak low ridge values.

Higher -> cleaner/wider interdune flats.
Lower -> more low sand remains between dunes.

### `slope_asymmetry`
Controls the windward/lee asymmetry.

Higher values give a longer windward ramp and shorter lee face.

### `wind_angle_degrees`
Native dune wind direction in the generator's world-axis convention.

### `foreland_weight` — NEW in 0.5.11
Maximum province contribution to dune suitability in sandy foreland gaps.

Because final dune height is approximately:

```text
max_height * suitability * local_dune_profile
```

with `max_height = 30`:

```text
foreland_weight = 0.10 -> up to roughly 3 blocks
foreland_weight = 0.16 -> up to roughly 4.8 blocks
foreland_weight = 0.25 -> up to roughly 7.5 blocks
```

Rock height locally suppresses dune suitability, so dunes prefer the sand between fragments.

### `broken_rock_weight`
Equivalent dune-activity contribution inside the Broken Rock Desert.

Increasing this is the main control for adding small/medium dune activity among detached
outliers before the outer transition.

### `transition_weight`
Dune contribution in the Sand–Rock Transition.

### Dune suitability and rock suppression

The province weights do not directly force dunes onto rock. The final suitability is
suppressed as geological rock height rises.

This is intentional:

```text
open sand gap   -> dunes can grow
low rock fringe -> partial suppression
large rock      -> dunes largely suppressed
```

---

## Native rock foundation — 0.5.11

Visible geological formations are no longer placed only above the Y=64 sand surface.

For every column where native geology produces visible rock, the generator scans downward
through the flat Arrakis base until it finds existing hard crust (`stone`, `deepslate`, or
`bedrock`). It replaces the softer layers above that crust with stone and then continues the
formation upward.

With the current base stratigraphy this means the sandstone and sand directly beneath a
formation are replaced by contiguous stone.

Conceptually:

```text
BEFORE 0.5.11                0.5.11

      ROCK                       ROCK
      ROCK                       ROCK
~~~~~~SAND~~~~~~                 ROCK
~~~~~~SAND~~~~~~                 ROCK
====SANDSTONE===                 ROCK
====SANDSTONE===                 ROCK
######STONE#####             ####STONE#####
```

This is a column foundation operation only where visible native rock exists. Ordinary desert
columns retain the normal sand/sandstone layers.

---

## Current proposed next geological step

After the JSON/profile tuning is stable, the next major morphology step should be a true
**3D escarpment and erosion pass**, rather than another height-field adjustment.

Target behavior:

- near-vertical, locally unclimbable hard-rock walls;
- resistant caprock / strata;
- preferential removal of softer layers;
- undercut shelves;
- locally negative-angle / overhanging faces;
- collapse scars and talus where unsupported rock fails.

That 3D pass should happen before regional wind-shadow / sand-transport coupling, because
the final cliff geometry should be what the wind system sees.
