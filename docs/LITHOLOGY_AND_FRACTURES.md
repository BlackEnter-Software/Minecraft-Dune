# Lithology and fracture framework — 0.5.13

## Scope

0.5.13 gives native Arrakis rock geological identity and adds a separate massif-top fissure
network. It is deliberately a framework release:

- it does generate coherent rock units, intrusive bodies, basalt structures, calcite veins,
  and top-down cracks/slots/chasms;
- it defines resistance classes and loose talus material hooks;
- it does not yet create 0.5.14 differential erosion, undercuts, negative-angle cliffs, or
  final large talus cones;
- it does not yet create 0.5.15 caves or water. Limestone is only marked as a future cavern
  host in this release.

All fields use the world seed, serialized terrain profile, and absolute coordinates. There
is no per-chunk simulation and no `ServerLevel#setBlock` post-pass, so results are seamless
at chunk boundaries and suitable for Distant Horizons pregeneration.

## Geological roles and resistance

| Logical material | Default block | Resistance | 0.5.13 role |
|---|---|---|---|
| stone | `minecraft:stone` | medium | background structural host |
| sandstone | `minecraft:sandstone` | soft | soft sedimentary unit |
| tuff | `minecraft:tuff` | soft | altered/weak volcanic unit |
| limestone | `create:limestone` | soft | rare future cavern-host lens |
| calcite | `minecraft:calcite` | medium | veins and mineralized fracture fill |
| andesite | `minecraft:andesite` | hard | intrusive/structural body |
| diorite | `minecraft:diorite` | hard | intrusive/structural body |
| basalt | `minecraft:basalt` | very hard | dikes and resistant sheets |
| blackstone | `minecraft:blackstone` | very hard | rare ancient resistant body |
| talus | `minecraft:gravel` | loose | future scree/collapse material, not bedrock |

Resistance modestly changes fissure width and depth in 0.5.13. The larger purpose is to give
0.5.14 a stable input: soft units can recede, hard intrusions can brace faces, and basalt or
blackstone can survive as extremely resistant ribs/caps after long coronal-wind erosion.

## Optional Create limestone

The default limestone identifier is `create:limestone`, which is present in the current
development `run/mods` installation. Minecraft: Dune does not import a Create Java class and
does not declare Create as a Gradle dependency. The identifier is looked up in the block
registry when the generator is created. If it is unavailable or invalid,
`limestone_fallback` is used instead. The default fallback is `minecraft:sandstone`.

This keeps Create optional while allowing worlds made with Create installed to use its
limestone directly. Removing a content mod from an existing world is still a general world
compatibility decision; the fallback mainly supports profiles decoded when that registry
entry is not present.

## `lithology` JSON parameters

### Body and stratigraphy controls

`unit_horizontal_scale`
: Horizontal coherence scale for background stone/sandstone/tuff units. Larger values make
  individual rock units broader in X/Z.

`unit_vertical_scale`
: Vertical coherence scale for the same 3D unit field. Larger values make units thicker and
  slower-changing with elevation.

`strata_thickness`
: Nominal thickness of stratigraphic bands. It also spaces candidate resistant basalt sheets.

`strata_warp_scale`
: Horizontal scale of folding/warping applied to the stratigraphic elevation. Larger values
  produce broader, slower undulation.

`strata_warp_strength`
: Maximum approximate vertical displacement contributed by the strata-warp field. Zero gives
  level bands; larger values bend units more strongly.

`intrusion_scale`
: 3D coherence scale of andesite/diorite intrusive bodies. This is not a literal diameter.

`intrusion_threshold`
: Minimum centered 3D intrusion noise needed for hard andesite/diorite. Raising it makes
  intrusions rarer; lowering it makes them more extensive.

`rare_body_scale`
: 3D coherence scale shared by rare limestone lenses and blackstone bodies.

`limestone_threshold`
: Absolute negative threshold for limestone-host bodies. Limestone is selected where rare-body
  noise is at or below `-limestone_threshold`. Raising the value makes limestone rarer.

`blackstone_threshold`
: Positive threshold for ancient blackstone bodies. Blackstone is selected where rare-body
  noise is at or above this value. Raising it makes blackstone rarer.

### Dike, sheet, and vein controls

`dike_spacing`
: Nominal separation of warped, near-vertical basalt dikes.

`dike_half_width`
: Half-width of a basalt dike. It also provides the base thickness for rare basalt sheets.
  A value of `2.5` therefore permits a dike approximately five blocks wide before local warp.

`calcite_vein_spacing`
: Nominal separation of thin oblique calcite vein planes through ordinary host rock.

