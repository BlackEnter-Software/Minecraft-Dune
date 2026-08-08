# Minecraft: Dune

Standalone NeoForge 1.21.1 development project for the Minecraft: Dune mod.

Current development version: **0.5.0**

The project currently contains:

- the Muad'dib desert mouse test entity, model, texture, and animations;
- the selectable **Arrakis Dev** flat desert world preset;
- an operator-only deterministic dune prototype for the Arrakis Dev world.

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

The first run downloads:

1. The official Gradle wrapper JAR from the NeoForge 1.21.1 MDK repository.
2. Gradle 9.2.1.
3. Minecraft, NeoForge, mappings, and development dependencies.

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

The 0.5.0 prototype generates a deterministic 128 x 128 block dune test region aligned
to the world grid. It uses a reduced 64 x 64 sand-thickness simulation with two blocks
per simulation cell.

The simulation includes:

- a fixed regional wind directed 24 degrees toward positive X and positive Z;
- saltation-like directional sand hops;
- reduced erosion in lee-side wind shadow;
- repeated slope stabilization approximating sand avalanching;
- deterministic regional seeding from the world seed and region coordinates;
- edge blending back into the flat Arrakis Dev surface.

The prototype intentionally supports only two initial dune families:

- `transverse` for a high-sand-supply ridge field;
- `barchan` for lower-supply isolated crescent dunes.

Enable cheats or use operator permission level 2, stand inside the target 128 x 128
region, and run one of these commands:

```mcfunction
/minecraftdune dunes generate transverse
/minecraftdune dunes generate barchan
/minecraftdune dunes info
/minecraftdune dunes clear
```

The region is aligned to multiples of 128 blocks. Running the same mode again in the
same region and with the same world seed reproduces the same height field.

`generate` and `clear` are destructive development commands. They modify natural sand
between Y=65 and Y=84 in the selected region. Non-sand blocks are preserved, but these
commands should not be used around builds that rely on placed sand.

See [`docs/ARRAKIS_DUNE_PROTOTYPE.md`](docs/ARRAKIS_DUNE_PROTOTYPE.md) for the algorithm,
limitations, and testing checklist.

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
build/libs/minecraftdune-0.5.0.jar
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

See [`PATCH_NOTES.md`](PATCH_NOTES.md). The current public repository contains four
historical commits, while the changelog separates the Muad'dib work into the development
milestones that led to the combined model commit.
