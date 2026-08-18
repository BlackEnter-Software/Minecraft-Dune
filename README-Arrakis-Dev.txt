Minecraft: Dune — Arrakis Dev
==============================

Geological provinces + native far-erg dunes — 0.5.9
---------------------------------------------------

For a clean morphology comparison, create a NEW Arrakis Dev world. 0.5.8 saves use the same
native generator codec, but already-generated chunks keep their 0.5.8 terrain and can seam
against newly generated 0.5.9 chunks.

Base overworld stratigraphy
---------------------------
Y  65 and above : Native rock / dune terrain where fields require it
Y  55 to 64     : Sand (10-block base layer)
Y  45 to 54     : Sandstone (10 blocks)
Y   0 to 44     : Stone (45 blocks)
Y -63 to -1     : Deepslate (63 blocks)
Y -64           : Bedrock (1 block)

Biome: minecraft:desert
Biome features: disabled
Lakes: disabled
Structures: disabled
Caves: disabled by the flat-generator base
Generator type: minecraftdune:arrakis_dev
Nether and End: normal vanilla generation

Province sequence
-----------------
0-800          CENTRAL_BASIN
               Exact flat pure sand. Arrakeen reservation.

800-~1120      INNER_ROCK_FORELAND
               Mostly sand. Sparse small formations, roughly 5-28 blocks high.

~1000-3020     SHIELD_WALL_MASSIF
               Main high rock body. Majestic 0.5.8 scale retained.

~2450-3660     FAULTED_MARGIN
               Overlaps the outer massif. Four long narrow rocky fault-ravine traces.

~2920-4450     BROKEN_ROCK_DESERT
               Independent detached rock outliers after the massif terminates.

~3900-5400     SAND_ROCK_TRANSITION
               Lower remnant rock plus increasing native dune activity.

~4700+         OPEN_ERG
               Native transverse dunes. Full suitability around effective radius 5250.

All non-central ranges overlap and are distorted by low-frequency world-seed fields.

Shield Wall crossings
---------------------
Two separate crossing types are generated:

1. Fault ravines
   - narrow;
   - non-radial structural traces;
   - usually low rocky floors.

2. Sandy corridors
   - two major seed-dependent routes;
   - slowly curved with radius;
   - center fully suppresses native rock;
   - continues from the Shield Wall into the broken-rock region.

Native transverse dunes
-----------------------
The far desert does NOT run the finite 64x64 DuneSimulation per chunk. Native generation
uses a continuous coordinate field with the calibrated transverse profile:

maximum height       = 30
spacing              = 350
spacing variation    = 0.18
ridge sharpness      = 3.0
valley cutoff        = 0.20
slope asymmetry      = 0.82
wind angle           = 24 degrees
surface resolution   = sixteenth

Native dune material above the base surface uses minecraftdune:sand and a single optional
minecraftdune:sand_layer top block.

The fixed 24-degree wind is temporary until regional wind exposure/shelter is implemented.

Terrain inspection
------------------
/dune geology
/dune geology info
/dune geology sample <x> <z>

The bare /dune geology command now works and reports the current terrain sample.

Pregeneration
-------------
/dune geology generate
/dune geology generate_initial
/dune geology generate_nearest <1..12>
/dune geology generation status
/dune geology generation cancel

/dune geology generate_initial
    = 100 vanilla-Minecraft-chunk / 1600-block radius around absolute (0,0)

/dune geology generate_nearest 1
    = current 256x256 geology tile + one neighbor in every direction
    = 3x3 geology tiles
    = 768x768 blocks
    = 2304 normal Minecraft chunks

Distant Horizons can also generate the native terrain independently.

Dune laboratory — frozen v1 baseline
------------------------------------
The finite calibrated DuneSimulation remains available unchanged for development experiments:

/dune dunes generate transverse
/dune dunes generate barchan
/dune dunes info
/dune dunes clear
/dune dunes settings
/dune dunes settings reset

The native far-erg dune field is a separate chunk-safe implementation; it does not replace
the laboratory transport/cascade code.

Deferred after 0.5.9
--------------------
- sandstone strata / bedding;
- caprock and true mesa/butte morphology;
- talus / scree;
- yardangs;
- thermal and salt weathering;
- terrain-projected wind shelter/exposure;
- regional wind direction changes;
- ecological/spawn use of the new provinces.

NeoForge remains 21.1.248.