`calcite_vein_half_width`
: Half-width of those natural calcite veins. Mineralized fissures use the separate
  `fractures.calcite_wall_thickness` control.

### `materials`

Every material value is a namespaced block identifier resolved through the registry:

- `background` — normal stone host;
- `sandstone` — soft sedimentary unit;
- `tuff` — soft altered unit;
- `limestone` — preferred optional limestone block;
- `limestone_fallback` — block used when the preferred limestone is unavailable;
- `calcite` — veins and mineralized fissure exposure;
- `andesite` / `diorite` — hard intrusive bodies;
- `basalt` — very-hard dikes/sheets;
- `blackstone` — rare very-hard ancient bodies;
- `talus` — loose scree/collapse material, normally gravel.

Changing an identifier changes representation, not the logical geological role or resistance
class. Resistance is intentionally code-owned in 0.5.13 so future erosion has a stable model.

### `talus`

`local_scree_enabled`
: Reserved switch for modest local scree placement. It is `false` in 0.5.13 because final
  deposition belongs with 0.5.14 slope/escarpment analysis.

`minimum_fracture_strength`
: Future minimum fissure strength for a local talus candidate.

`maximum_thickness`
: Future cap on a local scree deposit's vertical thickness.

`spread`
: Future approximate horizontal spread around a fissure mouth or cliff base.

These values are serialized now, and fissure samples expose a `talusCandidate` hook, but no
large final talus cones are emitted in 0.5.13.

## `fractures` JSON parameters

The fissure field is separate from `faults`. Regional faults are kilometre-scale passages;
fractures are local massif-top hazards built from finite bent trunks and branches.

`enabled`
: Master switch for fissure carving. Lithology still generates when this is false.

`cell_size`
: Size of the deterministic spatial cells that can seed a local network. This controls
  distribution, not a visible grid; segments may cross cell and chunk boundaries.

`density`
: Probability from `0..1` that a candidate cell contributes a fracture network.

`minimum_length` / `maximum_length`
: Length range for main fracture trunks. Branches are shorter fractions of their trunk.

`branch_chance`
: Probability that an active trunk develops a side branch. Some successful trunks may also
  receive a shorter second branch on the opposite side.

`minimum_width` / `maximum_width`
: Full target fissure-width range before local taper and modest resistance modulation.
  Defaults target approximately 1–12 blocks.

`minimum_depth` / `maximum_depth`
: Centerline design-depth range before surface activation, edge taper, and modest resistance
  modulation. Defaults target approximately 5–68 blocks.

`minimum_rock_height`
: Added native-rock height at which fractures begin activating. This prevents small foreland
  stones and almost-flat rock remnants from being shredded by massif hazards.

`minimum_massif_weight`
: Minimum macro massif/faulted-margin permission for activation. Broken-rock terrain receives
  only a reduced contribution.

`mineralization_chance`
: Probability that a fracture network carries calcite mineralization metadata and visible
  wall/floor exposure.

`calcite_wall_thickness`
: Approximate thickness of the calcite exposure halo along a mineralized fissure wall.

`resistance_width_influence`
: Blend from unmodified width (`0`) toward the surface lithology's soft/hard width factor
  (`1`). The 0.5.13 default is intentionally modest.

`resistance_depth_influence`
: Equivalent blend for fissure depth. This is not the full differential-erosion pass.

## Generated fissure geometry

The generator lowers the solid rock top inside the procedural fracture trace. This creates
shallow cracks, narrow slots and deeper open chasms with vertical walls in the existing
height-column architecture. It does not create roofs, caves, overhangs or undercuts.

The floor is clamped to at least one native-rock block above the Y64 base surface. Every
visible rock column is still rewritten down through the former sand/sandstone layers until it
meets the hard flat-world crust, so formations and fissure floors remain foundation-connected.

## Diagnostics

```mcfunction
/dune geology info
/dune geology sample <x> <z>
/dune geology profile
```

Coordinate diagnostics report logical/resolved lithology, resistance, limestone-host status,
intrusion/basalt/calcite flags, fissure strength/width/depth, activation and mineralization.
The profile command reports the active lithology and fracture scales and confirms which
limestone/talus block identifiers actually resolved in the current mod set.

## Future consumers

0.5.14 should consume `ResistanceClass`, material roles, fracture samples and talus settings
to build true differential erosion, near-vertical unclimbable faces, selective undercuts,
locally negative-angle cliffs, and coherent scree.

0.5.15 should use `limestoneHost`, fracture connectivity and mineralization to place dry,
mineralized and collapse caverns. Sealed water caverns should be extremely rare and limited
to appropriate limestone/fracture combinations.
