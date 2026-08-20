# Minecraft: Dune

Standalone NeoForge 1.21.1 development project for the Minecraft: Dune mod.

Current development version: **0.5.13**

The project currently contains:

- the animated Desert Hare and the smaller exported Muad'dib desert mouse;
- the selectable **Arrakis Dev** desert world preset;
- native deterministic macro geology generated as part of the Arrakis chunk pipeline;
- coherent 3D lithology with geological resistance roles and optional Create limestone;
- deterministic through-going massif fissures, dead-end branches and variable calcite bands;
- native transverse far-erg dunes with full and sixteenth-layer sand surfaces;
- an operator-only deterministic dune prototype for the Arrakis Dev world;
- live in-game tuning commands for the dune prototype;
- custom full and fractional dune-sand blocks with selectable surface resolution;
- persistent fixed cameras and repeatable named/batch screenshots for terrain testing.

## Requirements

- Minecraft Java Edition 1.21.1
- NeoForge 21.1.248
- Java Development Kit 21
- Internet access on the first Gradle run
- IntelliJ IDEA, Eclipse, or another Java IDE is optional

## Run the development client on Windows

Open PowerShell in this directory:

```powershell
.\gradlew.bat runClient
```

The development client JVM arguments configured in `build.gradle` are preserved. The
Gradle project intentionally declares no third-party runtime mods; Distant Horizons and
other development/test mods can be installed manually.

## Arrakis Dev world

**Create a new Arrakis Dev world for clean 0.5.13 lithology/fracture testing.**

The `minecraftdune:arrakis_dev` generator codec stores its serialized `terrain` profile.
0.5.13 adds optional `lithology` and `fractures` sections, so older generator data remains
decodable with defaults. Existing chunks are never rewritten; create a new world (or
regenerate closed-world region files) for clean visual comparisons.

The native generator retains the same base stratigraphy:

| Y range | Base material |
|---:|---|
| 65 and above | Native rock/dune terrain where fields require it; otherwise air |
| 55 to 64 | Sand |
| 45 to 54 | Sandstone |
| 0 to 44 | Stone |
| -63 to -1 | Deepslate |
| -64 | Bedrock |

Biome features, lakes, structures, and caves remain disabled in the Arrakis Dev overworld.
The Nether and End retain normal vanilla generation.

## Lithology and massif fissures — 0.5.13

Native rock now reads as coherent geological units rather than uniform stone or per-block
speckle. Stone, sandstone, tuff, calcite, andesite, diorite, basalt and blackstone have
explicit roles and soft/medium/hard/very-hard resistance classes. Gravel is defined as loose
talus/collapse material rather than intact bedrock.

The current development mod set contains Create and the profile requests `create:limestone`
for rare sedimentary lenses. Minecraft: Dune keeps Create optional by resolving the identifier
through the block registry and falling back to `minecraft:sandstone` when it is absent.

An independent absolute-coordinate fracture field creates warped primary traces that cross
the exposed massif, plus finite tapered branches that may terminate inside it:

- approximately 1–12 blocks wide;
- primary traces span the exposed formation; finite branches run tens to hundreds of blocks;
- approximately 5–68 blocks deep at their strongest centers;
- resistance-modulated so soft units open modestly more than basalt/blackstone;
- variably mineralized with intermittent horizontal calcite bands on some fissure walls.

Primary fissures do not originate in the middle of a plateau. Their analytic lines continue
through the massif and are clipped only where exposed macro rock ends; internal dead ends are
reserved for branches. Lithology contacts use coherent detail at multiple scales so adjacent
rock units interlock instead of meeting along smooth oval or ruler-straight boundaries.

This is still height-column fissure geometry. The 0.5.14 escarpment pass will own true
undercuts, overhangs/negative-angle faces, differential erosion and final talus deposition.
The 0.5.15 cave pass will consume rare limestone hosts and fractures; no caves or water are
generated yet.

See [Lithology and fracture framework](docs/LITHOLOGY_AND_FRACTURES.md) and the full
[terrain profile reference](docs/ARRAKIS_TERRAIN_PROFILE.md).

## Historical terrain-profile tuning — 0.5.10

Version 0.5.10 keeps the fast native chunk-generation architecture and tunes the 0.5.9
province model from in-world testing. The principal terrain parameters now live in the
world preset JSON and are serialized with the chunk generator instead of existing only as
Java constants.

The working first-region sequence is now:

| Approximate range | Province | Development intent |
|---:|---|---|
| `0–800` | Central Basin | Strict flat pure sand reserved for Arrakeen. |
| `800–~1150` | Inner Rock Foreland | More numerous 2–9 block micro-rocks plus occasional 4–28 block knobs. |
| `~1000–3020` | Shield Wall / Main Massif | Majestic large rock scale retained; steepness/overhang work remains later. |
| `~2450–3660` | Faulted Margin | Same useful width, but centerlines now meander much more strongly. |
| `~2920–5650` | Broken Rock Desert | Longer-lived outliers that become smaller/noisier with distance. |
| `~4450–6500` | Sand–Rock Transition | Sparse low remnants mixed with increasingly active sand. |
| `~5850+` | Open Erg | Native transverse dunes rise toward full suitability near 6700. |

The ranges overlap and their boundaries are warped. They are **continuous terrain fields**,
not Minecraft biome borders.

### Shield Wall passages and breakup

The main massif is made more continuous than the 0.5.7/0.5.8 field. Crossings now come from
explicit operators instead of very broad missing sectors:

- four long seed-dependent **fault ravines** with the 0.5.9 width retained but substantially
  stronger along-fault meander;
- intermittent fault segments that cut fully to the base sand, while other segments retain
  a very low resistant rocky floor;
- two broad seed-dependent **sandy corridors** that fully suppress rock at their center and
  connect the inner desert to the outer sand regions.

The massif's outer envelope still ends abruptly near the 3-km scale. The independent Broken
Rock Desert now persists much farther outward and blends a large-remnant field with a second
micro-remnant field, so formations become progressively smaller and sparser rather than
simply disappearing.

### Native transverse dunes

0.5.9 adds `NativeTransverseDuneField`, a continuous analytic world-coordinate dune field.
It does **not** run the finite 64 × 64 iterative `DuneSimulation` during chunk generation.
Instead, it carries the calibrated transverse morphology into a chunk-safe form:

```text
maximum height       = 30 blocks
dune spacing         = 525 blocks
spacing variation    = 0.18
ridge sharpness      = 3.0
valley cutoff        = 0.20
slope asymmetry      = 0.82
wind angle           = 24 degrees
surface resolution   = 1/16 block
```

Dunes are activated by `MacroGeologyField.duneSuitability()`: weak in sandy gaps of the
Broken Rock Desert, stronger through the Sand–Rock Transition, and full-strength in the
Open Erg. Tall rock suppresses them locally.

Native dunes use the existing `minecraftdune:sand` block and at most one
`minecraftdune:sand_layer` top block, so the far-erg surface retains the established
sixteenth-layer representation.

The 24-degree wind is intentionally still a global development direction. Regional wind,
terrain shelter and sand-supply fields remain later work.

### Serialized terrain profile

The active world preset contains a `terrain` JSON object. It stores basin, foreland, massif,
fault, lithology, fracture, sand-pass, broken-rock, outer-transition, and native-dune values.
`ArrakisChunkGenerator.CODEC` serializes the same object into the world's generator data.

This makes terrain tuning explicit and prevents diagnostic values from being scattered across
hard-coded constants. A new command reports the active world's loaded profile:

```mcfunction
/dune geology profile
```

The source profile is in:

```text
src/main/resources/data/minecraftdune/worldgen/world_preset/arrakis_dev.json
```

### Geology diagnostics and pregeneration

The `/dune geology` branch is re-registered through Brigadier's normal `/dune` merge path.
The bare command now works too:

```mcfunction
/dune geology
/dune geology info
/dune geology sample <x> <z>
/dune geology profile
```

Diagnostics include province weights, rock height, small-formation mask, fault mask,
fault sand-floor mask, sand-pass mask, boundary warp, dune suitability and native local
dune height.

Pregeneration remains native FULL-chunk generation:

```mcfunction
/dune geology generate
/dune geology generate_initial
/dune geology generate_nearest <1..12>
/dune geology generation status
/dune geology generation cancel
```

`generate_initial` remains a 100 vanilla-Minecraft-chunk / 1600-block radius around
absolute `(0,0)`. `generate_nearest 1` remains a player-centered 3 × 3 set of 256 × 256
geology tiles.

`/dune geology clear` is still explanatory only: geology and far-erg dunes are native terrain
and cannot safely be removed as an independent post-generation layer.

See [`docs/MACRO_GEOLOGY.md`](docs/MACRO_GEOLOGY.md),
[`docs/NATIVE_ARRAKIS_TERRAIN.md`](docs/NATIVE_ARRAKIS_TERRAIN.md),
[`docs/ARRAKIS_TERRAIN_PROFILE.md`](docs/ARRAKIS_TERRAIN_PROFILE.md), and
[`docs/NATIVE_TRANSVERSE_DUNES.md`](docs/NATIVE_TRANSVERSE_DUNES.md).

## Dune prototype

