# Escarpment and differential erosion — 0.5.14

Version 0.5.14 adds a deterministic, lithology-aware three-dimensional erosion layer to the
native Arrakis generator. Its purpose is to turn selected smooth massif and large Broken Rock
edges into steep scarps, resistant benches, narrow ribs, bounded undercuts and localized talus.
It is a morphology pass, not a decorative material-noise pass or an iterative erosion
simulation.

## Generation order

For each absolute X/Z column the generator evaluates:

```text
flat Arrakis foundation
        ↓
MacroGeologyField rock envelope, faults, sand passes and province weights
        ↓
independent NativeTransverseDuneField height sample from macro suitability
        ↓
LithologyField 3D units + MassifFractureField fissures
        ↓
EscarpmentErosionField face candidate and per-Y rock occupancy
        ↓
foundation-connected surviving rock
        ↓
native full-block dune base where present
        ↓
localized talus above rock / full dune blocks
        ↓
optional fractional dune layer only where talus does not occupy that Y
```

The erosion field is **removal-only**. It cannot place rock above the macro/fissure envelope,
so it cannot create a new floating formation or bridge a gap that macro geology already cut.
`getBaseHeight()`, `getBaseColumn()` and full chunk generation all use the same terrain-column
evaluation.

## Escarpment candidates

The field first rejects ordinary sand, low-relief rock and areas strongly carved by a regional
fault or sand pass. It then samples macro elevation and rock masks at four points around the
column. Those coarse probes provide:

- local relief;
- an inward/outward face normal;
- a signed distance from the selected rock-mask edge;
- permission from massif, faulted-margin or suitably large Broken Rock terrain.

The signed edge distance replaces part of the former smooth height-field apron with a much
steeper face. `vertical_face_bias` controls how strongly the candidate is pulled toward that
face. `broken_rock_scale` applies the same system to detached remnants at reduced strength;
small outer fragments normally fail the relief and source-height gates and retain their simpler
shape.

## Three-dimensional occupancy and support

After a column becomes a candidate, every Y within its existing rock envelope is tested against
the local lithology, fracture exposure, wind exposure and a bounded 3D detail field. Different
Y levels can therefore produce:

```text
rock
rock       resistant cap or bench
air
air        locally recessed soft layer
rock       supported lower wall
```

This is genuine voxel occupancy rather than a single surface height. The safety rules are:

- the field only removes from an existing, fissure-adjusted rock envelope;
- differential/material boundary offset around the selected face is capped by
  `max_undercut_blocks` and hard-clamped to at most 16 blocks. This is not the total width of
  the former smooth macro apron replaced by face steepening;
- undercut patches require a resistant supported cap sampled across the surface and two layers
  below it;
- soft or medium cap sequences suppress the undercut gate;
- the lowest native-rock layers above Y64 are always retained;
- shallow one- and two-block outcrops therefore survive instead of being erased by the
  escarpment occupancy pass;
- fault cores and sand passes are excluded before occupancy evaluation;
- no post-generation block updates or unsupported-roof growth occurs.

These constraints allow occasional negative-angle faces without producing enormous roofs or
detached sheets. The implementation is not structural physics: it estimates plausible cap
support and bounded retreat analytically.

## Lithology and resistance

The 0.5.13 logical material roles remain authoritative. Erosion reads the material at each Y;
it does not repaint a surface independently from geology.

| Resistance | Materials | 0.5.14 retreat multiplier | Morphological result |
|---|---|---:|---|
| soft | sandstone, tuff, limestone | `1.35` | recesses, wider fracture halos and undercut-prone lower units |
| medium | stone, calcite-bearing host | `1.00` | baseline retreat |
| hard | andesite, diorite | `0.58` | projecting cores, benches and ribs |
| very hard | basalt, blackstone | `0.28` | sharp resistant sheets, dikes and remnant faces |
| loose | gravel | not intact rock | talus/collapse matrix only |

The three serialized multipliers are relative to the code-owned medium baseline. Increasing a
multiplier makes that class retreat more; decreasing it makes the class stand farther proud.
Calcite exposed by the fracture system remains a medium mineralized wall/band material.
Limestone remains a soft coherent host unit and is not converted into a cave in this release.

## Fractures and intersections

The existing massif fissure field is sampled before erosion. Nearby active fissures add a
bounded susceptibility halo, and `intersectionStrength` adds extra collapse potential where
branches or primary traces meet. The accumulator uses the strongest and second-strongest
overlapping traces, so intersections modestly strengthen and deepen the carve. In the 3D face
operator, fracture-driven retreat fades out below the fissure's design depth instead of
continuing down the complete cliff. This means:

- soft fracture margins can widen into recesses;
- basalt and blackstone slots tend to remain narrow and resistant;
- intersections can become locally larger chasms/collapse scars;
- through-going plateau fissures remain open through an eroded cliff face;
- fissure outlets modestly increase talus suitability.

Fracture erosion is a contribution to the local face retreat, not permission to turn every
crack into a canyon. Regional faults remain controlled by their independent 0.5.12 absolute
floor model and are excluded from the escarpment operator at strong carve values.

## Coarse wind exposure

The current development wind direction is shared with
`native_dunes.wind_angle_degrees`—24 degrees in the supplied profile. The erosion field compares
that direction with the estimated outward face normal, local relief and a slow deterministic
shelter field. Wind-facing exposed scarps therefore retreat more than sheltered faces.

This is intentionally inexpensive and is not computational fluid dynamics. A later regional
wind system can replace or enrich the direction, strength, shelter and sand-supply inputs
without changing the lithology/occupancy contract.

## Talus and scree

Talus uses the existing `lithology.talus` profile instead of a duplicate erosion palette. It is
restricted to coherent patches on the low/outside side of eligible cliff faces. Suitability
combines escarpment strength, low-side elevation and distance, deterministic patch noise,
fracture proximity and wind exposure. The supplied `spread=18` and `maximum_thickness=7` create
localized aprons rather than a blanket around the massif.

Final vertical composition is support-aware. Full dune-sand blocks are treated as a solid base,
so talus starts above both surviving rock and the top full dune block. If the optional fractional
dune layer occupies that same Y, talus wins and the partial layer is omitted. Gravel therefore
does not hang unsupported above a fractional-height sand layer, and full dune blocks are not
silently replaced by scree.

Gravel is the principal matrix. Coherent minority clasts are sampled from the adjacent source
lithology, producing approximate palettes such as stone/andesite/gravel, tuff/gravel or
limestone/calcite/gravel without arbitrary block speckle. The apron may create occasional
climbable approaches, but the main wall is still intended to remain unclimbable in many places.

The legacy JSON name `minimum_fracture_strength` is retained for profile compatibility. In
0.5.14 it is also the minimum combined talus-suitability threshold; raising it makes deposits
rarer, while lowering it allows more cliff-base patches.

## JSON parameter guide

The active source profile stores an optional `terrain.erosion` object. Every field has a codec
default. A saved 0.5.13 profile that omits the entire object decodes with erosion disabled, so
newly generated chunks do not silently change at its old border. The 0.5.14 source preset
explicitly enables the pass.

### `erosion`

| Parameter | Supplied value | Unit / useful range | Increasing it does | Important interactions |
|---|---:|---|---|---|
| `enabled` | `true` | boolean | activates 3D escarpment occupancy | missing group defaults to `false` for 0.5.13 save compatibility |
| `minimum_relief` | `18.0` | blocks; usually `8..48`, effective minimum `4` | requires taller local relief, so fewer low formations qualify | compared with four coarse face probes and source rock height |
| `face_probe_distance` | `18.0` | blocks; runtime clamp `6..48` | observes a broader/coarser neighborhood | controls local relief, face normal and signed edge distance; larger values cost no extra probes |
| `escarpment_start_strength` | `0.32` | rock-mask value; runtime clamp `0.05..0.90` | moves the selected face contour toward stronger interior rock | shifts the mask isocontour; it is not a simple erosion multiplier |
| `vertical_face_bias` | `0.84` | dimensionless; runtime clamp `0..1.25` | strengthens cliff-face replacement and makes eligible aprons steeper | still gated by relief, province and face distance |
| `wind_exposure_strength` | `0.42` | multiplier; normally `0..1.5` | increases retreat contrast between exposed and sheltered faces | uses `native_dunes.wind_angle_degrees`, outward normal and coarse shelter |
| `fracture_erosion_strength` | `0.58` | multiplier; normally `0..1.5` | increases retreat near fissures and intersections | combines fracture strength, halo proximity and intersection strength |
| `soft_rock_multiplier` | `1.35` | relative retreat; usually `1.0..2.0` | recesses sandstone, tuff and limestone farther | medium rock is fixed at `1.0`; also sets loose-material susceptibility |
| `hard_rock_multiplier` | `0.58` | relative retreat; usually `0..1.0` | erodes andesite/diorite more; lowering leaves stronger ribs | evaluated independently at every Y |
| `very_hard_rock_multiplier` | `0.28` | relative retreat; usually `0..1.0` | erodes basalt/blackstone more; lowering leaves sharper remnants | evaluated independently at every Y |
| `undercut_strength` | `0.72` | multiplier; runtime clamp `0..1.5` | deepens eligible recessed layers | only acts in coherent patches beneath sufficiently resistant caps |
| `max_undercut_blocks` | `6` | horizontal blocks; runtime clamp `0..16` | permits farther differential/material offset and wider negative-angle faces | caps offset around the selected signed face, not the total smooth macro apron removed by face steepening; `0` keeps face steepening but removes differential reach |
| `undercut_frequency` | `0.24` | probability-like `0..1` | makes coherent undercut patches more common | does not bypass cap-support, relief or maximum-reach rules |
| `broken_rock_scale` | `0.72` | contribution scale; runtime clamp `0..1.5` | applies escarpment candidates to more/lower Broken Rock remnants | `0` removes the Broken Rock contribution; massif/faulted-margin permission remains |

