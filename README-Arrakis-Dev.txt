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

Macro geology laboratory — 0.5.7
--------------------------------
The world origin (0,0) is the reference center of the first Gameplay Arrakis region.

The first 1000 blocks are hard-reserved as a flat Arrakeen basin. Outside it, a
deterministic low-frequency field permits an irregular Shield Wall / massif province,
then an eroded outer margin, before open desert dominates around 3600-4200 blocks.

Inspect the field:
/dune geology info
/dune geology sample <x> <z>

Generate/clear the current aligned 256 x 256 test tile:
/dune geology generate
/dune geology clear

Generate the initial 100-Minecraft-chunk / 1600-block radius around absolute (0,0):
/dune geology generate_initial

Generate a square of 256x256 geology tiles centered on the player's current tile:
/dune geology generate_nearest <1..12>

Radius 1 means 3x3 tiles: the player's tile plus one neighboring tile in every X/Z
direction. Radius 2 means 5x5 tiles.

Large-job controls:
/dune geology generation status
/dune geology generation cancel

The initial and nearest commands are spread over server ticks to avoid one huge watchdog
stall. The current rock output is deliberately crude minecraft:stone up to Y=240.

Dune laboratory — frozen 0.5.6 v1 baseline
------------------------------------------
The 0.5.7 release preserves the 0.5.6 transverse v1 implementation unchanged.

Default transverse development profile:
cell_size          = 8
surface_resolution = sixteenth
max_height         = 30 (transverse mode default)
dune_spacing       = 350
spacing_variation  = 0.18
ridge_sharpness    = 3.0
valley_cutoff      = 0.20
slope_asymmetry    = 0.82
interdune_cleanup  = 0.40
repose_angle       = 33
cascade_passes     = 25
iterations         = 180 (transverse mode default)
transport_strength = 1.0
wind_angle         = 24
edge_blend         = 7

Basic commands:
/dune dunes generate transverse
/dune dunes generate barchan
/dune dunes info
/dune dunes clear
/dune dunes settings
/dune dunes settings reset

Wind angles use world axes, not player yaw: 0 points toward +X (east), 90 toward +Z
(south), 180 toward -X (west), and 270 toward -Z (north).

Surface output controls:
/dune dunes settings surface_resolution whole
/dune dunes settings surface_resolution eighth
/dune dunes settings surface_resolution sixteenth

Sixteenth remains the default. Generated columns use full minecraftdune:sand below and at
most one minecraftdune:sand_layer block on top.

Repeatable screenshots
----------------------
/dune camera save A
/dune camera goto A
/dune screenshot test
/dune screenshot batch morphology
/dune screenshot batch morphology 60
/dune screenshot batch cancel

Camera presets persist in config/minecraftdune/debug-cameras.json.
/minecraftdune remains a compatibility alias for /dune.

Notes
-----
- Gradle third-party development-mod dependencies are intentionally not configured.
- Settings are temporary development-session state and reset on process restart.
- Non-sand blocks are preserved by the dune commands.
- Dune generate/clear commands are destructive to sand above the Y=64 test surface.
- Barchan generation is intentionally unchanged.
- Detailed rock morphology and terrain-projected wind remain deferred.
- NeoForge remains 21.1.248.
