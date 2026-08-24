# Minecraft: Dune patch notes

## Minecraft: Dune 0.5.14.6 - Orphan Remnant Suppression

### Exposed-face connectivity cleanup

- Added `OrphanRemnantFilter`, a final removal-only support check after the existing major and
  surface erosion occupancy fields.
- The filter targets the remaining vertical artifact where a narrow resistant column is
  supported from the base but detached by an air gap from the main cliff at the same Y.
- A survivor is retained when it has immediate contiguous inward rock support, or can reach an
  inward-supported block through a short contiguous lateral path.
- Source defaults use `inward_support_depth=1`, `lateral_search_radius=2`,
  `minimum_height_above_base=5` and `minimum_face_relief=24`.
- The protected base/fault-floor region, small formations, attached ribs, large connected
  overhangs, existing undercuts and talus remain outside or pass through the filter.
- Support samples use the raw deterministic erosion result rather than recursively filtered
  neighbors, preserving chunk-order independence.

### Additional vanilla lithologies

- Added smooth basalt as HARD, red sandstone as SOFT and terracotta as MEDIUM.
- Smooth basalt forms hard margins around very-hard basalt sheets.
- Red sandstone is a coherent soft variant of sandstone units.
- Terracotta forms coherent medium-strength clay-rich host units.
- Added the serialized top-level `additional_materials` group rather than expanding the
  already-16-field material-palette codec beyond DataFixerUpper's arity limit.
- Missing `additional_materials` decodes disabled, so old serialized worlds retain their old
  lithology selection in newly generated chunks.

### Compatibility and scope

- Added optional `erosion.orphan_remnants`; missing data decodes disabled.
- Global erosion strengths, scarp widths, fault floors, dune tuning and 0.5.14.5 scarp
  roughness remain unchanged.
- Added deterministic checks for isolated-remnant removal, direct/lateral attachment, protected
  base behavior, relief gating and the three new material resistance tiers.
- Source terrain profile is `5146`; project version is `0.5.14.6`.
- Broader vertical-face modulation remains deferred unless Seed 0 still demonstrates a separate
  morphology problem after this cleanup.
- Subsurface geology remains planned for 0.5.15.

## Minecraft: Dune 0.5.14.5 - Scarp Roughness & Wall Continuity

### Coherent Shield Wall boundary roughness

- Added two-scale lateral warping to the physical inner and outer Shield Wall scarp positions.
- The source profile uses a 150-block / 7-block broad component and a 42-block / 2.5-block
  detail component.
- Warping shifts the cliff boundary in X/Z; it does not add per-block vertical height noise.
  Plateau mass and the steep 36/48-block structural scarp widths remain intact.
- Runtime erosion permission now reads the exact seeded physical massif envelope stored by
  `MacroGeologyField.Sample`, so warped scarp geometry and erosion authorization stay aligned.

### Regional fault wall continuity

- Added `wall_variation_scale` and `wall_variation` to the existing nested fault morphology
  settings. The source profile uses 90 blocks and +/-3 blocks.
- Fault wall width now changes coherently along each fault instead of remaining an identical
  14-block cross-section for long runs.
- The same low-amplitude structural signal can expand the protected core edge slightly, but
  never narrows it below configured `core_width`; absolute 0.5.12 fault-floor elevation remains
  unchanged.
- Existing broad/medium fault-centerline meander is preserved.

### Compatibility and scope

- Missing massif roughness strengths and fault wall variation decode to zero, preserving
  serialized 0.5.14.4 geometry.
- Global erosion strength, lithology, talus, dunes, scarp widths and fault floor values are
  unchanged.
- Added deterministic/bounds validation for massif boundary offsets and fault cross-section
  variation.
- Source terrain profile is `5145`; project version is `0.5.14.5`.
- Unsupported/floating resistant remnants and eight-direction face probes remain deferred.
- If seed-0 is satisfactory after this pass, above-ground erosion architecture should be
  frozen and 0.5.15 can begin subsurface geology.

## Minecraft: Dune 0.5.14.4 - Structural Face Erosion Coupling

### Inner Shield Wall erosion follows the physical scarp

- Fixed a stale permission mismatch exposed by 0.5.14.3: the new 36-block inner physical
  Shield Wall could finish before the old 150-block geographical massif weight became strong
  enough to authorize erosion.
- `ScarpMorphologyField.massifErosionPermission` now accepts either the broad geographical
  massif weight or the physical 0.5.14.3 massif envelope.
- `RockFaceExposure`, `EscarpmentErosionField` and `RockSurfaceErosionField` use that shared
  permission. Height-derived exposure remains the actual cliff detector.

### Fault floors are protected separately from fault walls

- Removed the old broad fault-carve exclusions (`>0.20` / `>0.86`) that accidentally protected
  most of the new 14-block physical fault wall from erosion.
- Added a continuous fault erosion permission: carve masks up to 0.90 have full permission,
  0.90-0.995 blend into protection, and the full-depth core remains protected.
- The 0.5.12 absolute rocky/sandy fault-floor behavior is unchanged.
- Major and surface erosion now weather the physical fault wall while fading cleanly into the
  protected structural floor.

### Scope

- No erosion strength, scarp-width, lithology, dune or fault-floor tuning values were changed.
- Added deterministic validation for inner physical-scarp permission and fault core/wall
  separation.
- Source terrain profile is `5144`; project version is `0.5.14.4`.
- Unsupported/floating resistant remnants remain deferred until face coverage is stable.
- Subsurface geology remains planned for 0.5.15.

## Minecraft: Dune 0.5.14.3 - Scarp & Fault-Wall Morphology

### Physical scarps are no longer province fades

- Added `ScarpMorphologyField`, a deterministic structural precursor between broad macro
  geography and the existing 0.5.14.x erosion stack.
- The Shield Wall's broad province intervals remain available for geological weighting, but
  they no longer have to be the physical 150-500 block-wide mountain slope.
- Added `massif.scarp_morphology_enabled`, `inner_scarp_width` and `outer_scarp_width`.
  The source profile uses 36/48 block physical scarps, making both inner and outer Shield Wall
  faces steep enough for `RockFaceExposure` while leaving erosion responsible for detailed
  ribs, recesses, undercuts and talus.
