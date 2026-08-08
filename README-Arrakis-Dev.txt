Minecraft: Dune — Arrakis Dev
==============================

Purpose
-------
Arrakis Dev is the flat development world used for deterministic testing of the
Minecraft: Dune terrain, entities, and survival systems.

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

Dune laboratory
---------------
Version 0.5.1 includes a command-driven deterministic dune prototype. Basic commands:

/minecraftdune dunes generate transverse
/minecraftdune dunes generate barchan
/minecraftdune dunes info
/minecraftdune dunes clear
/minecraftdune dunes settings

The simulation is 64 x 64 cells. The horizontal output scale is adjustable at runtime:

/minecraftdune dunes settings cell_size 4

cell_size 2 -> 128 x 128 blocks (original prototype)
cell_size 4 -> 256 x 256 blocks
cell_size 8 -> 512 x 512 blocks

Peak height and slope behavior can also be tuned:

/minecraftdune dunes settings max_height 10
/minecraftdune dunes settings stable_slope 0.75
/minecraftdune dunes settings cascade_passes 4

See docs/ARRAKIS_DUNE_PROTOTYPE.md for every exposed parameter and its valid range.

Notes
-----
- Settings are temporary development-session state and reset on process restart.
- Non-sand blocks are preserved by the dune commands.
- Dune generate/clear commands are destructive to sand above the Y=64 test surface.
- If cell_size is reduced after generating a larger field, clear the old scale explicitly,
  for example: /minecraftdune dunes clear 8