The calibrated 0.5.6 transverse laboratory remains the **v1 baseline**. Version 0.5.10 does
not change its simulation math or defaults; the native far-erg field is a separate analytic
implementation. The laboratory remains at 350-block spacing while the planetary field is
now 525 blocks.

```mcfunction
/dune dunes settings reset
```

resets to:

```text
cell_size          = 8
surface_resolution = sixteenth
max_height         = mode default (transverse 30, barchan 20)
dune_spacing       = 350
spacing_variation  = 0.18
ridge_sharpness    = 3.0
valley_cutoff      = 0.20
slope_asymmetry    = 0.82
interdune_cleanup  = 0.40
repose_angle       = 33
cascade_passes     = 25
iterations         = mode default (transverse 180, barchan 220)
transport_strength = 1.0
wind_angle         = 24
edge_blend         = 7
```

Generation commands:

```mcfunction
/dune dunes generate transverse
/dune dunes generate barchan
/dune dunes info
/dune dunes clear
```

Show or reset the active session settings:

```mcfunction
/dune dunes settings
/dune dunes settings reset
```

Transverse morphology controls include:

```mcfunction
/dune dunes settings dune_spacing <32..512>
/dune dunes settings spacing_variation <0.0..0.50>
/dune dunes settings ridge_sharpness <1.0..8.0>
/dune dunes settings valley_cutoff <0.0..0.80>
/dune dunes settings slope_asymmetry <0.0..1.0>
/dune dunes settings interdune_cleanup <0.0..1.0>
```

`slope_asymmetry` moves the crest downwind as it increases, producing a longer
windward/stoss ramp and a shorter lee face. The v1 baseline uses `0.82`.

`interdune_cleanup` is support-aware: weak low-height sand near a substantial dune body is
retained as a dune toe while similarly weak isolated remnants in broad interdune basins are
reduced. The v1 baseline uses `0.40`.

Slope and transport controls remain:

```mcfunction
/dune dunes settings repose_angle <10..45>
/dune dunes settings cascade_passes <0..64>
/dune dunes settings iterations <0..1000>
/dune dunes settings wind_angle <-360..360>
/dune dunes settings edge_blend <0..32>
/dune dunes settings transport_strength <0.0..4.0>
```

Wind angles use the generator's world-axis convention: `0` points toward `+X` (east), `90`
toward `+Z` (south), `180` toward `-X` (west), and `270` toward `-Z` (north).

Surface rendering modes remain:

| Setting | Top-surface increments | Output |
|---|---:|---|
| `whole` | 1 block | Compatibility mode. |
| `eighth` | 1/8 block | Uses even `sand_layer` states. |
| `sixteenth` | 1/16 block | Default; uses all 15 partial layer states. |

Every generated dune column contains full `minecraftdune:sand` blocks and no more than one
partial `minecraftdune:sand_layer` on top. Version 0.5.10 reuses the same layered-sand
assets for native far-erg dunes and does not replace their models or textures.

See [`docs/ARRAKIS_DUNE_PROTOTYPE.md`](docs/ARRAKIS_DUNE_PROTOTYPE.md).

`/dune` is the canonical command root. `/minecraftdune` remains a compatibility alias.

## Debug cameras and screenshots

```mcfunction
/dune camera info
/dune camera save A
/dune camera goto A
/dune camera list
/dune camera delete A
/dune camera tp 1200.5 190 -850.5 -135 12
```

Camera presets persist locally in:

```text
config/minecraftdune/debug-cameras.json
```

Take one named screenshot:

```mcfunction
/dune screenshot testG
```

Batch capture:

```mcfunction
/dune screenshot batch spacing400
/dune screenshot batch spacing400 60
/dune screenshot batch cancel
```

## Test the desert entities

```mcfunction
/summon minecraftdune:desert_hare
/give @s minecraftdune:desert_hare_spawn_egg
/summon minecraftdune:muaddib_mouse
/give @s minecraftdune:muaddib_mouse_spawn_egg
```

`desert_hare` is the renamed original entity and retains its idle, hop, sniff, and head-wiggle
animations and Rabbit-based behavior. `muaddib_mouse` uses the separate 32×32 Blockbench Java
export and texture from `blockbench/java/`.

## Build the distributable mod

```powershell
.\gradlew.bat clean build
```

The compiled JAR is written to:

```text
build/libs/minecraftdune-0.5.13.jar
```

## Package and namespace

- Java package: `com.blackenter.minecraftdune`
- Maven group: `com.blackenter.minecraftdune`
- Minecraft mod ID / resource namespace: `minecraftdune`

## Version history

See the consolidated [`PATCH_NOTES.md`](PATCH_NOTES.md) for this release and the complete
preserved project history.