- Existing massif start/full and outer_start/outer_end values are preserved.

### Regional fault wall / toe separation

- Decoupled the 0.5.12 guaranteed fault core from the complete `core_width -> outer_width`
  percentage-depth ramp.
- Added `faults.morphology.wall_width`; the source profile uses 14 blocks for the physical
  canyon wall.
- Added `faults.morphology.toe_depth`; the remainder of `outer_width` becomes only a shallow structural
  shoulder (4 blocks in the source profile) instead of a tens-of-blocks-high smooth boot.
- Rocky and sandy absolute fault floors remain unchanged.
- The narrower depth transition also leaves the exposed wall outside the protected fault core,
  allowing the existing escarpment/surface erosion fields to weather it normally.

### Compatibility and validation

- Missing scarp controls and `faults.morphology` decode to legacy morphology; serialized
  0.5.14.2 worlds do not
  silently change geometry at new chunk borders.
- Preserved lithology, fissures, granite/deepslate, dunes, talus and current erosion strengths.
- Added deterministic checks for inner/outer structural widths, full fault-core depth,
  fault-wall termination, shallow shoulder containment and outer-width bounds.
- Source terrain profile is `5143`; project version is `0.5.14.3`.
- Subsurface geology remains deferred to 0.5.15.

## Minecraft: Dune 0.5.14.2 — Exposed Cliff Face Erosion

### Height-derived face detection

- Added the shared deterministic `RockFaceExposure` field. It measures short-range and
  far-range cardinal terrain heights once per X/Z column and reports local relief, steepness,
  outward normal, high/low elevations, vertical face interval and bounded distance behind the
  physical edge.
- Fixed the architectural cause of smooth Shield Wall faces: `rockFormationMask` is no longer
  used as the ordinary cliff detector. A formation mask remains geological eligibility data;
  actual neighboring height differences determine whether rock is physically exposed.
- Reused the shared height sample in both major escarpment and ordinary surface erosion,
  removing the duplicate neighbor probing formerly embedded in `EscarpmentErosionField`.
- A 100+ block wall can now remain a strong erosion candidate even when its formation mask is
  approximately `1.0`.

### Whole-wall differential recession

- Extended ordinary surface erosion from shallow top trimming to the complete measured
  `face_low_y .. face_high_y` interval.
- Added coherent low-amplitude 3D recession down exposed walls. Retreat is compared with the
  column's short-probe distance behind the edge, keeping the pass removal-only, shallow and
  bounded by `erosion.surface.max_retreat_blocks`.
- Lithology now changes the vertical silhouette throughout a cliff: soft sandstone, tuff and
  limestone retreat farther; medium stone/calcite stay near baseline; hard andesite, diorite,
  granite and deepslate form smaller ribs; very-hard basalt and blackstone remain most resistant.
- Inner Foreland, Broken Rock and transition remnants use the same physical exposure test at
  their existing reduced `small_rock_strength` / `broken_rock_strength` permissions.

### Fissures, faults and compatibility

- Kept local fissure depth authoritative while applying coherent, resistance-aware widening
  down the existing wall interval. The current `fissure_multiplier` and intersection signal
  provide the extra bounded recession.
- Preserved strong regional fault cores, 0.5.12 absolute fault floors and sand-pass corridors;
  eligible exposed walls can weather without modifying their protected floors.
- Preserved the 0.5.14.1 granite and deepslate bodies and all current serialized tuning.
- Added concise `/dune geology` face diagnostics: exposure, relief, low/high Y, steepness,
  outward normal and ordinary face-erosion strength.
- Added deterministic validation for a mask-independent 140-block wall, full-height recession,
  resistance ordering, fissure widening/depth bounds, fault-floor protection and chunk-order
  independence.
- No terrain JSON parameters were added. The source profile version is `5142`; project version
  is `0.5.14.2`.

## Minecraft: Dune 0.5.14.1 - Erosion Coverage & Rock Surface Morphology

### Continuous exposed-rock erosion

- Added `RockSurfaceErosionField`, a cheap deterministic removal-only pass that complements
  the rare/large 0.5.14 escarpment events.
- Ordinary massif faces now receive low-amplitude coherent recession instead of reverting to
  long mathematically smooth walls between major erosion features.
- The pass also affects foreland boulders and Broken Rock remnants at independently tunable
  strengths, breaking their silhouettes without scaling the large cavern/undercut system down
  onto every small rock.
- Surface erosion remains bounded to a small configured retreat and is suppressed inside strong
  regional-fault cores and sand passes.
- Material resistance is evaluated per Y. Soft units recess farther while hard/very-hard units
  survive as small ribs, benches and ledges, so lithology changes physical silhouette rather
  than only block color.

### Fissure-wall weathering

- Local massif fissures receive a separate erosion multiplier around their existing walls.
- Existing fissure depth remains authoritative; 0.5.14.1 primarily widens and roughens exposed
  fissure walls instead of deepening every crack toward the crust.
- Soft units can widen beyond the designed fissure edge while resistant units remain narrower.
- Fracture intersections retain an additional bounded boost.

### Granite and deepslate

- Added vanilla granite as a coherent HARD plutonic intrusion alongside andesite/diorite.
- Added `granite_fraction` to tune granite's share of already-qualified intrusive bodies.
- Added vanilla deepslate as HARD ancient basement lithology.
- Added `deepslate_top_y` and `deepslate_warp_strength` so deep cuts expose a coherent warped
  basement instead of random dark surface speckle.
- Added serialized `granite` and `deepslate` block identifiers.

### Serialized surface tuning

- Added optional `erosion.surface` settings:
  `enabled`, `strength`, `scale`, `detail_scale`, `max_retreat_blocks`,
  `fissure_multiplier`, `small_rock_strength`, `broken_rock_strength`, and
  `lithology_relief_strength`.
- Missing `erosion.surface` data decodes disabled for serialized 0.5.14 worlds.
- The 0.5.14.1 source preset explicitly enables the pass and bumps terrain profile to `5141`.
- Project version bumped to `0.5.14.1`.
- Full cavern/water generation remains deferred.

