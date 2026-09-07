# 0.6.0-dev.2 — Shield-Wall erosion morphology and review fixes

Baseline: `main` at `82e34c6409fac49dc2dc4f479142e69b89ccc3d6`
(`Add automated Arrakis dev world launcher`). Mod version remains **0.6.0-dev.2**;
terrain profile remains **6000**. The review fixes continue the existing uncommitted dev.2
implementation: 14 modified tracked files and four untracked files at the start of the fix.
No reset, commit, merge or push was performed.

## Status and scope

Implemented and analytically validated: coherent sector/mesoscale recession, wandering gullies,
downslope source-region colluvium, slope eligibility independent of the old cliff flag,
shoulder/profile erosion, resistance through the reached depth, production diagnostics and
final-surface morphology tests.

**Minecraft visual acceptance remains pending.** Analytical surface previews were inspected,
but no Minecraft client or fresh game world was launched during the review fixes. The previews
are heightfield diagnostics, not game/Distant Horizons screenshots or proof of 0.5.14.8 visual
parity. Explicit fault naturalization, secondary cracks, overhangs and subsurface systems remain
deferred.

## Why erosion was weak, including the initial dev.2 implementation

The historical 0.5.14.8 `EscarpmentErosionField` and `RockSurfaceErosionField` tested a face at
each Y, allowing material and vertical noise to change its survival independently of the roof
slope. Dev.1 preserved useful salts and responses but replaced this with a short downhill
`R0` lookup. Height loss becomes roughly horizontal distance times slope, and can vanish on a
shoulder. The 6-block major and 4-block surface budgets do not recover the old face geometry.

Initial dev.2 added broad/gully potentials, but independent review found four remaining gaps:

1. `WallErosionMorphology` required the old `face.exposed()` flag. That flag rejects steepness
   at or below 0.45, bypassing the new field's intended gentler-slope response.
2. Broad/mesoscale recession still depended on one displaced roof point. At 3100/150 it reported
   13.25 blocks of mesoscale recession but changed the dev.1 roof by only about 0.07 blocks.
   In contrast, some cliff-foot columns lost more than 100 blocks.
3. Improvement statistics selected columns with active erosion before computing their means.
   Noise-potential correlation and retained high columns did not establish final visible
   channels, bays or buttresses.
4. Resistance sampled only the fracture-incised roof and 8/20/40 blocks below it, even when
   recession passed into deeper resistant formations.

One-block bilinear interpolation is not the primary smoothing cause. No second erosion pass
is needed to address these representation and eligibility gaps.

## Unchanged architecture

```text
regional geology + tectonic uplift + signed fault displacement -> R0
independent basin sediment / dunes                            -> S
R0 and max(R0,S) neighbors -> analytical exposure
  + coherent erosion potentials + displaced lithology         -> Re (one fixed pass)
Re + exposed erosion supply -> coherent downslope deposits    -> C
bedrock + continuous geology through Re + sediment + deposits  -> final column
```

Raw geology, 36/48-block structural ramps, basin radii/datum, dunes, faults, basement,
`geologicalY = worldY - structuralDisplacement`, continuous rock composition and height/base-
column integration are preserved. Profile 6000 cannot enter legacy repair stages. No arbitrary
occupancy holes, forced vertical frontage, hard-cliff shell, basal concealment or cleanup pass
was introduced. The historical dev.1 documentation and frozen dev.1 fixture are unchanged.

## Sector recession, slopes and shoulders

The deterministic cylindrical sector/mesoscale pattern is retained: nominal scales 180, 86
and 50 blocks, with domain warping and coherent radial variation. Existing structural contact
information selects inner/outer identity and alignment, not a prescribed cliff location.
Recession follows the measured external-envelope normal.

Externally exposed rock is authorized by `R0 > S` and continuous envelope/ownership responses,
without requiring the old steep-cliff flag. The morphology steepness response fades in from
0.05 to 0.30; relief fades in over 4–18 blocks. Flat envelopes and buried roofs still receive
no new morphology. Low-potential sectors and resistant beds can retain projections.

The eight cardinal exposure probes remain; the far distance is 48 blocks with current defaults.
Inside the same evaluation, three raw-roof probes along the measured downhill normal sample
one-third, two-thirds and all of that distance. Their positive secant slopes are averaged with
weights 0.2/0.3/0.5. This sees a shoulder's downhill relief as well as shorter slopes followed
by rising ground. A flat profile contributes zero.

The sector and mesoscale horizontal budgets are converted to vertical erosion work using that
profile slope. Work is the greater of this profile contribution and the original endpoint loss.
Thus recession can reshape retained shoulders without requiring the short offset to cross the
toe. This constructs no target cliff or guaranteed frontage; both the potential and raw profile
vary spatially.

Sector/mesoscale budgets remain capped at 32/16 blocks in the preset. With legacy weathering,
the maximum horizontal recession lookup remains 58 blocks. These are input sampling/work
budgets, not a claim that the final visible contour retreats by exactly that distance.

## Gullies and resistance through depth

