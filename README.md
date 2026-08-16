# Minecraft: Dune

Standalone NeoForge 1.21.1 development project for the Minecraft: Dune mod.

Current development version: **0.5.7**

The project currently contains:

- the Muad'dib desert mouse test entity, model, texture, and animations;
- the selectable **Arrakis Dev** flat desert world preset;
- an operator-only deterministic dune prototype for the Arrakis Dev world;
- live in-game tuning commands for the dune prototype;
- custom full and fractional dune-sand blocks with selectable surface resolution;
- persistent fixed cameras and repeatable named/batch screenshots for terrain testing;
- a deterministic world-scale macro-geology prototype centered on `(0,0)`.

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

The Gradle project itself intentionally has no third-party runtime mods declared. Optional
terrain/rendering/test mods such as Distant Horizons can be installed in the development
environment separately.

## Arrakis Dev world

Create a new world and cycle the **World Type** button until **Arrakis Dev** appears.
The preset generates a uniform desert surface at Y=64 with this column layout:

| Y range | Material |
|---:|---|
| 65 and above | Air |
| 55 to 64 | Sand |
| 45 to 54 | Sandstone |
| 0 to 44 | Stone |
| -63 to -1 | Deepslate |
| -64 | Bedrock |

Biome features, lakes, structures, and caves are disabled in the Arrakis Dev overworld.
The Nether and End retain normal vanilla generation.

## Macro geology prototype

Version 0.5.7 starts the upstream terrain framework while leaving the calibrated 0.5.6
transverse dune generator frozen.

The field is deterministic from the world seed plus absolute X/Z coordinates. Geological
province values remain continuous inputs rather than being encoded directly as Minecraft
biomes.

First-region targets:

- `0-1000`: hard-reserved flat Arrakeen / central basin;
- roughly `1000-1500`: rock transition;
- roughly `1400-3000`: Shield Wall / massif province;
- roughly `2800-4000`: broken eroded outer margin;
- roughly `3600-4200`: increasing open-desert dominance;
- roughly `4200+`: open desert.

Outside the protected 1000-block basin, low-frequency seeded boundary warp and formation
continuity fields prevent the rock province from becoming a perfect circular ring.

Inspect the field without changing blocks:

```mcfunction
/dune geology info
/dune geology sample <x> <z>
```

Materialize the aligned 256 x 256 geology tile containing the player:

```mcfunction
/dune geology generate
/dune geology clear
```

For a fresh world, generate a **100 vanilla-Minecraft-chunk radius** around absolute
`(0,0)`:

```mcfunction
/dune geology generate_initial
```

That is a 1600-block radius. The command forces/generates the underlying Arrakis Dev chunks
as well as materializing macro rock, making it suitable for a Distant Horizons overview.
Because this covers tens of thousands of Minecraft chunks, it runs as a tick-spread job
rather than trying to finish inside one command tick.

`generate_nearest` is now player/source-centered and its argument is a radius measured in
**256 x 256 geology tiles**:

```mcfunction
/dune geology generate_nearest <1..12>
```

`generate_nearest 1` generates the tile containing the player plus one neighboring tile in
every X/Z direction: a 3 x 3 tile square, or 768 x 768 blocks total. Radius 2 is 5 x 5
tiles, and so on.

Large-job controls:

```mcfunction
/dune geology generation status
/dune geology generation cancel
```

The first rock output is deliberately crude `minecraft:stone`, with provisional maximum
surface Y=240. Large jobs use an additive fast path and do not perform the expensive
Y=240-downward cleanup scan in every chunk; use the normal single-tile `generate`/`clear`
commands when locally retesting changed geology.

See [`docs/MACRO_GEOLOGY.md`](docs/MACRO_GEOLOGY.md).

## Dune prototype

Version 0.5.6 froze the calibrated transverse dune generator as the **v1 baseline** built
around the 0.5.4 fractional dune surface. Version 0.5.7 preserves that implementation and
its defaults unchanged while macro geology is developed upstream.

The tested Arrakis Dev baseline is:

```mcfunction
/dune dunes settings reset
```

which resets to:

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

`slope_asymmetry` changes the seeded transverse cross-section before transport. Higher values
move the crest downwind, producing a longer windward/stoss ramp and a shorter lee face. The
v1 baseline uses `0.82`.

`interdune_cleanup` is support-aware rather than another global cutoff. Weak low-height sand
near a substantial dune body is retained as a dune toe, while similarly weak isolated
remnants in broad interdune basins are reduced. The v1 baseline uses `0.40`.

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
points toward `+Z` (south), `180` points toward `-X` (west), and `270` points toward `-Z`
(north).

Surface rendering modes remain:

| Setting | Top-surface increments | Output |
|---|---:|---|
| `whole` | 1 block | Compatibility mode. |
| `eighth` | 1/8 block | Uses even `sand_layer` states. |
| `sixteenth` | 1/16 block | Default; uses all 15 partial layer states. |

Every generated column contains full `minecraftdune:sand` blocks and no more than one
partial `minecraftdune:sand_layer` on top. Version 0.5.7 does not replace or modify the
layered-sand textures/models.

Settings are development-session state and reset when the game/server process restarts.
The same world seed, aligned region, dune mode, and settings remain deterministic.

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
build/libs/minecraftdune-0.5.7.jar
```

## Package and namespace

- Java package: `com.blackenter.minecraftdune`
- Maven group: `com.blackenter.minecraftdune`
- Minecraft mod ID / resource namespace: `minecraftdune`

## Version history

See [`PATCH_NOTES.md`](PATCH_NOTES.md).