## Minecraft: Dune 0.5.14 — Escarpment & Differential Erosion

### Three-dimensional escarpment morphology

- Added `EscarpmentErosionField`, a deterministic removal-only morphology stage between the
  0.5.13 lithology/fracture fields and final talus/dune placement.
- Replaced eligible parts of smooth massif and large Broken Rock aprons with signed-edge,
  near-vertical faces without merely sharpening the macro height exponent.
- Added independent per-Y rock occupancy inside each existing rock envelope. A column can now
  contain bounded `rock / air / rock` geometry for shelves and negative-angle faces.
- Restricted candidates by local relief, source height, formation-edge strength and massif /
  faulted-margin / scaled Broken Rock permission. Small outer remnants retain simpler shapes.
- Kept erosion removal-only: it cannot add rock above the macro/fissure envelope, bridge a
  regional fault or sand pass, or grow detached floating sheets.
- Preserved the lowest native-rock layers and the existing downward rewrite into stone,
  deepslate or bedrock, so every surviving visible formation remains hard-crust connected.
- Preserved shallow one- and two-block native-rock outcrops instead of allowing the occupancy
  pass to erase them.

### Differential lithology and supported undercuts

- Applied erosion to the logical material at every Y instead of repainting morphology with
  unrelated surface noise.
- Kept medium stone/calcite host at the `1.0` retreat baseline; supplied relative retreat is
  `1.35` for soft sandstone/tuff/limestone, `0.58` for hard andesite/diorite, and `0.28` for
  very-hard basalt/blackstone.
- Hard units can form projecting benches, intrusive ribs and caprock. Very-hard sheets/bodies
  can stand sharply proud while adjacent soft units recess.
- Added coherent undercut patches only where the surface and two supporting layers provide a
  sufficiently resistant cap. The supplied profile caps differential/material boundary offset
  around the selected escarpment face at 6 blocks; the runtime safety clamp is 16. Steepening
  the broader smooth macro apron is not measured by that offset.
- Added bounded 3D face detail so contacts do not resolve as ruler-flat vertical planes.

### Fracture and wind interaction

- Added a bounded erosion halo around active local fissures and extra susceptibility at
  fracture intersections; resistant basalt/blackstone slots remain narrower than soft margins.
- Extended the fracture accumulator with a second-overlap/intersection signal. Intersections
  modestly increase fissure strength/depth while retaining the existing foundation floor clamp.
- Used that same intersection signal for stronger local cliff retreat and talus suitability.
- Attenuated fracture-driven face erosion below each fissure's design depth, preventing shallow
  cracks from weakening an entire cliff down to the crust.
- Added coarse wind exposure from the estimated outward face normal, local relief, the existing
  24-degree development wind and deterministic low-frequency shelter.
- Kept regional faults independent. Strong fault carving bypasses escarpment occupancy, so the
  0.5.12 absolute rocky/sandy floor model remains authoritative.

### Localized talus and scree

- Activated the existing serialized `lithology.talus` framework with gravel as the principal
  loose matrix, a 7-block maximum local thickness and an 18-block apron falloff.
- Combined escarpment strength, low-side face distance, coherent patch noise, wind exposure,
  fracture proximity and fissure outlets so talus forms localized aprons below scarps instead
  of blanketing plateau tops.
- Added coherent minority clasts from the adjacent source lithology. Tuff, limestone/calcite,
  stone/andesite and other nearby units therefore influence debris composition without
  decorative per-block speckle.
- Retained `minimum_fracture_strength` as a backwards-compatible JSON name; in 0.5.14 it also
  serves as the combined talus-suitability threshold, set to `0.44` in the supplied profile.
- Started talus above surviving rock and the highest full dune block. When gravel shares a Y
  with the optional fractional dune layer, the partial layer is omitted; full dune blocks remain
  below as support, so scree is not left floating on partial sand.

### Serialized profile and compatibility

- Added optional serialized `terrain.erosion` settings for candidate relief/probing, edge and
  vertical-face strength, wind/fracture exposure, resistance multipliers, bounded undercuts and
  reduced-scale Broken Rock application.
- A saved 0.5.13 generator that omits `erosion` decodes with the pass disabled. The 0.5.14
  source preset explicitly enables it, preventing an automatic morphology seam in older saves.
- Bumped the source terrain profile from `513` to `514` and the project version from `0.5.13`
  to `0.5.14`.
- Preserved all basin, foreland, massif, fault, sand-pass, Broken Rock, outer-transition,
  lithology/material, fracture and native-dune tuning except the intentional activation/tuning
  of the existing talus controls.

### Diagnostics, validation, and documentation

- Extended `/dune geology`, `/dune geology info` and `/dune geology sample <x> <z>` with
  surviving rock Y, exposed lithology/resistance, fissure intersection strength, escarpment
  activation/strength, local relief, maximum differential boundary offset (reported as maximum
  retreat), coarse wind exposure, fracture erosion, undercut potential and talus
  suitability/thickness.
- Extended `/dune geology profile` with the active erosion controls and resolved talus switch,
  depth and spread. X/Z diagnostics intentionally summarize the surface/face candidate; actual
  per-Y occupancy may contain an undercut below that surface.
- Added `validateArrakisTerrain`, a dependency-free deterministic smoke check covering profile
  compatibility, resistance order, pure-basin/open-erg exclusion, native dunes, bounded and
  explicitly located undercuts, fracture/no-fracture comparisons, supported localized talus,
  chunk-boundary seams, reversed evaluation order, 0.5.12 fault floors and hard-crust
  connection.
- Added `docs/ESCARPMENT_EROSION.md` with the generation order, support rules, complete JSON
  parameter table, performance model, diagnostic limitations and deferred systems.

### Preserved and deferred systems

- Generation remains deterministic from world seed + serialized profile + absolute coordinates,
  seamless across chunks, and direct to `ChunkAccess`; there is no iterative simulation,
  lighting update or post-generation `ServerLevel#setBlock` terrain pass.
- Preserved layered native dunes, Arrakis fauna, camera/screenshot tooling, the frozen finite
  `DuneSimulation` laboratory, NeoForge 21.1.248 and the current runClient ZGC/JVM settings.