Gullies retain jittered wrapped angular cells, two wandering scales, variable width and depth,
a fading continuity envelope and occasional merging branches. They generally follow structural
downslope rather than a simulated drainage network. Spacing remains 44 blocks and nominal
gully work 24 blocks before existing fracture/fault modulation.

Recession work and gully work are combined and integrated once through the displaced material
below the already weathered roof. Each reached interval is at most four blocks deep, with
linearly interpolated inverse susceptibility and a continuous analytical inverse for the last
partial interval. At most 88 material samples cover the entire configured -48..300 range;
ordinary shallow erosion stops much earlier. This is a bounded integration of erosion work,
not per-Y occupancy, repeated exposure or an iterative world simulation.

Existing soft/medium/hard/very-hard controls are reused (1.35/1/.58/.28). Integration bounds
susceptibility to .05..1.6. A deeper hard formation consumes more work and slows lowering when
encountered, including below the former 40-block window. The final roof is fractional and is
not snapped to sample depths or mandatory bed-height steps. Sub-four-block formations can be
under-resolved by this approximation; exact physical material transport/collapse is deferred.

The achieved removal is attributed proportionally to recession and gully work for diagnostics.
Actual gully influence is additionally tested by disabling gully work and comparing the final
visible surfaces; the attribution alone is not used to claim a visible channel.

## Talus coherence and supply

The initial dev.2 correction is retained: fixed four-block source nodes and smooth compact
kernels replace the dev.1 recipient-shifted eight rays and strongest-source height selection.
A fixture reproduces a source at 8/0 being seen by recipient 0/0 but missed by 0/1 in dev.1.
The coherent source-region fixture deposits across 25/25 neighboring recipients.

The review fix also permits newly eroded slopes to supply deposits even when the old steep-
cliff flag is false. Sources still require actual erosion, exposure above S and the existing
recipient drop/direction requirements. Source blending, reach, thickness, clast selection and
material placement otherwise remain unchanged. No deposit connectivity cleanup was added.

Four-block source sampling can miss narrow features; rounding, recipient relief and source
material changes can still produce patch edges. Absence of every in-game gravel post is not
claimed. Talus is not mass-conserving and remains downstream of rock erosion.

## Settings and compatibility

No additional controls, preset changes or version/profile bump were needed for the review fixes.
The original dev.2 opt-ins/settings remain:

| Field under `buried_rock` | Missing-field default | Preset | Range / meaning |
| --- | --- | --- | --- |
| `erosion.morphology.enabled` | false | true | Enable morphology and extended exposure halo. |
| `erosion.morphology.sector_scale` | 180 | 180 | 80–600; broad along-wall scale. |
| `erosion.morphology.sector_recession` | 32 | 32 | 0–64; sector horizontal work/sampling budget. |
| `erosion.morphology.mesoscale_recession` | 16 | 16 | 0–32; mesoscale horizontal work/sampling budget. |
| `erosion.morphology.gully_spacing` | 44 | 44 | 16–128; nominal channel spacing. |
| `erosion.morphology.gully_depth` | 24 | 24 | 0–64; base vertical erosion work. |
| `talus.coherent_sources` | false | true | Blend fixed source-region influence. |

All numbers retain finite/range-validating codecs. Existing erosion/talus enable flags apply.
Both new opt-ins false preserve dev.1 behavior; profile 5148 remains isolated.

**Use a newly created Seed-0 world for the fixes.** Existing morphology-enabled dev.2 saves
also use the corrected algorithm for newly generated chunks; previously generated chunks are
unchanged. Keeping profile 6000 is intentional during this development stage. Reopening an old
world does not regenerate its terrain, and adjacent old/new chunks can differ.

The automated launcher files, `ArrakisDevWorldLauncher`, Gradle launch integration, normal
`runClient` behavior and JVM settings are untouched. No save was deleted or modified.

## Diagnostics

`/dune terrain inspect` and `/dune geology` retain their production-evaluator basis. The old
face flag is now labeled `steep-cliff` to distinguish it from exposed-slope eligibility.
Horizontal sector/mesoscale budgets are explicitly labeled as budgets; `recession-removal`
reports achieved vertical morphology removal alongside attributed gully removal.

`removed = incision + major + surface + gully`. Recession removal is already included in
`major`; do not add it again. Lithology response is achieved morphology removal divided by its
work budget, including any geological-floor limit. Talus tendency/coherent status and the
MORPHOLOGY stage counter remain available.

## Validation and measurements

All dev.1 assertions are retained. New review regressions cover:

- a genuinely exposed planar slope rejected by the legacy cliff flag;
- buried and flat-envelope controls;
- meaningful production roof lowering at the reviewed 3100/150 shoulder;
- a resistant formation first reached 60 blocks below the surface;
- vertical translation invariance and continuous/monotone resistance integration;
- independently selected exposed slopes and shoulders, including zero-effect samples;
- gully-on versus gully-off **final visible surfaces**, neighboring continuity and direction;
- an actual Y150 height-contour transect measuring differential retreat and spatial coherence.

