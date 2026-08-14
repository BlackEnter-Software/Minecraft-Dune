Minecraft: Dune â€” Arrakis Dev
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

Dune laboratory â€” 0.5.5
-----------------------
The 0.5.5 laboratory keeps the 64 x 64 simulation grid, 512 x 512 maximum synchronous
footprint, 0.5.4 fractional dune sand, and 0.5.3 fixed-camera screenshot tools.

Default transverse development profile:
cell_size          = 8
surface_resolution = sixteenth
max_height         = 30 (transverse mode default)
dune_spacing       = 350
spacing_variation  = 0.18
ridge_sharpness    = 3.0
valley_cutoff      = 0.20
slope_asymmetry    = 0.60
interdune_cleanup  = 0.30
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

New 0.5.5 morphology controls:
/dune dunes settings slope_asymmetry <0.0..1.0>
/dune dunes settings interdune_cleanup <0.0..1.0>

slope_asymmetry changes the seeded transverse dune profile before transport. The default
0.60 gives the dune a longer windward ramp and a shorter lee face.

interdune_cleanup selectively removes weak isolated low-sand remnants while retaining
low-height dune feet that are locally supported by a substantial dune body. The transport
stage also mildly attenuates stochastic movement on nearly flat transverse sand.

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
- Settings are temporary development-session state and reset on process restart.
- Non-sand blocks are preserved by the dune commands.
- Dune generate/clear commands are destructive to sand above the Y=64 test surface.
- Barchan generation is intentionally unchanged while transverse dunes are tuned.
- The 0.5.5 patch does not modify the 0.5.4 sand texture/model assets.
- NeoForge remains 21.1.248.