- Deferred full caves/collapse chambers, extremely rare sealed water caverns, regional wind and
  dynamic sand transport, physical collapse, final texture art and a complete mesa/butte
  lifecycle. Limestone hosts and mineralized fractures remain available for 0.5.15.

## Minecraft: Dune 0.5.13 — Lithology & Fracture Framework

### Desert Hare and Muad'dib entity split

- Renamed the original hare-like Muad'dib test entity and its complete runtime/resource stack
  to `desert_hare` / Desert Hare.
- Preserved the Desert Hare's Rabbit-based behavior, sand preference, breeding, idle state,
  hop cycle, ground-sniff action, head-wiggle action, model geometry, texture, and original
  spawn-egg colors.
- Added a separate Muad'dib entity under the freed `muaddib_mouse` identifier using the
  32×32 Java model and texture exported to `blockbench/java/`.
- Added a lightweight synchronized hop, head tracking, and idle tail motion to the exported
  Muad'dib hierarchy; the source Blockbench project currently has no authored keyframes.
- Registered separate entity types, attributes, renderers, model layers, translations, item
  models, creative-tab entries, and spawn eggs. Each spawn egg now points directly to its
  corresponding entity type.
- The existing `muaddib_mouse` registry ID is intentionally reused by the new Muad'dib. Saved
  mobs or eggs carrying that old ID therefore resolve as Muad'dib; Desert Hare uses the new
  `desert_hare` and `desert_hare_spawn_egg` IDs.

### Muad'dib movement and Arrakis fauna refinement

- Rotated the complete exported Muad'dib model hierarchy 180 degrees so its visible facing
  direction agrees with Rabbit locomotion instead of making forward hops look backwards.
- Compensated head pitch for that rotated hierarchy so Muad'dib looks up toward nearby players
  instead of pitching farther downward as they approach; horizontal head tracking is unchanged.
- Raised Muad'dib's movement-speed attribute from Rabbit's `0.3` baseline to `0.9`, exactly
  three times the original value.
- Multiplied Rabbit's launch power by `1.5`. Under Minecraft's discrete gravity this produces
  approximately twice the original jump apex; doubling launch velocity itself would produce
  roughly four times the jump height.
- Added the featureless `minecraftdune:arrakis_desert` biome for newly created Arrakis Dev
  worlds. Its only natural creature entries are Muad'dib and Desert Hare, with no monster,
  ambient, underground-water, water-creature, water-ambient or axolotl entries.
- Registered valid on-ground placement rules for both desert entities and made full and
  layered Dune Sand valid Rabbit-family spawn surfaces.
- Added Arrakis-generator placement and finalization gates that reject every entity type
  except Muad'dib and Desert Hare from natural, patrol, event, structure, spawner, and other
  autonomous spawn paths. This also prevents vanilla nighttime spawning in older Arrakis
  saves whose serialized biome is still `minecraft:desert`. Commands, spawn eggs, buckets,
  dispensers, and breeding remain available for deliberate player/test activity.
- Kept the project and generator profile versions at `0.5.13` / `513` for this refinement.

### Native lithology

- Added optional serialized `terrain.lithology` settings with backwards-decoding defaults.
- Replaced uniform native stone with deterministic coherent 3D/stratigraphic rock units.
- Added geological roles and resistance classes:
  - soft: sandstone, tuff, limestone;
  - medium: background stone and calcite-bearing host;
  - hard: andesite and diorite intrusions;
  - very hard: basalt sheets and rare blackstone bodies;
  - loose: gravel talus/collapse material, never intact bedrock.
- Added warped strata, broad units/lenses, hard intrusions, rare limestone/blackstone bodies,
  resistant basalt sheets, and calcite bands without per-block decorative speckle.
- Added coherent detail and micro-detail to material selection and contact elevation so
  adjacent units form rough, interlocking boundaries instead of smooth ovals or planes.
- Added registry-based material identifiers. `create:limestone` is used when available in the
  current mod set and falls back to `minecraft:sandstone` without a compile-time Create
  dependency.

### Massif-top fractures

- Added optional serialized `terrain.fractures` settings with backwards-decoding defaults.
- Added an absolute-coordinate local fracture network separate from regional faults.
- Continuous warped primary trace families cross the exposed massif instead of beginning at
  finite seed points in the middle. Probabilistic tapered side branches may end internally.
- Retained the serialized `cell_size` name for compatibility but redefined it as primary-line
  and branch-node spacing; supplied defaults are now `520` spacing and `0.72` active lines.
- Default target geometry is approximately 1–12 blocks wide and 5–68 blocks deep, ranging
  from shallow cracks to slots and deeper chasm hazards.
- Fractures favor substantial exposed massif/faulted-margin rock and are suppressed on low
  foreland stones.
- Soft/hard resistance modestly changes width/depth now and exposes a stable model for the
  full 0.5.14 differential-erosion pass.
- Mineralization presence and abundance now vary by fissure and coherently along each trace.
  Calcite appears in intermittent horizontal wall bands instead of outlining entire crack
  walls and floors.
- Fissure floors retain at least one native-rock block above the Y64 base, and all visible
  rock remains connected through the former sand/sandstone layers to hard crust.

### Talus, caves, and future passes

- Added serialized talus definitions and a fissure talus-candidate hook using gravel as the
  principal loose material.
- Left local scree disabled and did not generate final large talus cones in 0.5.13.
- Did not add full caves or water. Rare limestone lenses and fracture/mineralization metadata
  are inputs for the later 0.5.15 dry/mineralized/collapse cavern and extremely rare sealed
  water-cavern pass.
- Did not add 0.5.14 undercuts, overhangs, negative-angle cliffs, or full escarpment erosion.
- Deferred general massif-top dents and other surface deformation to that later shaping pass.

### Diagnostics and documentation

- Extended `/dune geology info` and `/dune geology sample <x> <z>` with dominant logical and
  resolved lithology, resistance, limestone-host, intrusion/basalt/calcite, fissure strength,
  width/depth, activation and mineralization values.
- Extended `/dune geology profile` with active lithology/fracture settings and resolved
  limestone/talus blocks.
- Added a complete JSON parameter and design reference in
  `docs/LITHOLOGY_AND_FRACTURES.md`.
