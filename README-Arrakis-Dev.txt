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
Version 0.5.2 concentrates on transverse-dune morphology. The simulation remains 64 x 64
cells and the largest synchronous test footprint remains 512 x 512 Minecraft blocks.

Basic commands:
/minecraftdune dunes generate transverse
/minecraftdune dunes generate barchan
/minecraftdune dunes info
/minecraftdune dunes clear
/minecraftdune dunes settings

The 0.5.2 default horizontal scale is:
cell_size 8 -> 512 x 512 blocks

Transverse shape controls:
/minecraftdune dunes settings dune_spacing 100
/minecraftdune dunes settings spacing_variation 0.18
/minecraftdune dunes settings ridge_sharpness 4.0
/minecraftdune dunes settings valley_cutoff 0.20

Slope controls:
/minecraftdune dunes settings repose_angle 33
/minecraftdune dunes settings cascade_passes 16

`stable_slope` was removed in 0.5.2. Cascading now works in Minecraft-scale heights using
`repose_angle` and occurs after height mapping, so later normalization no longer hides most
of its effect.

See docs/ARRAKIS_DUNE_PROTOTYPE.md for every exposed parameter, valid ranges, and three
recommended screenshot profiles.

Notes
-----
- Settings are temporary development-session state and reset on process restart.
- Non-sand blocks are preserved by the dune commands.
- Dune generate/clear commands are destructive to sand above the Y=64 test surface.
- Barchan generation is intentionally unchanged in 0.5.2 while transverse dunes are tuned.
- If cell_size is reduced after generating a larger field, clear the old scale explicitly,
  for example: /minecraftdune dunes clear 8
