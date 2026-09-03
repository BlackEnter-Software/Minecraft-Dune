# 0.6.0-dev.1 — Buried Rock

Baseline: `main`, `432d5201b65208243bee3af806b9be97ba755a9f`
(`0.5.14.8.2 - Front-Shell Cleanup`). New development terrain profile: **6000**.
Use a fresh Arrakis Dev world; do not change the profile number in an existing save.

## Implemented architecture

The old model added special native rock above a flat substrate, eroded individual occupied
blocks, then repaired remnants and searched for displaced cliff contact. That model remains
available only to legacy profiles. Profile 6000 has a separate production path:

```text
regional geological roof + warped tectonic uplift + signed fault displacement -> R0
basin grading + regional sediment + unchanged transverse dunes              -> S
R0 + S -> analytical external exposure -> one fixed recession pass           -> Re
Re + erosion supply -> bounded downslope colluvium                           -> C
bedrock + solid geology through Re + sediment through S + C                  -> final column
```

`ArrakisTerrainEvaluator` remains the authoritative facade. Its `BuriedRockTerrain` engine
owns the operation-local cache and this dependency graph. `BuriedTerrainColumn` is the shared
composer consumed by both actual chunk writes and base-column queries. Height queries use
its highest occupied block. Profile 6000 cannot enter the facade's legacy column cache:
attempts throw instead of quietly running any cleanup stage.

### Rock, uplift and sediment

`RawRockSurfaceField` evaluates non-flat regional geology **everywhere**, including the basin.
It adds the stronger of Shield-Wall uplift and secondary formation uplift, then signed fault
displacement. `MacroGeologyField.structure` reuses the existing macro geometry, sand corridors
and smooth 36/48-block structural ramps. It does not perform the old fault-floor carve or
apply the old -4-block alignment offset. Regional geology survives even where uplift is zero.

The initial regional roof is 36 +/-14 blocks, with +/-4 detail; the geological roof is bounded
to -48..300. The lower bound protects geological thickness above bedrock, not a Y64 pedestal.
Shield-Wall uplift is scaled by 1.15 to retain approximately the existing wall scale despite
the lower regional roof. This is initial analytical tuning, not verified visual parity.

`SedimentSurfaceField` has no rock-height input. Its absolute datum is 64, regional relief is
3 blocks and the existing transverse dune mathematics is unchanged. The shared central-basin
radii grade the sediment to a level Y64 through radius 1500, blending out to radius 2000.
Buried geology beneath this area is not flattened. Province weights modulate dune suitability;
the old exposed-rock mask does not gate geological existence.

Conceptually, sediment thickness is `max(0, S-Re)` and external height is `max(Re,S,C)`.
For blocks, `floor(Re)` is the highest full geological block. Sediment stores sixteenth-block
units: full blocks through `floor(S)` and a fractional layer at the next Y when needed.
`finalSurface` reports this quantized rock roof plus fractional sediment/deposits;
`highestOccupiedY + 1` is the Minecraft world-surface base-height query. Motion-blocking and
ocean-floor queries apply Minecraft's actual block predicate to the same composition, so a
non-motion-blocking thin sand layer may be skipped. Thus fractional sand has a higher
occupied block coordinate than its continuous surface value. There are no unsupported holes
between geology, sediment and deposits.

### Lithology and faults

`LithologyField.Column` uses `geologicalY = worldY - structuralDisplacement`. Uplift and fault
throw move the entire stratigraphic evaluation, including basement, coherent contact noise,
intrusions, sheets, limestone and calcite. The material palette and registry fallbacks are
unchanged. Create remains optional; unavailable limestone resolves to the configured sandstone.

The active profile changes `lithology.materials.deepslate_top_y` from 72 to **-20 geological Y**.
Keeping 72 after changing coordinates would blanket the new regional roof in basement and
hide most other units. All other existing lithology tuning is preserved. The old value is
retained in the frozen legacy fixture and existing worlds.

`GeologicalFaultField` reuses the exact deterministic warped fault traces. Each finite radial
trace contributes smooth, signed up/down throw, localized across its influence band, and a
damage zone that increases erosion weakness. Diagnostics expose signed distance, side, strike,
damage and dominant trace. The roof and strata receive the same displacement. No side is
targeted to Y64. This first model has localized throw (tapering back to zero), not full persistent
tectonic blocks, dip mechanics or thrust faults.

`MassifFractureField.structural` reuses the through-going traces, branches, mineralization
metadata and resistance modulation without the native-root/massif activation gate. Fractures
drive external incision and susceptibility; they do not carve buried cave air in this version.