- Bumped the generator source profile to `513` and project version to `0.5.13`.

### Preserved baselines

- Preserved all current 0.5.12 user tuning outside additive lithology/fracture fields.
- Preserved the 0.5.12 absolute fault-floor behavior and `rocky_floor_height`.
- Preserved native dune settings, layered sand, cameras/screenshots, desert entities, the frozen
  finite `DuneSimulation` laboratory, NeoForge 21.1.248, and runClient ZGC/JVM settings.
- Generation remains direct-to-`ChunkAccess`, deterministic from world seed + profile +
  absolute coordinates, with no `ServerLevel#setBlock` terrain post-pass.

## Minecraft: Dune 0.5.12 — Fault Floor Consistency

0.5.12 is a focused correction to the native fault-depth model.

### Problem

In 0.5.11, fault depth was applied proportionally to the original rock height:

```text
new height = lerp(original height, low fault floor, fault mask * 0.96)
```

Even a nominally full fault retained 4% of the original massif height. On a ~200-block wall
that residual alone could leave several additional blocks, and slightly sub-core masks could
leave much larger shelves. The sandy-floor selection was also thresholded twice.

This made fault depth vary too strongly with the height of the rock it crossed.

### Changes

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

### Intended cross-section

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

## Minecraft: Dune 0.5.11 — Rock gradients, rooted geology, and interior dunes

- Preserved the user's current pushed 0.5.10 Arrakis terrain profile as the tuning baseline:
  - pure-sand basin radius 1500;
  - foreland end 3050;
  - massif start 3000 / outer end 4500;
  - six faults;
  - broken-rock range 4000–6650;
  - outer transition to 9000;
  - native dune spacing 512 and spacing variation 0.38.
- Added radial **foreland growth** so the first boulders near the basin are smaller and the
  surviving fragments become progressively larger toward `massif.start_radius`.
- Added `foreland.inner_height_scale`.
  - Controls the inner-edge vertical scale of large foreland fragments.
- Added `foreland.inner_threshold_boost`.
  - Raises both large-rock thresholds at the inner edge, shrinking their footprint/density;
    the boost fades to zero toward the massif.
- Added `foreland.growth_power`.
  - Shapes how early/late the foreland reaches full size.
- Micro-rock remains small but gains some height toward the massif.
- Changed Broken Rock size progression so decay starts at `broken_rock.start_radius` rather
  than waiting until `broken_rock.full_radius`.
- Added `broken_rock.size_decay_power`.
  - Controls how quickly large near-massif remnants transition toward small outer remnants.
- Added `native_dunes.foreland_weight`.
  - Enables low dune activity in sandy foreland gaps.
- Raised the supplied profile's `broken_rock_weight` from 0.12 to 0.22 for visible but still
  subordinate dune activity among broken-rock outliers.
- Native dunes remain locally suppressed by rock height, so they preferentially occupy sand
  between formations rather than growing through major rock bodies.
- Rooted all visible native geological formations into the underlying hard crust:
  - the generator scans downward from Y=64 until it finds stone/deepslate/bedrock;
  - sandstone and sand between that crust and a visible rock formation are replaced by stone;
  - ordinary sand-only columns keep the original Arrakis flat stratigraphy.
- Updated terrain profile version to 511 and mod version to 0.5.11.
- Added backwards-compatible codec defaults for all new JSON fields so 0.5.10 generator data
  lacking the new fields remains decodable.
- Updated the terrain profile documentation with detailed explanations of the new fields and
  the rock-foundation behavior.

## Minecraft: Dune 0.5.10 — Terrain profile + morphology tuning

- Added `ArrakisTerrainSettings` and serialized it in the `minecraftdune:arrakis_dev`
  chunk-generator codec.
- Added an explicit `terrain` object to the Arrakis Dev world preset JSON.
- Added `/dune geology profile` to report the main terrain parameters loaded by the world.
- Kept `/dune geology sample <x> <z>` as a computed diagnostic rather than configuration.
- Extended the Inner Rock Foreland to roughly 1150 blocks and added a second micro-rock
  field for more numerous 2–9 block formations between the existing larger knobs.
- Retained the successful main Shield Wall height scale.
- Kept the useful 0.5.9 fault width, but substantially increased lateral centerline meander
  with broad, medium and sinusoidal along-fault warp components.
- Made fault carving a final rock operation so Broken Rock outliers cannot form narrow
  transverse "fences" across already-carved basins.
- Added intermittent fault segments that cut fully to the Y=64 sand surface; other segments
  retain a very low resistant rocky floor.
- Extended the Broken Rock Desert from the previous ~4450 outer scale to roughly 5650.
- Split Broken Rock morphology into large and micro-remnant fields. Large formations become
  progressively rarer/lower outward while micro-remnants gain relative importance before
  also fading.
- Extended the low-remnant Sand–Rock Transition to roughly 6500 and moved full Open Erg
  suitability outward to roughly 6700.
- Increased **native world-generation transverse dune spacing** from 350 to 525 blocks.
- Preserved the finite `DuneSimulation` laboratory baseline at 350-block spacing.
- Preserved native sixteenth-layer dune surfaces and the existing 24-degree development wind.
- Deliberately deferred true escarpment/mesa geometry. The later erosion pass is expected to
  support ultra-hard remnant rock, near-vertical walls, undercut shelves and locally
  negative-angle/overhanging faces where softer material has been removed.
- NeoForge remains 21.1.248.

## Minecraft: Dune 0.5.9 — Geological provinces + native far-erg dunes

- Reduced the strict pure-sand Arrakeen reservation from 1000 to **800 blocks**.
- Added an independent **Inner Rock Foreland** from roughly 800–1100 blocks with sparse
  small formations rather than a miniature Shield Wall.
- Preserved the main Shield Wall's large 0.5.8 vertical scale while making the macro body
  more continuous.
- Added four long seeded, non-radial **fault-ravine traces**. Fault centers normally retain
  low rocky floors rather than becoming broad sand gaps.
- Added **two major seed-dependent sandy corridors** through the Shield Wall and broken-rock
  zone. Corridor centers fully suppress the provisional rock field.
