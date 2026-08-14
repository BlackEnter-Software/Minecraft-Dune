# Minecraft: Dune

Standalone NeoForge 1.21.1 development project for the Minecraft: Dune mod.

Current development version: **0.5.6**

The project currently contains:

- the Muad'dib desert mouse test entity, model, texture, and animations;
- the selectable **Arrakis Dev** flat desert world preset;
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

## Dune prototype

Version 0.5.6 freezes the calibrated transverse dune generator as the **v1 baseline** built
around the 0.5.4 fractional dune surface. The simulation remains a 64 x 64 deterministic
laboratory grid with `cell_size 8`, giving a 512 x 512 Minecraft-block test field. Barchan
generation remains available but is still deferred while upstream terrain systems are built.

The tested Arrakis Dev baseline is now:

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
v1 baseline uses `0.82`; useful tuning remains available without changing the frozen default.

`interdune_cleanup` is support-aware rather than another global cutoff. Weak low-height sand
near a substantial dune body is retained as a dune toe, while similarly weak isolated
remnants in broad interdune basins are reduced. A mild low-relief attenuation also reduces
stochastic transport texture on nearly flat transverse sand. The v1 baseline uses `0.40`.

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
(north). This is independent of Minecraft player/camera yaw.

Surface rendering modes remain:

| Setting | Top-surface increments | Output |
|---|---:|---|
| `whole` | 1 block | Compatibility mode. |
| `eighth` | 1/8 block | Uses even `sand_layer` states. |
| `sixteenth` | 1/16 block | Default; uses all 15 partial layer states. |

Every generated column contains full `minecraftdune:sand` blocks and no more than one
partial `minecraftdune:sand_layer` on top. The 0.5.6 baseline does not replace or modify the
0.5.4 sand textures/models, so the current layered-sand assets remain intact.

Settings are development-session state and reset when the game/server process restarts.
The same world seed, aligned region, dune mode, and settings remain deterministic.

`generate` and `clear` are destructive development commands for sand above the Y=64 Arrakis
Dev surface. Non-sand blocks are preserved. If you reduce `cell_size` after generating a
larger footprint, clear the old footprint explicitly first:

```mcfunction
/dune dunes clear 8
```

See [`docs/ARRAKIS_DUNE_PROTOTYPE.md`](docs/ARRAKIS_DUNE_PROTOTYPE.md) for ranges, pipeline
details, and repeatable morphology test profiles.

`/dune` is the canonical command root. `/minecraftdune` remains a compatibility alias.
## Debug cameras and screenshots

Position the player for a useful comparison view, then save and recall the exact dimension,
position, yaw, and pitch:

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

This creates `screenshots/dune_testG.png`. Existing names are not overwritten; later
captures use `_2`, `_3`, and subsequent numeric suffixes.

To capture every saved camera in alphabetical order:

```mcfunction
/dune screenshot batch spacing400
/dune screenshot batch spacing400 60
/dune screenshot batch cancel
```

The optional final number is the stabilization delay in client ticks; the default is 40.
During a batch the client waits for each server-authoritative teleport, locks the saved
camera through a rendered frame, hides the HUD, takes the screenshot, and restores the
previous HUD state when the batch finishes or is cancelled.

## Test the Muad'dib entity

Create a world with cheats enabled and run:

```mcfunction
/summon minecraftdune:muaddib_mouse
```

Or obtain its spawn egg:

```mcfunction
/give @s minecraftdune:muaddib_mouse_spawn_egg
```

The egg also appears in the Spawn Eggs creative tab.

## Build the distributable mod

```powershell
.\gradlew.bat clean build
```

The compiled JAR is written to:

```text
build/libs/minecraftdune-0.5.6.jar
```

## Package and namespace

- Java package: `com.blackenter.minecraftdune`
- Maven group: `com.blackenter.minecraftdune`
- Minecraft mod ID / resource namespace: `minecraftdune`

## Current entity behavior

`MuaddibMouseEntity` subclasses vanilla `Rabbit`. It inherits rabbit hopping and the
existing animal AI while desert-specific behavior is developed. It also assigns a high
pathfinding preference to sand.

Natural biome spawning is not enabled yet. Summoning and the spawn egg are deliberately
the only spawn mechanisms in this development version.

## Blockbench

Open:

```text
blockbench/muaddib_mouse.bbmodel
```

See `docs/BLOCKBENCH_WORKFLOW.md` before exporting revised Java geometry.

## Version history

See [`PATCH_NOTES.md`](PATCH_NOTES.md).