### `lithology.talus`

| Parameter | Supplied value | Unit / useful range | Increasing it does | Important interactions |
|---|---:|---|---|---|
| `local_scree_enabled` | `true` | boolean | permits localized talus deposition | no talus is emitted when false; erosion geometry still runs |
| `minimum_fracture_strength` | `0.44` | normalized suitability `0..0.98` | makes talus rarer | legacy name; threshold now combines cliff, patch, fracture and wind suitability and still gates fissure outlets |
| `maximum_thickness` | `7` | blocks; runtime clamp `0..32` | allows thicker local aprons | `0` emits no talus; actual depth is scaled by suitability and distance falloff |
| `spread` | `18.0` | horizontal blocks; effective minimum `1` | extends apron falloff farther from the face | also controls the coarse size of deterministic talus patches |

The block palette remains under `lithology.materials`; `talus` resolves to
`minecraft:gravel` in the supplied profile. Optional Create limestone continues to resolve by
registry with the configured vanilla fallback.

## Diagnostics

```mcfunction
/dune geology
/dune geology info
/dune geology sample <x> <z>
/dune geology profile
```

Coordinate diagnostics now report surviving rock Y, exposed lithology/resistance, fissure
intersection strength, escarpment activation/strength, local relief, maximum differential
boundary offset (the diagnostic's `maximum retreat` value), coarse wind exposure, fracture
erosion, undercut potential and talus suitability/thickness. `profile` reports the active
erosion controls and resolved talus material.

The coordinate commands accept X/Z, not Y. Their escarpment line describes the most useful
surface/face candidate for that column. A true undercut may occur below the reported exposed
surface because occupancy and lithology are evaluated independently at every Y. Use an in-world
cross-section or repeated samples around the face when evaluating vertical geometry.

## Determinism and performance

- Every field depends only on world seed, serialized profile and absolute coordinates.
- Four fixed-distance macro probes replace any large neighborhood search.
- Candidate/province/fault/sand-pass checks return before vertical work where possible.
- Per-Y occupancy is evaluated only inside native rock-bearing columns.
- There is no iterative per-chunk erosion, chunk-order dependency, lighting update or
  `ServerLevel#setBlock` post-pass.
- Blocks are written directly to `ChunkAccess`; height and base-column queries share the same
  analytic path.
- The pure basin and open erg retain their existing fast exclusions. Final composition keeps
  full dune blocks below talus and suppresses only an overlapping fractional dune layer.

These properties keep chunk seams deterministic and retain the architecture needed by Distant
Horizons pregeneration. The four extra macro probes and per-Y occupancy make eligible cliff
columns more expensive than 0.5.13 height columns, but work is restricted to plausible rock
faces.

## Validation and limitations

The noninteractive `validateArrakisTerrain` check covers profile decoding, disabled fallback
for missing erosion settings, resistance order, deterministic repeated samples, explicit
chunk-boundary and reversed-order comparisons, pure-basin/open-erg exclusion, native dune
survival, bounded retreat, actual and potential rock-air-rock undercuts, fracture/no-fracture
comparisons, localized supported talus, 0.5.12 fault floors and hard-crust connection.

0.5.14 intentionally does not implement:

- a full cave or collapse-chamber simulation;
- rare sealed water caverns;
- dynamic or regional wind/sand transport;
- physical collapse of every unsupported voxel;
- the final rock texture/art pass;
- a complete mesa-to-butte lifecycle model;
- biological or ecological systems.

Limestone hosts, mineralized fractures and exposed cliff units remain available to the planned
0.5.15 subsurface geology pass. No common exposed water is introduced here.