- Changed the outer massif from a broad fade to a comparatively abrupt termination around
  the 3-km scale.
- Added an independent **Broken Rock Desert** outlier field after the main massif.
- Added a **Sand–Rock Transition** with lower/smaller remnant formations before the open erg.
- Added an **Open Erg** province, reaching full weight near 5250 blocks effective radius.
- Added `duneSuitability` to the macro-geology sample; broken rock contributes weak dune
  activity, the transition contributes more, and the open erg reaches full activity while
  tall rock suppresses dunes locally.
- Added `NativeTransverseDuneField`, a fast continuous world-coordinate implementation based
  on the calibrated transverse morphology rather than the finite iterative laboratory.
- Native transverse parameters start at: 350-block spacing, 0.18 spacing variation,
  3.0 ridge sharpness, 0.20 valley cutoff, 0.82 slope asymmetry, 30-block maximum height,
  24-degree wind, and sixteenth-block surface resolution.
- Native dune crests use `minecraftdune:sand` plus the existing fractional
  `minecraftdune:sand_layer` top surface.
- Updated `ArrakisChunkGenerator` so native rock is generated first and dune sand can bury
  low rock only where the analytic sand surface is higher.
- Added matching combined terrain logic to generator base-height/base-column queries.
- Added a far-erg fast path that skips expensive geological formation/fault calculations once
  all mixed-rock province weights are zero.
- Made `/dune geology` itself execute the terrain info command.
- Reworked geology command registration to merge the geology child through Brigadier's
  normal `/dune` registration path instead of mutating a previously looked-up command node.
- Expanded `/dune geology info` / `sample` output with province, fault, sand-pass and native
  dune diagnostics.
- Preserved the 0.5.8 native generator codec, tick-spread pregeneration manager, NeoForge
  21.1.248, ZGC runClient arguments, layered-sand assets, camera/screenshot tools, barchan
  prototype and frozen 0.5.6 transverse laboratory defaults.
- Did not yet add true strata, caprock, mesas/buttes, talus, yardangs, thermal weathering or
  terrain-projected regional wind.

## Minecraft: Dune 0.5.8 — Native Arrakis terrain generation

- Registered the `minecraftdune:arrakis_dev` chunk-generator codec.
- Added `ArrakisChunkGenerator`, extending vanilla `FlatLevelSource`.
- Preserved the 0.5.7 `MacroGeologyField` mathematics and provisional plain-stone output.
- Captured the real world seed in `ChunkGenerator#createState` and continued using absolute
  X/Z coordinates for deterministic macro terrain.
- Moved macro-rock creation into `fillFromNoise`, writing directly to `ChunkAccess`.
- Updated generator base-height and base-column queries to include native macro relief.
- Changed the Arrakis Dev world preset from `minecraft:flat` to
  `minecraftdune:arrakis_dev` while retaining the existing flat settings object.
- Converted `/dune geology generate` into native pregeneration of the player's current
  aligned 256 x 256 geology tile.
- Kept `/dune geology generate_initial` as a 100 vanilla-Minecraft-chunk / 1600-block
  pregeneration radius around absolute `(0,0)`.
- Kept `/dune geology generate_nearest <radius>` player-centered; radius 1 is 3 x 3 geology
  tiles.
- Large jobs now request ordinary `ChunkStatus.FULL` chunks and no longer run a second
  terrain-materialization pass.
- Retained `/dune geology generation status` and `cancel`.
- Changed `/dune geology clear` into a compatibility explanation because native terrain is
  no longer a removable post-generation layer.
- Existing 0.5.7 worlds do not migrate automatically; create a new Arrakis Dev world for
  native-generator testing.
- Preserved NeoForge 21.1.248, the existing runClient JVM arguments, empty third-party
  Gradle runtime dependency list, layered sand assets, debug cameras/screenshots, barchan
  prototype, and the frozen transverse 0.5.6 v1 defaults.
- Deferred the evaluated morphology changes (0–800 basin, 800–1000 sparse rock, faults,
  sandy passes, abrupt breakups and the additional outer mixed province) to the next pass.

## 0.5.7 — Macro geology foundation

- Added a deterministic `MacroGeologyField` evaluated from the world seed and absolute X/Z
  coordinates.
- Hard-reserved the first 1000 blocks from `(0,0)` as a flat Arrakeen / central basin.
- Added continuous weights for central basin, rock transition, Shield Wall / massif,
  eroded margin, and open desert.
- Added very-low-frequency radial boundary warping outside the protected basin.
- Added broad seed-dependent continuity lobes plus low-frequency formation fields so the
  Shield Wall permission field is not a circular annulus around spawn.
- Added a provisional macro base-elevation field with up to 176 blocks of rock relief above
  the Arrakis Dev Y=64 surface.
- Added `/dune geology info` and `/dune geology sample <x> <z>`.
- Added `/dune geology generate` and `/dune geology clear` for one aligned 256 x 256 tile.
- Changed `/dune geology generate_initial` to generate a 100 vanilla-Minecraft-chunk
  radius (1600 blocks) around absolute `(0,0)` for Distant Horizons-scale inspection.
- Changed `/dune geology generate_nearest <1..12>` to be centered on the player's current
  256 x 256 geology tile. Radius 1 is a 3 x 3 tile square, radius 2 is 5 x 5, etc.
- Added tick-spread large-area generation with `/dune geology generation status` and
  `/dune geology generation cancel` to avoid one enormous synchronous command tick.
- Kept the first materialized geology deliberately plain `minecraft:stone`; detailed strata,
  plateaus, escarpments, ridges, mesas, talus and erosion remain deferred.
- Preserved the frozen 0.5.6 transverse dune v1 implementation and defaults unchanged.
- Restored the Gradle dependency/repository configuration to the GitHub 0.5.6 baseline:
  no automatic third-party development mods are resolved by this project.
- Kept NeoForge at 21.1.248.
- Updated version metadata and active terrain documentation to 0.5.7.

## 0.5.6 — Transverse dune v1 baseline