Original active-field statistics are kept only as supplementary diagnostics. Determinism,
negative coordinates, cache saturation/reverse order, chunk equality, solid-rock composition,
legacy isolation, original pattern coherence and talus-source validation still run.

Final Seed-0 independent ring sample: angles every 6 degrees and radii 2950–4300 every 25 blocks.
Selection uses raw rock above S+12 and physical massif weight >.45, before looking at erosion.
There are **2,250 samples**, including plateaus. Mean visible roof change from dev.1 is **6.02**.
Gentler slopes (envelope steepness .15–.45): **59/110** change by more than one block.
Shoulders (near slope <.1, envelope steepness >.6): **115/124** change by more than four blocks.
Low-potential sectors and resistant rock are allowed to retain their height.

**62** sampled gullies lower the visible surface by more than two blocks; **57** retain at least
one block of effect two blocks downslope. Mean absolute along/across differences have ratio
**0.221**, measuring the final surface rather than just the noise potential.

Fixed inner-wall window X2980–3200/Z-128–384, sampled every four blocks: the actual Y150 contour
retreats **16.71** blocks on average, with **11.05** standard deviation. Mean changes over 4/64
along-wall blocks are **2.22/13.20**, supporting coherent recessed and projecting sectors.
These are analytical geometry measurements, not rendered visual acceptance.

| Seed-0 X/Z | Initial dev.2 Re (review) | Fixed Re | R0 | S |
| --- | ---: | ---: | ---: | ---: |
| 3057/150 | 50.87 | 65.74 | 150.74 | 61.69 |
| 3060/150 | 47.55 | 80.11 | 182.62 | 61.69 |
| 3100/150 | 220.15 | 203.48 | 220.88 | 61.75 |
| 4096/0 | 46.03 | 84.95 | 170.79 | 62.31 |

This reduces excessive cliff-foot excavation in these cases while increasing shoulder change.
The basin and far-erg controls remain unchanged. The northwest reference -1240/-3900 now has
79.45 blocks of removal; its local window remains an auxiliary fixture, not a coverage claim.

Commands from the repository root:

```powershell
.\gradlew.bat test
.\gradlew.bat clean build
git diff --check
```

`test` passed in 23 seconds; the buried-rock harness executed in 21.37 seconds, including the
new geometry checks. The clean build and final whitespace/package checks are recorded below.

`clean build` passed in **7 minutes 20 seconds**. All reached validators passed:
`validateBuriedRock`, `validateArrakisTerrain`, `validateArrakisEvaluator` and
`validateDunePrototypeState`, followed by `test`/`check`. Gradle reused valid compilation and
test cache entries; the custom validation harnesses executed. The saved-profile evaluator
fingerprint remains `485af85209e2da18`, with active and saved-profile order checks passing.

`git diff --check` passed; the four existing untracked dev.2 files were separately checked
for trailing whitespace. Packaged metadata and preset were verified: artifact
`build/libs/minecraftdune-0.6.0-dev.2.jar`, mod version `0.6.0-dev.2`, profile `6000`, morphology
enabled and both erosion classes present. HEAD remains the starting launcher commit.

## Performance and limitations

New production work is bounded: three downhill raw-roof probes and depth-dependent material
integration replace the initial four shallow material samples. No global caches, composed-
neighbor recursion or extra exposure pass were added. Existing operation-local caches remain.

Warmed analytical two-chunk measurements after fixes were approximately **18.19 ms**, compared
with roughly **15.21 ms** for the initial dev.2 review run and **154.73 ms** for dev.1 in the final
standalone run. The additional cost in this small sample is about 20%; separate runs and JVM
noise limit precision. These numbers exclude block writes/rendering and do not establish DH
throughput or a whole-generator speedup. No controlled DH benchmark was run.

Inspect a fresh world at nearby/full-detail distances, especially the listed inner/outer
coordinates, the northwest wall, and the fixed inner-wall window. Additional reference points
remain 3400/0, 657/3306, 2553/1706, 3001/464, 3053/190 and -2200/-1900. Do not diagnose DH grids or
incomplete LOD transitions as generation defects without nearby confirmation.

Before further feature work, obtain in-game acceptance of bay/shoulder shape, contour breakup,
gully connections, material-controlled projections and talus. Explicit major-fault surface
naturalization and a separate shallower-joint population remain later work.

## Files changed by the review fixes

- `WallErosionMorphology.java`: exposed-slope eligibility.
- `RockErosionField.java`: downhill profile, depth integration and achieved-removal diagnostics.
- `BuriedRockTerrain.java`: erosion supply from newly eroded exposed slopes.
- `ArrakisTerrainCommand.java`: distinguish budgets, cliff flags and actual removal.
- `BuriedRockMorphologyValidation.java`: slope/depth and final-surface regression coverage.
- This report, `PATCH_NOTES.md`, `README.md`, `README-Arrakis-Dev.txt` and
  `docs/ARRAKIS_TERRAIN_PROFILE.md`: corrected behavior and validation guidance.

The rest of the existing dev.2 implementation, including settings, version, preset, coherent
talus kernels, launcher and original architectural validation, is preserved.