### Erosion migration

`RockFaceExposure.external` compares R0 with neighboring `max(R0,S)` heights. Buried roofs
have no external cliff exposure. No world, chunk block, air or cave state is an input. The
existing cardinal near/far probes, gradients, relief, steepness and downhill normals survive.

`RockErosionField` adapts the successful erosion character to **one fixed 2.5D recession pass**:

- Preserves face-detail, coarse, detail and vertical noise salts, the 310-block wind shelter
  field, face detail frequencies 13/19, coarse/detail 18/6 defaults and 72/28 pattern weighting.
- Keeps relief gating, exposure gating, wind-facing response, fracture proximity/intersection
  susceptibility, and soft/medium/hard/very-hard responses 1.35/1/.58/.28.
- Reuses the .34/.28 differential-recession response, .18/.82 surface-weathering pattern and
  .65 lithology-relief contrast; adds explicit fault-damage weakness.
- Converts horizontal recession into roof lowering by sampling the raw roof downhill;
  exposed fracture incision and shallow top weathering are removal-only contributions.
- Does not preserve old occupancy gaps, undercut pockets, support/root protections or repairs.

The default maximum horizontal distance is 6 major +4 surface blocks, not a 10-block vertical
limit. A steep slope can lose much more height through that recession. Every column below Re
is filled solidly. True overhangs and exact old cliff silhouettes are deliberately not claimed.
Exposure is based on the provisional raw roof; there is no second exposure/recession iteration.

### Talus

`TalusColluviumField` is a finite eight-direction downslope transport approximation. It samples
eroded source roofs every two blocks within a 16-block radius, requires removed rock, final
source rock still above sediment, a meaningful height drop and an outward-facing source.
The strongest source contributes bounded gravel/source-lithology debris with coherent patch
variation, distance taper and limited distal sand. No source or no cliff means no deposit.
It never asks about structural contact, remnant ownership or another talus deposit.

This is not mass-conserving: several recipient columns may use one source, and narrow source
features may fall between stencil rays. Deposits on fractional sediment solidify its top layer
for support, adding up to one extra filled block beneath the configured deposit height.
Cliff-face debris may occur on sloping rock, not exclusively at a single basal toe.

## Integration, caching and compatibility

- `FlatLevelSource` remains the Minecraft integration superclass. Its temporary substrate is
  overwritten at every Y by the new composer, including air above the terrain. Base columns
  allocate the full world height, not FlatLevelSource's 129-layer substrate buffer. Foundation
  discovery does not run on profile 6000. Generation heightmaps are re-primed afterwards.
- The existing **one bottom bedrock layer at Y=-64** is preserved. This does not add vanilla's
  randomized multi-layer bedrock roughness. Geology begins at -63.
- The existing fauna restrictions, both animals, animations, layered sand, cameras, screenshot
  tools, frozen `DuneSimulation`, runClient JVM/ZGC settings and dependencies are unchanged.
- Caves/features/lakes/structures remain disabled in the current development preset. Future
  cave carving can run after solid geology without feeding air back into erosion.
- Caches are bounded (64 query /1024 chunk entries), per-operation, and optional. Evictions only
  cause recomputation. Raw/interpolated halo samples never depend on eroded or generated neighbor
  blocks; talus sources request erosion, never completed deposits. The default dependency reach
  is at most 16 talus +18 exposure +1 interpolation block, so no neighboring chunk must exist.
- `-Dminecraftdune.terrainMetrics=true` adds stage evaluation counts for raw rock, faults,
  sediment, lithology columns, exposure, erosion, talus and composition to existing metrics.
  Raw-stage misses also count analytical halo work. Metrics never affect results.
- Profile 5148 and older recognized profiles keep the existing runtime path. The old erosion,
  orphan/component/front-shell filtering, actual-contact apron and sand skirt are **legacy-only**.
  Unsupported versions above 5148 other than 6000 are rejected, not silently treated as legacy.
- Defaults are codec-optional and may be omitted when saved. Their profile-6000 meanings must
  remain stable; future changed defaults need an explicit compatibility revision.

## Settings

New parameters live under `terrain.buried_rock`; all values below are initial defaults.
Distances/scales are blocks. Every numeric codec checks finite values and bounded ranges.
Existing basin, massif, foreland, Broken Rock, transition, sand-pass, lithology, fracture and
dune groups retain their useful geometry/material controls.

### `rock_surface`