- Froze the calibrated transverse generator as the v1 local dune-synthesis baseline.
- Kept the 350-block development crest spacing introduced in 0.5.5.
- Promoted the tested `slope_asymmetry` default from 0.60 to 0.82.
- Promoted the tested `interdune_cleanup` default from 0.30 to 0.40.
- Preserved all other transverse profile defaults, deterministic morphology/transport math,
  the validated wind convention (`0 = +X`, `90 = +Z`), and orientation behavior.
- Preserved the 0.5.4 layered-sand block, model, and texture assets unchanged.
- Preserved the existing fixed-camera and screenshot system unchanged.
- Left barchan generation unchanged.
- Deferred geological macro-height, shield-wall, rock-field, and terrain-projected wind work;
  none of the planned 0.5.7 framework is included in this release.
- Updated project version metadata and active dune documentation to 0.5.6.

## 0.5.5 — Transverse dune morphology pass

- Promoted the tested Arrakis Dev transverse profile to the development defaults:
  - `cell_size=8`;
  - `surface_resolution=sixteenth`;
  - transverse mode maximum height 30 blocks;
  - `dune_spacing=350`;
  - `spacing_variation=0.18`;
  - `ridge_sharpness=3.0`;
  - `valley_cutoff=0.20`;
  - `repose_angle=33`;
  - `cascade_passes=25`;
  - transverse transport default 180 iterations;
  - `transport_strength=1.0`;
  - `wind_angle=24`;
  - `edge_blend=7`.
- Added `slope_asymmetry` (0.0-1.0, default 0.60). The transverse seed now blends from
  the old symmetric profile toward a wind-oriented profile with a longer windward/stoss
  ramp and shorter lee face before transport and cascading.
- Added `interdune_cleanup` (0.0-1.0, default 0.30). Cleanup is based on local dune support
  rather than a second global cutoff: low dune toes near a major body are retained while
  weak isolated remnants in broad interdune plains are reduced.
- Added mild low-relief transverse transport attenuation to reduce stochastic micro-islands
  on nearly flat sand while preserving transport deformation on the dune body.
- Added a low-height positive-detail attenuation pass before support-aware cleanup.
- Kept `valley_cutoff=0.20`; screenshot tests showed that 0.18 retains too much basin clutter
  while 0.28 cleans the basin at the cost of visibly clipping useful dune-foot material.
- Kept the 0.5.4 sixteenth-layer sand renderer, block/model/texture assets, 0.5.3 camera and
  screenshot tools, 33-degree repose cascade, and barchan prototype otherwise unchanged.
- Kept NeoForge at 21.1.248.
- Updated project version metadata and active dune documentation to 0.5.5.
## 0.5.4 — Fractional dune surfaces

- Upgraded NeoForge from 21.1.244 to 21.1.248.
- Added the registered `minecraftdune:sand` falling block and its block item, model,
  localization, loot table, sand tag, and shovel-mining tag.
- Added `minecraftdune:sand_layer`, a custom 1-15 layer surface block with sixteenth-block
  visual shapes, snow-like stacking, support checks, collision, block item, models,
  localization, loot table, and shovel-mining tag. Placing one more layer on a 15/16 block
  converts it to a full `minecraftdune:sand` block.
- Added `/dune dunes settings surface_resolution whole|eighth|sixteenth`.
- Made `sixteenth` the 0.5.4 default. `eighth` uses even-numbered layer states, and `whole`
  retains the 0.5.3 nearest-whole-block output for direct comparisons.
- Preserved the 0.5.3 simulation, transverse/barchan morphology settings, regional seeds,
  camera presets, fixed-camera teleport behavior, and named/batch screenshot tools.
- Changed only the renderer boundary: the same interpolated physical height is quantized at
  the selected resolution, with full `minecraftdune:sand` below and at most one partial
  `minecraftdune:sand_layer` at the top of each column.
- Generation and clearing recognize old vanilla prototype sand as well as both new custom
  blocks, so an existing 0.5.3 test region can be regenerated or cleared in place.
- Updated project version metadata, README files, and the Arrakis dune prototype guide.

## 0.5.3 — Debug camera and repeatable screenshots

- Made `/dune` the canonical command root for the existing dune laboratory and the new
  camera tools.
- Retained `/minecraftdune` as a compatibility alias, so existing commands and test notes
  continue to work unchanged.
- Added client-side fixed-camera commands:
  - `/dune camera info`;
  - `/dune camera list`;
  - `/dune camera save <name>`;
  - `/dune camera goto <name>`;
  - `/dune camera delete <name>`;
  - `/dune camera tp <x> <y> <z> <yaw> <pitch>`.
- Saved camera presets include dimension, exact position, yaw, and pitch, and persist in
  `config/minecraftdune/debug-cameras.json`.
- Camera recall uses a server-authoritative teleport and supports returning to presets in
  another dimension.
- Added `/dune screenshot <name>` for named captures such as `dune_testG.png`.
- Added `/dune screenshot batch <label> [settle_ticks]` to visit every saved camera in
  alphabetical order and capture consistently named comparison images. The default delay
  is 40 client ticks after arrival at each camera.
- Added `/dune screenshot batch cancel`.
- Batch capture waits for the client to arrive, locks exact current and previous-frame
  position/rotation values, hides the HUD, captures after a fully rendered locked frame,
  and restores the previous HUD state on completion or cancellation.
- Screenshot files are never intentionally overwritten; repeated names receive `_2`,
  `_3`, and later numeric suffixes.
- Aligned the active dune documentation with the current source limit of 512 blocks for
  `dune_spacing` (the original 0.5.2 notes retain the earlier 256-block release limit).
- Updated the project version and human-facing build documentation to `0.5.3`. The
  generated `neoforge.mods.toml` continues to obtain its version from `mod_version` in
  `gradle.properties`.

## 0.5.2 — Transverse dune morphology and physical cascade

- Focused the Arrakis Dev dune laboratory on transverse-dune tuning while leaving the
  experimental barchan initializer unchanged for a later pass.
- Kept the simulation at 64 x 64 cells and capped `cell_size` at 8, so the largest
  synchronous test footprint remains 512 x 512 blocks.
