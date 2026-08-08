# Minecraft: Dune

Standalone NeoForge 1.21.1 development project for the Minecraft: Dune mod.

Current development version: **0.5.2**

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

Version 0.5.2 focuses on tuning **transverse dunes** in the Arrakis Dev laboratory. The
simulation stays at 64 x 64 cells and the synchronous output remains capped at 512 x 512
Minecraft blocks (`cell_size 8`). Barchan generation remains available but is deliberately
deferred while the transverse morphology is tuned.

Generation commands:

```mcfunction
/minecraftdune dunes generate transverse
/minecraftdune dunes generate barchan
/minecraftdune dunes info
/minecraftdune dunes clear
```

Show or reset the active session settings:

```mcfunction
/minecraftdune dunes settings
/minecraftdune dunes settings reset
```

The 0.5.2 transverse controls are:

```mcfunction
/minecraftdune dunes settings cell_size 8
/minecraftdune dunes settings max_height 18
/minecraftdune dunes settings dune_spacing 100
/minecraftdune dunes settings spacing_variation 0.18
/minecraftdune dunes settings ridge_sharpness 4.0
/minecraftdune dunes settings valley_cutoff 0.20
/minecraftdune dunes settings repose_angle 33
/minecraftdune dunes settings cascade_passes 16
/minecraftdune dunes settings iterations 180
/minecraftdune dunes settings wind_angle 24
/minecraftdune dunes settings edge_blend 7
/minecraftdune dunes settings transport_strength 1.0
```

Important 0.5.2 changes:

- `dune_spacing` controls transverse crest spacing directly in Minecraft blocks instead of
  inheriting a fixed wavelength from simulation-cell scale.
- `ridge_sharpness` narrows or broadens the ridge body.
- `valley_cutoff` creates genuinely flat interdune terrain by removing weak low-level sand.
- `spacing_variation` makes ridge spacing and alignment less mechanically periodic.
- `stable_slope` is removed. `repose_angle` now controls the allowed physical slope in
  degrees.
- `cascade_passes` now runs after height mapping and accepts 0-64 passes, so the cascade
  result is no longer normalized back toward the original height profile.

The 0.5.2 default is aimed at roughly 100 blocks between transverse crests in a 512 x 512
test area. Settings remain development-session state and reset when the game/server process
restarts. The same world seed, aligned region, dune mode, and settings remain deterministic.

`generate` and `clear` are destructive development commands for sand above the Y=64 Arrakis
Dev surface. Non-sand blocks are preserved. If you reduce `cell_size` after generating a
larger footprint, clear the old footprint explicitly first, for example:

```mcfunction
/minecraftdune dunes clear 8
```

See [`docs/ARRAKIS_DUNE_PROTOTYPE.md`](docs/ARRAKIS_DUNE_PROTOTYPE.md) for parameter ranges,
behavior, the revised simulation pipeline, and screenshot test profiles.

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
build/libs/minecraftdune-0.5.2.jar
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
