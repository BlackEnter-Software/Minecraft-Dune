Minecraft: Dune — Arrakis Dev
==============================

Native terrain generation — 0.5.8
---------------------------------

IMPORTANT:
Create a NEW Arrakis Dev world after installing 0.5.8.

Existing 0.5.7 worlds keep the old minecraft:flat generator stored in level.dat and will
not automatically change to the new native generator.

Base overworld stratigraphy
---------------------------
Y  65 and above : Air before native macro geology
Y  55 to 64     : Sand (10 blocks)
Y  45 to 54     : Sandstone (10 blocks)
Y   0 to 44     : Stone (45 blocks)
Y -63 to -1     : Deepslate (63 blocks)
Y -64           : Bedrock (1 block)

Biome: minecraft:desert
Biome features: disabled
Lakes: disabled
Structures: disabled
Caves: disabled by the flat-generator base
Nether and End: normal vanilla generation

The overworld generator type is now:
minecraftdune:arrakis_dev

The generator extends Minecraft's FlatLevelSource. It first creates the base layers above,
then adds the current MacroGeologyField rock mass directly to ChunkAccess during normal
chunk fill/noise generation.

Macro geology
-------------
The 0.5.7 field mathematics are intentionally unchanged in 0.5.8:

0-1000       exact flat Arrakeen basin
~1000-1500   rock transition
~1400-3000   Shield Wall / massif
~2800-4000   eroded margin
~3600-4200   open-desert transition
~4200+       open desert

The current prototype rock remains minecraft:stone and can reach Y=240.

Inspect:
/dune geology info
/dune geology sample <x> <z>

Pregenerate the current 256x256 geology tile:
/dune geology generate

Pregenerate 100 vanilla Minecraft chunks / 1600 blocks outward from absolute (0,0):
/dune geology generate_initial

Pregenerate around the player's current 256x256 geology tile:
/dune geology generate_nearest <1..12>

Radius 1 = 3x3 geology tiles = 768x768 blocks = 2304 normal Minecraft chunks.
Radius 2 = 5x5 geology tiles.

Job controls:
/dune geology generation status
/dune geology generation cancel

These commands do not place geology blocks. They only request FULL Minecraft chunks;
native chunk generation creates the geology.

The old clear behavior is no longer applicable:
/dune geology clear
prints an explanation instead of modifying terrain.

Dune laboratory — frozen 0.5.6 v1 baseline
------------------------------------------
The native-terrain migration does not change the calibrated transverse dune implementation.

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

Basic dune commands:
/dune dunes generate transverse
/dune dunes generate barchan
/dune dunes info
/dune dunes clear
/dune dunes settings
/dune dunes settings reset

Repeatable screenshots
----------------------
/dune camera save A
/dune camera goto A
/dune screenshot test
/dune screenshot batch morphology
/dune screenshot batch morphology 60
/dune screenshot batch cancel

Notes
-----
- Gradle third-party development-mod dependencies remain intentionally unconfigured.
- Distant Horizons can be installed manually for macro-scale inspection.
- Detailed rock morphology remains deferred until native generation is validated.
- The planned inner 0-800/800-1000 restructuring is NOT part of 0.5.8.
- Fault ravines, sandy Shield Wall passes, abrupt outer breakup and the additional
  sand/rock transition province are NOT part of 0.5.8.
- NeoForge remains 21.1.248.
