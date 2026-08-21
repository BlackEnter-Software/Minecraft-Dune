# Lithology and fracture framework — 0.5.13 foundation / 0.5.14 consumer

## Scope

0.5.13 gave native Arrakis rock geological identity and added a separate massif-top fissure
network. Version 0.5.14 now consumes that framework:

- it does generate coherent rock units, intrusive bodies, resistant basalt sheets, horizontal
  calcite bands, and top-down cracks/slots/chasms;
- it defines resistance classes and loose talus material hooks;
- 0.5.14 uses them for differential retreat, supported undercuts, resistant benches/ribs and
  localized cliff-base talus;
- it still does not create 0.5.15 caves or water. Limestone remains a future cavern host.

All fields use the world seed, serialized terrain profile, and absolute coordinates. There
is no per-chunk simulation and no `ServerLevel#setBlock` post-pass, so results are seamless
at chunk boundaries and suitable for Distant Horizons pregeneration.

## Geological roles and resistance

| Logical material | Default block | Resistance | Geological role |
|---|---|---|---|
| stone | `minecraft:stone` | medium | background structural host |
| sandstone | `minecraft:sandstone` | soft | soft sedimentary unit |
| tuff | `minecraft:tuff` | soft | altered/weak volcanic unit |
| limestone | `create:limestone` | soft | rare future cavern-host lens |
| calcite | `minecraft:calcite` | medium | horizontal bands and mineralized fracture exposure |
| andesite | `minecraft:andesite` | hard | intrusive/structural body |
| diorite | `minecraft:diorite` | hard | intrusive/structural body |
| basalt | `minecraft:basalt` | very hard | resistant horizontal sheets |
| blackstone | `minecraft:blackstone` | very hard | rare ancient resistant body |
| talus | `minecraft:gravel` | loose | active scree/collapse matrix, never intact bedrock |

Resistance modestly changes fissure width and depth. In 0.5.14 it also controls per-Y face
retreat: soft units recede, hard intrusions brace faces, and basalt or blackstone can survive
as extremely resistant ribs/caps after long coronal-wind erosion.

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
: Nominal thickness of background stratigraphic bands.

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

### Sheet and mineral-band controls

`dike_spacing`
: Legacy serialized name retained for backwards compatibility. It now controls the nominal
  vertical separation of laterally discontinuous, warped horizontal basalt sheets. The first
  0.5.13 visual test showed that world-spanning vertical planes read as artificial ruler lines.

`dike_half_width`
: Approximate half-thickness of those basalt sheets. Local gates and coherent contact noise
  break sheets into bodies and roughen their contacts.

`calcite_vein_spacing`
: Legacy serialized name retained for backwards compatibility. It now controls the vertical
  separation of horizontal calcite bands and lenses through ordinary host rock.

`calcite_vein_half_width`
: Approximate half-thickness of those natural calcite bands. Mineralized fissures use the
  separate `fractures.calcite_wall_thickness` control.

### Contact roughness

Material selection combines the broad unit, rare-body, and intrusion fields with coherent
detail and micro-detail fields. The added scales displace both vertical contacts and unit
selectors, producing irregular interlocking boundaries instead of smooth ellipses or planes.
They remain coherent geological bodies rather than per-block decorative speckle. These
contact-detail scales are intentionally derived from the serialized unit scales, so no new
required JSON fields were added by the initial visual-tuning refinement.

### `materials`

Every material value is a namespaced block identifier resolved through the registry:

- `background` — normal stone host;
- `sandstone` — soft sedimentary unit;
- `tuff` — soft altered unit;
- `limestone` — preferred optional limestone block;
- `limestone_fallback` — block used when the preferred limestone is unavailable;
- `calcite` — horizontal bands and mineralized fissure exposure;
- `andesite` / `diorite` — hard intrusive bodies;
- `basalt` — very-hard resistant sheets;
- `blackstone` — rare very-hard ancient bodies;
- `talus` — loose scree/collapse material, normally gravel.

Changing an identifier changes representation, not the logical geological role or resistance
class. Resistance remains code-owned; 0.5.14 maps those stable classes to serialized relative
retreat multipliers.

### `talus`

`local_scree_enabled`
: Master switch for localized cliff-base and fissure-outlet scree. It is `true` in the supplied
  0.5.14 source profile; older profiles that omit erosion remain unaffected.

`minimum_fracture_strength`
: Legacy compatibility name for the minimum combined talus-suitability threshold, and still
  the minimum fissure strength for the outlet boost. It is `0.44` in the supplied 0.5.14
  profile. Higher values make deposits rarer.

