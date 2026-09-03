# Arrakis terrain profile — parameter reference (0.6.0-dev.1 — Buried Rock)

## Current profile 6000

The active development JSON now selects `profile_version: 6000`. New controls are grouped
under `buried_rock.rock_surface`, `buried_rock.sediment`, `buried_rock.fault_displacement`,
`buried_rock.erosion` and `buried_rock.talus`. Every parameter, default and geological role is
documented in the [buried-rock parameter reference](BURIED_ROCK_TERRAIN_0.6.0-dev.1.md#settings).

Rock and sediment are independent absolute surfaces. Existing geometry/dune/material controls
are reused, but legacy repair groups are absent from the active preset and cannot run in 6000.
Saved 5148 worlds retain their old path; create a new world rather than editing a saved profile
number. The reference below is retained for legacy interpretation, **not current 6000 tuning**.

## Legacy 5148 front-shell cleanup

The development preset adds an optional top-level object:

```json
"front_shell_cleanup": {
  "enabled": true,
  "pass1_depth": 2,
  "pass2_depth": 2
}
```

- `enabled`: default **false**. When true, a final removal-only rock stage peels obsolete
  desert-facing Shield-Wall shell columns after erosion and existing orphan/component cleanup.
- `pass1_depth`: integer 0–4, preset **2**. Horizontal depth examined from the actual surviving
  front during the first pass.
- `pass2_depth`: integer 0–4, preset **2**. Additional horizontal depth examined after applying
  pass 1. `pass1_depth + pass2_depth` may not exceed four.

Depth is horizontal, not vertical: an eligible 60-block-high shell column is removed whole.
The structural scarp provides wall identity, normal, and a conservative ramp band; actual
post-filter occupancy decides exposure. Positive signed structural distance points into the
massif. On the inner wall the inward normal points away from the basin; on the outer wall it
points toward the massif center. Cleanup moves opposite that normal, toward the local desert.

Eligibility requires physical-massif and final rock ownership. Any regional-fault influence or
sand-pass influence protects the column; a foreland, Broken Rock, or transition weight stronger
than the physical massif also protects it. This is deliberately conservative at intersections.
The stage does not change macro morphology, either structural ramp, erosion, contact search,
wall relief, or talus. Missing `front_shell_cleanup` decodes to disabled, so profile `5148`
remains compatible. The mod version is `0.5.14.8.2`.

## Current tuning: deeper basal erosion and smaller varied talus

The development preset opts into three new serialized parameters. Their defaults reproduce
the previous hardening behavior in saved worlds. The mod version is `0.5.14.8.1` (Remnants),
while the serialized terrain profile remains `5148`.

- `erosion.surface.basal_erosion_depth`: integer 0–10, default 0, preset **4**. With
  `base_anchored_erosion=true`, both existing erosion passes protect through `64-depth`
  and reach full basal strength two blocks above that floor. The preset thus protects Y60,
  fades at Y61 and reaches full strength at Y62. It does not shift the noise, lithology,
  or upper wall pattern. Sand/fault datum remains Y64. Native rock omitted below the datum
  leaves the generator's normal substrate in place; the hard foundation is not excavated.
- `erosion.orphan_remnants.component_search_radius`: integer 3–5, default 3, preset **5**.
  The four-column removal limit, all-height connectivity and protected fault gates remain.
  The existing `inward_support_depth` is now **12**, increased from 8 to 10 in the first
  tuning pass and another two blocks after visual review; lateral search remains 2.
- `lithology.talus.organic_apron_enabled`: default false, preset **true**. Enables a curved,
  coherently varied apron and a shorter sand skirt. Preset maximum height is **4**, spread
  **10**, and sand-start fraction **0.80**. Variation gives an 8.5–11.5-block nominal gravel/
  colluvium spread and 16–20-block skirt reach. The actual visible toe can end sooner as
  sub-block deposits round away. Near-wall gravel still takes precedence over the skirt.

Actual contact search and wall-relief probing are unchanged; the organic mode retains the
Y71–76 wall-detection band independently of its smaller deposit height. The sand skirt
still extends four blocks inward and up to four layers down. This is static deposition,
not the future wind-driven sand-banking system. **Use a new Arrakis Dev world**: existing
worlds retain their serialized settings, even for newly generated chunks.
See [the basal tuning report](BASAL_EROSION_TALUS_TUNING_REPORT.md) for exact shapes and checks.

## Prior hardening addition: fault shoulders and ravine talus

Two further optional flags default to `false`; the development preset opts in:

- `erosion.orphan_remnants.fault_edge_cleanup_enabled`: allows the existing bounded
  component classifier on fault shoulders with carve mask <=0.85. Full cores and sand
  corridors stay protected. Radius 3, maximum four columns, all-height connections and
  conservative support/boundary retention are unchanged. This does NOT remove a small
  projection which is still connected to the broader toe (including 3050/70/190).
- `lithology.talus.ravine_contact_enabled`: after the existing massif search fails to
  qualify a contact, permits four local cardinal wall rays on fault shoulders. Each ray
  remains bounded to 32 cells, with the unchanged 24-cell connected wall-relief probe.
  It uses final pre-talus rock, not a nominal radius, and stops at protected core/pass
  cells. Qualified existing massif contacts always win. The same gravel shape and
  4-inward/24-outward/four-layer sand skirt consume the new actual contact.

The inspector now labels contact `source=massif` or `source=ravine` and shows the fault-edge
cleanup opt-in. No macro, erosion, face, lithology, fracture, fault-geometry or dune settings
changed. Version/profile stay `0.5.14.8` / `5148`; old saved profiles keep both flags false.
Create a NEW Arrakis Dev Seed-0 world for the next visual check. Existing worlds are not
migrated, and even their newly generated chunks retain their serialized options.
See [the fault-edge/ravine report](FAULT_EDGE_RAVINE_FINISHING_REPORT.md).

## Prior bounded-component and sand-skirt finishing pass

Two optional booleans default to `false` and are explicitly `true` in the development preset:

- `erosion.orphan_remnants.basal_component_cleanup_enabled`: post-orphan/pre-contact bounded
  component cleanup, radius 3, at most four connected columns. There is no pillar-height
  exemption. Connections above the residual Y65 floor, including diagonals and high ledges,
  protect connected ribs/buttresses; search-boundary and fault structures are retained.
- `lithology.talus.basal_sand_skirt_enabled`: actual-contact material skirt, up to four layers
  (Y61–64), four blocks inward and 24 outward. Depth is `ceil(4*(1-smoothstep(outward/24)))`.
  Existing contact/source/path/fault gates apply. Real cliff rock and visible gravel take
  precedence. A Y65 mantle is allowed only over a confirmed single-layer erosion residue,
  never over initially one-layer native foreland rock or an empty Y65 desert cell.

Neither changes existing erosion strengths or the contact search/probe. Missing flags preserve
the previous generation fingerprint. Version/profile remain `0.5.14.8` / `5148`, following the
explicit opt-in compatibility convention. A new development world is required to get the flags.
The retired uncommitted experiment's `micro_trim_enabled` and `sand_concealment_enabled`
keys are ignored, not aliases for the replacement. Worlds saved with those keys decode but
do not reproduce the discarded experimental finishing behavior in newly generated chunks.
See [the finishing report](BASAL_FINISHING_REPORT.md) for exact rules, tests and limitations.

## Current hardening addition: actual-contact talus

`lithology.talus.actual_contact_enabled` is an optional boolean, default `false`.
The development preset explicitly stores `true`. False/missing retains the historical
structural apron. True locates the final eroded wall with a bounded analytical search;
spread and inset must then be at most 32 blocks. Other material/shape controls are unchanged.

`erosion.surface.base_anchored_erosion=true` now also makes orphan basal protection end at
Y64. False/missing still protects through `64 + minimum_height_above_base` (Y69 with the
current value 5). The support depth, relief qualification and lateral rules are unchanged.
Existing true profiles can therefore lose unsupported Y65–69 blocks in newly generated
chunks even if they retain structural talus. Existing chunks are never rewritten.

Version/profile remain `0.5.14.8` / `5148`: talus compatibility is controlled by its explicit
flag, not an implicit version test. Use a new development world to get the new preset flag.
See [the current implementation report](BASAL_REMNANTS_ACTUAL_CONTACT_REPORT.md).

## Historical parameter reference

The native Arrakis generator reads its terrain parameters from:

```text
src/main/resources/data/minecraftdune/worldgen/world_preset/arrakis_dev.json
```

The `terrain` object is serialized into the world's chunk-generator data. For clean visual
comparisons after changing world-generation parameters, create a new Arrakis Dev world or
regenerate the affected region files while the world is closed.

## 0.5.14 erosion section

The source profile adds a third optional, backwards-decodable geological object:

```json
"erosion": {
  "enabled": true,
  "minimum_relief": 18.0,
  "face_probe_distance": 18.0,
  "escarpment_start_strength": 0.32,
  "vertical_face_bias": 0.84,
  "wind_exposure_strength": 0.42,
  "fracture_erosion_strength": 0.58,
  "soft_rock_multiplier": 1.35,
  "hard_rock_multiplier": 0.58,
  "very_hard_rock_multiplier": 0.28,
  "undercut_strength": 0.72,
  "max_undercut_blocks": 6,
  "undercut_frequency": 0.24,
  "broken_rock_scale": 0.72
}
```

A serialized 0.5.13 profile that omits `erosion` decodes with the pass disabled. The 0.5.14
source preset enables it explicitly, avoiding silent morphology changes at the unexplored edge
of an older world. The existing `lithology.talus` group is reused for cliff-base scree and is
enabled in the supplied 0.5.14 profile.

The current source preset uses `profile_version=5142`. Version 0.5.14.2 adds no JSON keys and
preserves every 0.5.14.1 erosion value. Ordinary face detection now reuses
`erosion.face_probe_distance` for its far height ring and
`erosion.surface.max_retreat_blocks + 1` (bounded internally) for its short height ring. See
[Escarpment and differential erosion](ESCARPMENT_EROSION.md) for the complete parameter table,
units, useful ranges, increase/decrease semantics, three-dimensional occupancy rules,
fracture/wind interactions and talus behavior.

## 0.5.13 lithology and fracture sections

Version 0.5.13 introduced two optional, backwards-decodable objects:

```json
"lithology": {
  "...": "3D units, strata, intrusions, rare bodies, sheets, mineral bands, materials and talus"
},
"fractures": {
  "...": "through-going massif fissures, finite branches and variable mineralization"
}
```

Older serialized profiles that omit either object receive the 0.5.13 defaults. Existing
generated chunks are not rewritten. The complete explanation of every lithology/fracture JSON
field, material role, resistance class, optional Create fallback, diagnostic, active erosion
input and future cavern hook is in
[Lithology and fracture framework](LITHOLOGY_AND_FRACTURES.md).

The 0.5.13 source profile used `profile_version=513`; that value remains valid in existing
serialized worlds.

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

A narrower interval generally produces a more abrupt macro height response. In 0.5.14 the
separate `erosion` object can replace eligible parts of that smooth apron with a 3D escarpment;
the massif thresholds remain the large-scale rock envelope.

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

## `erosion` — 0.5.14

`erosion` operates after macro geology, lithology and fissures but before talus and dunes. It
uses a signed rock-edge distance plus per-Y occupancy, allowing a column to contain surviving
rock above and below an eroded recess. It is removal-only and preserves the foundation layers,
faults and sand passes. The first one or two native-rock blocks above Y64 are also retained so
shallow outcrops survive. Fracture susceptibility fades below each fissure's design depth;
overlapping traces produce a bounded intersection signal that only modestly strengthens and
deepens the fissure and its nearby cliff response.

The main controls are grouped by role:

- candidate geometry: `minimum_relief`, `face_probe_distance`,
  `escarpment_start_strength`, `vertical_face_bias`;
- exposure: `wind_exposure_strength`, `fracture_erosion_strength`;
- material response: `soft_rock_multiplier`, `hard_rock_multiplier`,
  `very_hard_rock_multiplier`, with medium rock fixed at `1.0`;
- bounded negative-angle geometry: `undercut_strength`, `max_undercut_blocks`,
  `undercut_frequency`;
- detached-remnant application: `broken_rock_scale`.

The existing `lithology.talus` object supplies the switch, suitability threshold (`0.44` in the
source profile), maximum thickness, horizontal spread and gravel-based palette for localized
low-side cliff aprons. Final composition places talus above surviving rock and all full dune
blocks, then omits an optional fractional dune layer if it would overlap the scree. This keeps
the gravel fully supported. The full field-by-field guide is in
[ESCARPMENT_EROSION.md](ESCARPMENT_EROSION.md).

Regional wind shadow/sand transport, physical collapse, full caverns and sealed water remain
later systems. The completed cliff geometry is deliberately available as their future input.
