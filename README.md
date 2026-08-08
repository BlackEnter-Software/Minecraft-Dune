# Minecraft: Dune

Standalone NeoForge 1.21.1 development project for the Minecraft: Dune mod.

Current development version: **0.5.1**

The project currently contains:

- the Muad'dib desert mouse test entity, model, texture, and animations;
- the selectable **Arrakis Dev** flat desert world preset;
- an operator-only deterministic dune prototype for the Arrakis Dev world;
- live in-game tuning commands for the dune prototype.

## Requirements

- Minecraft Java Edition 1.21.1
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

The dune prototype uses a deterministic 64 x 64 sand-thickness simulation and expands
that grid into Minecraft blocks. Version 0.5.1 makes the important prototype parameters
changeable from commands so dune scale and shape can be tuned without rebuilding the mod.

Generation commands:

```mcfunction
/minecraftdune dunes generate transverse
/minecraftdune dunes generate barchan
/minecraftdune dunes info
/minecraftdune dunes clear
```

Show or reset the current tuning values:

```mcfunction
/minecraftdune dunes settings
/minecraftdune dunes settings reset
```

Change individual values:

```mcfunction
/minecraftdune dunes settings cell_size 4
/minecraftdune dunes settings max_height 10
/minecraftdune dunes settings stable_slope 0.75
/minecraftdune dunes settings cascade_passes 4
/minecraftdune dunes settings iterations 180
/minecraftdune dunes settings wind_angle 24
/minecraftdune dunes settings edge_blend 10
/minecraftdune dunes settings transport_strength 1.0
```

The most useful values for the current size/steepness tuning are:

- `cell_size`: horizontal Minecraft blocks represented by one simulation cell. The
  original value is 2, giving a 128 x 128 region. A value of 4 gives 256 x 256 and a
  value of 8 gives 512 x 512. Increasing it also stretches slopes horizontally.
- `max_height`: maximum added dune height. `0` restores the dune-mode default
  (transverse 18, barchan 20).
- `stable_slope`: maximum simulation height difference before cascading. Lower values
  encourage gentler simulated slopes.
- `cascade_passes`: number of slope-relaxation passes after each transport iteration.
  More passes generally smooth the sand field further.

A useful first comparison against the original prototype is:

```mcfunction
/minecraftdune dunes settings cell_size 4
/minecraftdune dunes settings max_height 10
/minecraftdune dunes settings stable_slope 0.75
/minecraftdune dunes settings cascade_passes 4
/minecraftdune dunes generate transverse
```

Settings are development-session state and reset when the game/server process restarts.
The same world seed, aligned region, dune mode, and settings produce the same result.

`generate` and `clear` are destructive development commands. They modify sand above the
Y=64 Arrakis Dev surface. Non-sand blocks are preserved. Version 0.5.1 clears prototype
sand up to Y=96 inside the selected footprint so lowering `max_height` removes old peaks.
If you reduce `cell_size` after generating a larger test field, clear the old footprint
explicitly first, for example `/minecraftdune dunes clear 8`.

See [`docs/ARRAKIS_DUNE_PROTOTYPE.md`](docs/ARRAKIS_DUNE_PROTOTYPE.md) for the algorithm,
parameter ranges, limitations, and testing procedure.

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
build/libs/minecraftdune-0.5.1.jar
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