- Changed the 0.5.2 default `cell_size` from 2 to 8 for a 512 x 512 transverse test field.
- Added transverse-specific live controls:
  - `dune_spacing` (32-256 blocks; default 100);
  - `spacing_variation` (0.0-0.50; default 0.18);
  - `ridge_sharpness` (1.0-8.0; default 4.0);
  - `valley_cutoff` (0.0-0.80; default 0.20).
- Removed the abstract `stable_slope` control and replaced it with `repose_angle`
  (10-45 degrees; default 33 degrees).
- Reworked cascade stabilization so it operates on Minecraft-scale coarse heights after
  the transported sand field is mapped to vertical blocks. No percentile normalization is
  performed after cascading, fixing the 0.5.1 behavior where cascade changes were largely
  stretched back toward the original profile.
- Reworked the slope test to account for `cell_size`: the permitted vertical difference
  between neighboring simulation samples is derived from the requested repose angle and
  their physical horizontal separation.
- Made the final cascade non-toroidal so a slope cannot avalanche across the visible
  laboratory region boundary.
- Increased `cascade_passes` from 0-8 to 0-64. Cascading now occurs after transport rather
  than after every transport iteration, making the control cheaper and easier to compare.
- Moved the artificial `edge_blend` fade to the final interpolation stage so it does not
  participate in physical repose stabilization.
- Reduced transverse per-cell seed noise to avoid amplifying low-level contour islands.
- Added documented baseline, flat-interdune, and cascade-stress screenshot profiles.
- Updated project metadata to version `0.5.2`.

## 0.5.1 — Arrakis Dev dune tuning commands

- Added live operator controls for the dune prototype so terrain can be tuned without
  recompiling or restarting the client.
- Added `/minecraftdune dunes settings` and `/minecraftdune dunes settings reset`.
- Exposed these simulation parameters:
  - `cell_size` (1-8);
  - `max_height` (0-32, where 0 uses the dune-mode default);
  - `stable_slope` (0.10-4.0);
  - `cascade_passes` (0-8);
  - `iterations` (0-1000, where 0 uses the dune-mode default);
  - `wind_angle` (-360 to 360 degrees, normalized internally);
  - `edge_blend` (0-32 simulation cells);
  - `transport_strength` (0.0-4.0).
- Made the output region size dynamic: the fixed 64 x 64 simulation grid now expands to
  `64 * cell_size` Minecraft blocks per side.
- Made wind direction, slope stabilization, cascade count, transport strength, output
  height, and interpolation footprint runtime parameters.
- Added `/minecraftdune dunes clear <cell_size>` for clearing a previously generated
  footprint after changing horizontal scale.
- Generation now clears prototype sand up to the hard development ceiling (Y=96) inside
  the active footprint so lowering `max_height` removes old peaks immediately.
- Updated dune documentation and README with a recommended first gentle/wide test profile.
- Corrected the project version metadata to `0.5.1`.

## 0.5.0 — Arrakis Dev dune prototype

- Added deterministic prototype dune generation for the Arrakis Dev world.
- Added operator commands:
  - `/minecraftdune dunes generate transverse`
  - `/minecraftdune dunes generate barchan`
  - `/minecraftdune dunes info`
  - `/minecraftdune dunes clear`
- Added a reduced 64 x 64 sand-thickness simulation rendered into a 128 x 128 block
  region.
- Added saltation-like directional transport, lee-side erosion dampening, and repeated
  slope stabilization.
- Added deterministic region seeds derived from the world seed, region coordinates,
  and dune mode.
- Added edge blending so generated test regions return to the original Y=64 surface.
- Added documentation of the simulation architecture, limitations, and test procedure.
- Updated the mod version from `0.1.0` to `0.5.0`.

## 0.4.0 — Arrakis Dev flat world

- Added the selectable `minecraftdune:arrakis_dev` world preset.
- Added a desert surface at Y=64 with ten sand layers, ten sandstone layers, stone,
  deepslate, and bedrock.
- Disabled overworld biome features, lakes, structures, and caves for deterministic
  testing.
- Retained normal vanilla Nether and End generation.

Public commit: `e24d3d5` — `Added Arrakis-dev flatbiome for testing`.

## 0.3.0 — Muad'dib redesign

- Replaced the original entity geometry with the revised tiny kangaroo-mouse design.
- Updated the runtime Java model, texture, and editable Blockbench source.
- Added the articulated body, ears, front legs, hind legs, and segmented tail used by
  the current animations.

## 0.2.1 — Muad'dib animation correction

- Corrected the animation/model integration produced during the initial animation patch.
- Aligned the revised hierarchy with the runtime model and movement-driven animation.
- Retained stationary random one-shot actions without adding network synchronization.

## 0.2.0 — Muad'dib animation update

- Added `MuaddibMouseAnimations.java`.
- Added coordinated hopping driven by actual entity movement.
- Added a subtle idle and balance loop.
- Added random stationary `wiggle_head` and `sniff_ground` actions.
- Added client-side animation state handling to the Muad'dib entity.

The public repository combines the 0.2.0, 0.2.1, and 0.3.0 development milestones in
commit `16b56a9` — `updated muaddib model`.

## 0.1.1 — NeoForge 1.21.1 compatibility fix

- Replaced the unavailable newer `DeferredRegister.Entities` API with the generic
  NeoForge 1.21.1 entity registry.
- Registered entity types through `Registries.ENTITY_TYPE` and `DeferredHolder`.
- Passed the registered resource identifier to `EntityType.Builder.build(String)`.
- Removed the deprecated explicit event-bus selector from `ClientModEvents`.
- Added the Gradle wrapper JAR.

Public commit: `0e36dd6` — `fixed`.

## 0.1.0 — Initial development build

- Created the standalone Minecraft 1.21.1 NeoForge project.
- Added the Muad'dib desert mouse entity as a rabbit-derived test mob.
- Added the spawn egg, client renderer, initial model and texture, project structure,
  build configuration, and Blockbench workflow.

Public commit: `5574dfa` — `initial`.

## Repository history note

The public repository currently contains four historical commits:

1. `5574dfa` — initial
2. `0e36dd6` — fixed
3. `16b56a9` — updated muaddib model
4. `e24d3d5` — Added Arrakis-dev flatbiome for testing

The semantic versions above describe the project milestones rather than claiming a
one-to-one release tag for every public commit.