| Key | Default | Meaning |
| --- | ---: | --- |
| `regional_y` | 36 | Mean buried geological roof, independent of sediment. |
| `amplitude` | 14 | Regional relief amplitude. |
| `scale` | 1200 | Regional relief wavelength. |
| `detail_amplitude` | 4 | Smaller buried relief amplitude. |
| `detail_scale` | 280 | Smaller relief wavelength. |
| `uplift_scale` | 1.15 | Shield-Wall structural uplift multiplier. |
| `other_relief_scale` | 1 | Secondary formation uplift multiplier. |
| `minimum_y` | -48 | Geological roof safety minimum above bottom bedrock. |
| `maximum_y` | 300 | Geological roof ceiling below the world build limit. |

### `sediment`

| Key | Default | Meaning |
| --- | ---: | --- |
| `datum` | 64 | Absolute central-basin depositional elevation. |
| `relief` | 3 | Regional sediment relief outside the graded basin. |
| `scale` | 2400 | Sediment relief wavelength. |
| `compaction_depth` | 8 | Depth below full sediment top at which infill becomes sandstone. |

### `fault_displacement`

| Key | Default | Meaning |
| --- | ---: | --- |
| `maximum_throw` | 32 | Maximum single-trace opposite-side offset; accumulated displacement also bounded to +/-this. |
| `transition_width` | 12 | Smooth signed-throw transition scale across the trace. |
| `influence_width` | 220 | Half-width to which localized throw fades; must cover transition/damage widths. |
| `damage_width` | 40 | Half-width of the erosion-weakness zone. |

The shared root `faults` group supplies count, radial gates and warped trace geometry. Its
legacy core/outer widths and sandy-floor threshold remain decoding-compatible but do not
control profile-6000 displacement. `rocky_floor_height` and `morphology` are omitted in the
active preset. There is no new geological fault-floor target.

### `erosion`

| Key | Default | Meaning |
| --- | ---: | --- |
| `enabled` | true | Enables the fixed recession/weathering/incision pass. |
| `minimum_relief` | 18 | Relief threshold for major cliff response. |
| `probe_distance` | 18 | Far external-envelope sampling radius. |
| `maximum_recession` | 6 | Maximum major horizontal roof recession. |
| `wind_strength` | .42 | Wind-facing contribution to major recession. |
| `fracture_strength` | .58 | Structural-fracture contribution to major recession. |
| `soft_multiplier` | 1.35 | Soft-rock susceptibility; medium is fixed at 1. |
| `hard_multiplier` | .58 | Hard-rock susceptibility. |
| `very_hard_multiplier` | .28 | Very-hard-rock susceptibility. |
| `surface_strength` | .34 | Ordinary roof/face weathering response. |
| `coarse_scale` | 18 | Coarse surface-weathering noise scale. |
| `detail_scale` | 6 | Detail surface-weathering noise scale. |
| `surface_retreat` | 4 | Additional horizontal recession bound/top-weathering amplitude. |
| `incision_scale` | 1 | Exposed structural-fracture incision multiplier. |
| `fault_weakness` | .7 | Damage-zone amplification of erosion. |
| `edge_threshold` | .32 | Start of major external-face exposure response. |

These controls replace root-level legacy `erosion`; legacy `base_alignment`, `erosion`,
`front_shell_cleanup` and `lithology.talus` are absent from the active JSON. Enabling legacy
erosion/cleanup/deposit groups in a 6000 profile is rejected. Fracture `minimum_rock_height`
and `minimum_massif_weight` remain legacy-only activation controls; structural fractures do
not gate on them. Mineralization metadata remains available for future volumetric work.

### `talus`

| Key | Default | Meaning |
| --- | ---: | --- |
| `enabled` | true | Enables erosion-derived deposits. |
| `yield` | .4 | Source removed-height to potential debris-height ratio. |
| `maximum_thickness` | 4 | Source supply/deposit height cap before taper. |
| `reach` | 16 | Maximum Euclidean source distance. |
| `minimum_erosion` | .75 | Minimum source rock removal to supply debris. |
| `minimum_relief` | 8 | Minimum source-to-recipient external height difference. |

## Diagnostics and validation

`/dune terrain inspect [x y z]` reports production R0/S/Re/H, regional rock, uplift, throw,
structural Y, external relief/normal, incision/major/surface erosion, sediment thickness,
talus source, and actual composed material. `/dune geology` uses the same report for 6000;
`/dune geology profile` reports the new settings. Legacy-only diagnostics remain on legacy worlds.

