Minecraft: Dune — Arrakis Dev
=============================

Purpose
-------
Arrakis Dev is an integrated development world preset for testing entities, survival
systems, structures, terrain tools, and future Gameplay Arrakis world generation.

Overworld layers
----------------
Y  65 and above : Air
Y  55 to 64     : Sand (10 blocks)
Y  45 to 54     : Sandstone (10 blocks)
Y   0 to 44     : Stone (45 blocks)
Y -63 to -1     : Deepslate (63 blocks)
Y -64           : Bedrock (1 block)

The surface block is at Y=64, one block above vanilla sea level Y=63.

Generation settings
-------------------
Biome: minecraft:desert
Biome features: disabled
Lakes: disabled
Structures: disabled
Caves: none, because this is a flat generator
Nether and End: normal vanilla generation

Creating the world
------------------
1. Run the development client:

   .\gradlew.bat runClient

2. Create a new world.
3. Cycle the World Type button until "Arrakis Dev" appears.
4. Enable cheats for the prototype terrain commands.

Dune prototype commands
-----------------------
The command operates on the 128 x 128 block region containing the command source.
Regions are aligned to multiples of 128 blocks.

Generate a high-sand-supply transverse field:

   /minecraftdune dunes generate transverse

Generate lower-supply isolated barchans:

   /minecraftdune dunes generate barchan

Display the selected region and prototype parameters:

   /minecraftdune dunes info

Remove prototype sand above the original Y=64 surface:

   /minecraftdune dunes clear

Prototype behavior
------------------
- The 128 x 128 block output is calculated from a 64 x 64 simulation grid.
- One simulation cell corresponds to 2 x 2 Minecraft columns.
- Wind direction is fixed at 24 degrees toward positive X and positive Z.
- The same world seed, region, and mode produce the same result.
- Sand transport is mass-conserving apart from negligible floating-point drift.
- Dune heights are blended to zero near the region boundary.
- Maximum added dune height is 20 blocks.

Safety and limitations
----------------------
- These are destructive development commands, not gameplay mechanics.
- Natural sand between Y=65 and Y=84 may be replaced or removed.
- Non-sand blocks are preserved, so structures can interrupt dune columns.
- The command runs synchronously and may pause the integrated server briefly.
- The simulation does not yet use rock obstacles, regional wind maps, chunk caches,
  storm-driven migration, or structure reservation masks.
- Existing worlds are not automatically changed. Run a command to create dunes.
