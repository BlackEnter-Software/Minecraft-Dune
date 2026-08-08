# Minecraft: Dune

Standalone NeoForge 1.21.1 development project for the Minecraft: Dune mod.

This initial build contains one test entity: the Muad'dib desert mouse.

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

## Test the entity

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
.\gradlew.bat build
```

The compiled JAR is written to:

```text
build/libs/minecraftdune-0.1.0.jar
```

## Package and namespace

- Java package: `com.blackenter.minecraftdune`
- Maven group: `com.blackenter.minecraftdune`
- Minecraft mod ID / resource namespace: `minecraftdune`

## Current entity behavior

`MuaddibMouseEntity` subclasses vanilla `Rabbit`. It therefore inherits
rabbit hopping and its existing animal AI while desert-specific behavior
is developed. It also assigns a high pathfinding preference to sand.

Natural biome spawning is not enabled yet. Summoning and the spawn egg are
deliberately the only spawn mechanisms in this test version.

## Blockbench

Open:

```text
blockbench/muaddib_mouse.bbmodel
```

The corrected model is upright in Blockbench. See
`docs/BLOCKBENCH_WORKFLOW.md` before exporting Java geometry.