`validateBuriedRock` is build-blocking and is also reached by `test`. It checks continuous
geology/bedrock, burial and exposure, graded sediment over variable rock, independent sediment
under changed regional rock, recognizable uplift, structural lithology shifts, signed faults
and negative/chunk-edge continuity, external normals, talus causality, bounded caches, query
order, adjacent complete 16x16 tiles, composer/height agreement and legacy isolation.
The real Minecraft NoiseColumn buffer/writer is exercised with a registry-free sentinel palette
through all 384 Y levels, including above the old 129-layer substrate. Heightmap-predicate
handling is tested separately. The production chunk/base writers are structurally asserted
to call the same tested composer/palette. This is not a running-Minecraft block integration test.

All existing deterministic tests remain. `ArrakisProfileValidation.loadProfile` now reads the
exact baseline JSON from `src/test/resources/terrain/arrakis_5148.json`; historical numerical
fingerprints and shell-cleanup assertions still test 5148. The new suite uses
`loadDevelopmentProfile`. No historical test or expected fingerprint was deleted or weakened.
The legacy fixture's Git blob hash is `e8f0dbb6780031b65a02c90b9f3f6d7fbc6a9551`, identical
to the starting HEAD's development preset; no prior JSON tuning was lost.

### Executed checks

- `./gradlew.bat compileJava` and `./gradlew.bat compileTestJava`: passed during integration.
- `./gradlew.bat test`: passed after the final code changes; the new harness ran in about 2.1s.
  An earlier fixture assumed default-valued codec fields were always emitted; that test-only
  assumption was corrected before these passing results.
- `./gradlew.bat clean build`: final run passed in 7m35s, reaching `validateBuriedRock`,
  `validateArrakisTerrain`, `validateArrakisEvaluator` and `validateDunePrototypeState`.
- Legacy erosion fingerprint remained `8eddb8f453b631f6`; saved evaluator fingerprint remained
  `485af85209e2da18`, including historical reconstruction `624f66b5a25a22a3`.
- `git diff --check`: passed. Git's Windows line-ending conversion notices are informational.
- Inspected the packaged JAR: mod version `0.6.0-dev.1`, embedded development profile `6000`.
- No client/server, screenshot or DH run was performed. No commit, push or merge was made.

## Seed-0 inspection points

These values are analytical, not observations from generated screenshots:

| X/Z | R0 | S | Re | Purpose |
| --- | ---: | ---: | ---: | --- |
| 0/0 | 48.42 | 64.00 | 48.42 | Dig below a level basin into continuous geology. |
| 3057/150 | 150.74 | 61.69 | 110.90 | Inner recession slope and debris. |
| 3060/150 | 182.62 | 61.69 | 138.10 | Neighboring inner wall silhouette. |
| 3100/150 | 220.88 | 61.75 | 220.22 | Inner plateau above the cliff. |
| 3400/0 | 214.11 | 61.88 | 213.81 | Main uplift and exposed strata. |
| 4096/0 | 170.79 | 62.31 | 162.31 | Outer wall recession. |
| 9000/9000 | 29.64 | 87.50 | 29.64 | Independent erg dunes over buried geology. |

Also retain 3059/150, 3050/190, 4098/0, 4095/0 and earlier reference areas 3001/464,
3053/190, 657/3306 and 2553/1706. Compare overall wall position, height, irregularity and
silhouette, not old cleanup outcomes. Earlier photographic orphan fixtures at 3067/106 and
3089/173 include seed `-5640511200611798902`; do not conflate them with Seed 0.

## Status and dev.2 priorities

**Fully implemented:** production continuous geology, independent sediment, uplift/throw,
shifted lithology, analytical exposure/recession, causal bounded deposits, shared composition,
profile migration/isolation, diagnostics and automated invariants.

**Partial:** preserving the 0.5.14.8 *visual* character. Its useful mathematics was migrated,
but the result is a heightfield, not per-Y undercut survival. Analytical tuning preserves wall
scale; no client screenshots or Distant Horizons throughput validation are claimed.

**Deferred:** overhangs, caves, sealed water, physical collapse, exact sediment conservation,
complex tectonic mechanics and final gameplay balancing.

For dev.2: visually compare both wall sides and fracture outlets in fresh worlds; tune roof
recession and uplift rather than add repair filters; inspect stratum/calcite exposure after
structural shifts; refine colluvium transport/coverage; measure actual chunk/DH costs before
changing cache limits; consider a second fixed exposure pass only if visual evidence warrants it.