`maximum_thickness`
: Cap, in blocks, on a local scree deposit's vertical thickness, runtime-clamped to `0..32`.
  A value of `0` emits no talus.

`spread`
: Approximate horizontal falloff, in blocks, around an eligible cliff base or fissure mouth.

The 0.5.14 erosion field combines these values with cliff strength, low-side elevation,
deterministic patch noise, wind exposure and fracture proximity. Gravel is the principal matrix
and coherent source-unit clasts form a minority of the apron. Talus begins above surviving rock
and any full dune-sand blocks; an overlapping fractional dune layer is omitted so gravel always
has full-block support. See [ESCARPMENT_EROSION.md](ESCARPMENT_EROSION.md).

## `fractures` JSON parameters

The fissure field is separate from `faults`. Regional faults are kilometre-scale passages.
Local fractures use continuous warped primary trace families that cross an exposed massif,
with finite side branches that may terminate as dead ends.

`enabled`
: Master switch for fissure carving. Lithology still generates when this is false.

`cell_size`
: Legacy field name retained for profile compatibility. It now controls the approximate
  spacing of candidate primary traces and branch nodes. It does not create a visible grid.
  The supplied refinement default is `520` blocks.

`density`
: Probability from `0..1` that a candidate continuous primary line is active. The supplied
  refinement default is `0.72`.

`minimum_length` / `maximum_length`
: Length range for finite branches. Primary traces are continuous and are clipped naturally
  by the exposed rock formation rather than by an arbitrary midpoint or endpoint.

`branch_chance`
: Probability that a deterministic node along an active primary trace develops one tapered
  side branch. A branch can end inside the massif; a primary fissure cannot begin there.

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
: Probability that a primary or branch carries calcite mineralization. Successful fissures
  receive different base abundances, which also vary coherently along the trace. Some fissures
  therefore show almost no calcite while mineral-rich ones expose more frequent bands.

`calcite_wall_thickness`
: Maximum approximate thickness of intermittent calcite exposure in a mineralized fissure
  wall. Exposure is restricted to horizontal bands; it no longer outlines the complete wall
  or floor of every mineralized crack.

`resistance_width_influence`
: Blend from unmodified width (`0`) toward the surface lithology's soft/hard width factor
  (`1`). The 0.5.13 default is intentionally modest.

`resistance_depth_influence`
: Equivalent blend for fissure depth. The separate 0.5.14 erosion pass applies its own
  per-Y retreat multipliers around exposed faces.

## Generated fissure geometry

The generator lowers the solid rock top inside the procedural fracture trace. Continuous
primary lines enter and leave wherever the macro massif surface exists, so a main fissure
does not visibly originate in the middle of a broad plateau. Finite tapered branches are the
intentional dead ends. The result includes shallow cracks, narrow slots and deeper open
chasms with vertical walls in the existing height-column architecture.

The fissure carve itself remains a top-down height adjustment. The 0.5.14 erosion field runs
afterward and can open the trace through an escarpment or form bounded rock-air-rock undercuts;
it still does not create general caves or summit-denudation simulation.

Where traces overlap, the strongest and second-strongest carves produce a bounded intersection
signal that modestly strengthens and deepens the fissure. Cliff-face fracture susceptibility is
vertically attenuated below the fissure's design depth, so a shallow crack does not weaken the
entire massif wall down to the crust.

The floor is clamped to at least one native-rock block above the Y64 base surface. Every
visible rock column is still rewritten down through the former sand/sandstone layers until it
meets the hard flat-world crust, so formations and fissure floors remain foundation-connected.
The first two native-rock blocks above Y64 bypass erosion occupancy, preserving shallow
one- and two-block outcrops.

## Diagnostics

```mcfunction
/dune geology info
/dune geology sample <x> <z>
/dune geology profile
```

Coordinate diagnostics report exposed logical/resolved lithology, resistance, limestone-host
status, intrusion/basalt/calcite flags, fissure strength/width/depth/intersection and the active
surface escarpment candidate. The profile command reports lithology, fracture and erosion
settings and confirms which limestone/talus block identifiers resolved in the current mod set.

## Current and future consumers

0.5.14 consumes `ResistanceClass`, material roles, fracture samples and talus settings to build
differential erosion, near-vertical faces, selective bounded undercuts, locally negative-angle
cliffs and coherent scree. The full algorithm and JSON guide are in
[ESCARPMENT_EROSION.md](ESCARPMENT_EROSION.md).

0.5.15 should use `limestoneHost`, fracture connectivity and mineralization to place dry,
mineralized and collapse caverns. Sealed water caverns should be extremely rare and limited
to appropriate limestone/fracture combinations.
