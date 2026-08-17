# Minecraft: Dune

Standalone NeoForge 1.21.1 development project for the Minecraft: Dune mod.

Current development version: **0.5.8**

The project currently contains:

- the Muad'dib desert mouse test entity, model, texture, and animations;
- the selectable **Arrakis Dev** desert world preset;
- native deterministic macro geology generated as part of the Arrakis chunk pipeline;
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

**Create a new Arrakis Dev world for 0.5.8 testing.**

Version 0.5.8 changes the overworld chunk-generator type from vanilla `minecraft:flat` to
`minecraftdune:arrakis_dev`. An existing 0.5.7 world stores its old flat generator in
`level.dat` and will not automatically switch to native geology after updating the mod.

The native generator retains the same base stratigraphy:

| Y range | Base material |
|---:|---|
| 65 and above | Air before macro geology |
| 55 to 64 | Sand |
| 45 to 54 | Sandstone |
| 0 to 44 | Stone |
| -63 to -1 | Deepslate |
| -64 | Bedrock |

Biome features, lakes, structures, and caves remain disabled in the Arrakis Dev overworld.
The Nether and End retain normal vanilla generation.

## Native macro geology — 0.5.8

The 0.5.7 `MacroGeologyField` mathematics are preserved for this milestone. The difference
is **where the resulting terrain is created**.

Previously the debug commands generated a flat chunk and then built cliffs with large
numbers of `ServerLevel#setBlock` calls. In 0.5.8 the registered
`ArrakisChunkGenerator` extends the vanilla flat generator, builds the normal Arrakis base
layers, and writes the macro-rock mass directly into `ChunkAccess` during
`fillFromNoise()`.

The generator captures the actual level seed when Minecraft creates the chunk-generator
structure state. Macro terrain therefore remains deterministic from:

```text
world seed + absolute X/Z
```

The current first-region field is intentionally unchanged:

- `0-1000`: hard-reserved flat Arrakeen / central basin;
- roughly `1000-1500`: rock transition;
- roughly `1400-3000`: Shield Wall / massif province;
- roughly `2800-4000`: eroded outer margin;
- roughly `3600-4200`: increasing open-desert dominance;
- roughly `4200+`: open desert.

The provisional rock output remains plain `minecraft:stone`, up to Y=240. The planned
0-800/800-1000 inner-region restructuring, faults/ravines, sandy passes, abrupt massif
breakup, outer broken-rock province, strata and erosion are deliberately deferred until
the native generator is validated.

Inspect the field without generating extra chunks:

```mcfunction
/dune geology info
/dune geology sample <x> <z>
```

Pregenerate the aligned 256 x 256 geology tile containing the player:

```mcfunction
/dune geology generate
```

Pregenerate a **100 vanilla-Minecraft-chunk radius** around absolute `(0,0)`:

```mcfunction
/dune geology generate_initial
```

This is a 1600-block radius. The command only asks Minecraft for normal FULL chunks;
`ArrakisChunkGenerator` creates the geology as part of those chunks.

Player-centered pregeneration uses a radius measured in 256 x 256 geology tiles:

```mcfunction
/dune geology generate_nearest <1..12>
```

`generate_nearest 1` means the current tile plus one neighboring tile in every X/Z
direction: a 3 x 3 tile square, 768 x 768 blocks, or 2304 ordinary Minecraft chunks.

Large pregeneration jobs remain tick-spread:

```mcfunction
/dune geology generation status
/dune geology generation cancel
```

`/dune geology clear` is retained only as an explanatory compatibility command. Native
terrain cannot safely be removed as a separate post-generation layer. For a clean retest,
create a fresh Arrakis Dev world or regenerate the relevant closed-world region files.

See [`docs/MACRO_GEOLOGY.md`](docs/MACRO_GEOLOGY.md) and
[`docs/NATIVE_ARRAKIS_TERRAIN.md`](docs/NATIVE_ARRAKIS_TERRAIN.md).

## Dune prototype

The calibrated 0.5.6 transverse generator remains the **v1 baseline**. Version 0.5.8 does
not change its morphology math or defaults.

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
partial `minecraftdune:sand_layer` on top. Version 0.5.8 does not replace the existing
layered-sand assets.

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

## Test the Muad'dib entity

```mcfunction
/summon minecraftdune:muaddib_mouse
/give @s minecraftdune:muaddib_mouse_spawn_egg
```

## Build the distributable mod

```powershell
.\gradlew.bat clean build
```

The compiled JAR is written to:

```text
build/libs/minecraftdune-0.5.8.jar
```

## Package and namespace

- Java package: `com.blackenter.minecraftdune`
- Maven group: `com.blackenter.minecraftdune`
- Minecraft mod ID / resource namespace: `minecraftdune`

## Version history

See [`PATCH_NOTES-0.5.8.md`](PATCH_NOTES-0.5.8.md) for this release and [`PATCH_NOTES.md`](PATCH_NOTES.md) for the preserved earlier history.
