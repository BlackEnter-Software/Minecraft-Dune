Minecraft: Dune — Arrakis Dev
==============================

Escarpment and differential erosion — 0.5.14
---------------------------------------------

For a clean comparison, create a NEW Arrakis Dev world. 0.5.14 adds an optional serialized
`erosion` object after the 0.5.13 `lithology` and `fractures` fields. A serialized 0.5.13
generator that omits erosion decodes with the pass disabled, preventing silent morphology
changes in its newly generated chunks. Already-generated chunks are never rewritten.

Native rock forms coherent stone/sandstone/tuff/limestone/calcite/andesite/diorite/basalt/
blackstone units with geological resistance roles. Create limestone is resolved by registry
and falls back to vanilla sandstone if Create is absent. Gravel is the principal loose talus
matrix rather than intact bedrock.

Continuous warped primary fissures cross the exposed massif; finite tapered branches can end
inside it. They form approximately 1-12 block wide, 5-68+ block deep cracks, slots and chasms.
Mineralization varies by fissure and appears as intermittent horizontal calcite wall bands.
Lithology contacts carry coherent multi-scale roughness instead of smooth geometric borders.
Overlapping traces modestly strengthen and deepen their intersection; fracture influence on a
cliff face fades below the fissure's design depth.

Eligible massif and large Broken Rock edges now use per-Y rock occupancy. Soft units recede,
hard/very-hard units survive as benches and ribs, and uncommon resistant caps can support
bounded rock-air-rock undercuts. The supplied maximum differential boundary offset around the
selected face is 6 blocks; steepening the former smooth macro apron is a separate operation.
Strong faults and sand passes are excluded, the bottom native-rock layers remain
crust-connected, and shallow one- or two-block outcrops are preserved. Wind-facing and
fracture-adjacent faces retreat more. Localized gravel/source-clast talus accumulates in coherent
patches on the low side of some scarps and fissure outlets. It starts above full dune blocks and
suppresses an overlapping fractional dune layer, preventing unsupported gravel.

Full caves, common water, general summit dents, physical collapse and rare sealed water caverns
remain later passes.

Base overworld stratigraphy
---------------------------
Y  65 and above : Native rock / dune terrain where fields require it
Y  55 to 64     : Sand (10-block base layer)
Y  45 to 54     : Sandstone (10 blocks)
Y   0 to 44     : Stone (45 blocks)
Y -63 to -1     : Deepslate (63 blocks)
Y -64           : Bedrock (1 block)

Biome: minecraftdune:arrakis_desert (new worlds)
Biome features: disabled
Lakes: disabled
Structures: disabled
Caves: disabled by the flat-generator base
Generator type: minecraftdune:arrakis_dev
Nether and End: normal vanilla generation

Natural fauna: Muad'dib and Desert Hare only. The Arrakis generator rejects other natural
spawn-placement attempts and autonomous patrol/event/structure/spawner paths, including vanilla
nighttime mobs in older Arrakis saves that retain the minecraft:desert biome. Commands, spawn
eggs, buckets, dispensers, and breeding remain available. Muad'dib faces its travel direction,
moves at 3x Rabbit's base speed, and uses a 1.5x launch velocity for approximately 2x the normal
jump apex.

Province sequence
-----------------
0-1500         CENTRAL_BASIN
               Exact flat pure sand. Arrakeen reservation.

1500-3050      INNER_ROCK_FORELAND
               Basin transition finishes at 2000; coherent 5-35 block formations and
               low micro-rocks grow toward the massif.

3000-4500      SHIELD_WALL_MASSIF
               Main high rock body; full by 3150 and fading outward from 4000.

1400-5850      FAULT_NETWORK
               Six strongly meandering regional traces; full from 2000 and fading after 4500.

4000-6650      BROKEN_ROCK_DESERT
               Full contribution near 5500; formations shrink and fade after 6000.

6000-9000      SAND_ROCK_TRANSITION
               Sparse low remnant rock plus increasing native dune activity.

8500-9000      OPEN_ERG_TRANSITION
               Open erg begins at 8500 and reaches full suitability at 9000.

9000+          OPEN_ERG
               Native transverse dunes at full outer-desert suitability.

All non-central ranges overlap and are distorted by low-frequency world-seed fields.

Shield Wall crossings
---------------------
Two separate crossing types are generated:

1. Fault ravines
   - narrow;
   - strongly warped non-radial structural traces;
   - some segments keep a low resistant rocky floor;
   - other segments cut fully to base sand, removing the 0.5.9 rock-fence artifact.

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
spacing              = 512
spacing variation    = 0.38
ridge sharpness      = 3.0
valley cutoff        = 0.20
slope asymmetry      = 0.82
wind angle           = 24 degrees
surface resolution   = sixteenth

Native dune material above the base surface uses minecraftdune:sand and a single optional
minecraftdune:sand_layer top block.

The erosion pass uses the fixed 24-degree development wind plus a coarse deterministic
face-orientation/relief/shelter factor. A full regional wind and sand-supply system remains later.

Serialized terrain profile
--------------------------
The authoritative tuning values for new Arrakis Dev worlds are now stored in the world
preset JSON under the generator's `terrain` object:

src/main/resources/data/minecraftdune/worldgen/world_preset/arrakis_dev.json

The chunk-generator codec serializes the same values into the world generator data.

Terrain inspection
------------------
/dune geology
/dune geology info
/dune geology sample <x> <z>
/dune geology profile

The bare /dune geology command reports the current terrain sample. It includes surviving rock
Y, exposed lithology/resistance, fissure intersection, escarpment strength/relief, maximum
differential boundary offset (shown as maximum retreat), coarse wind/fracture erosion, undercut
potential and talus suitability/depth. The X/Z line is a surface/face candidate summary; actual
occupancy can vary below it at each Y.
`profile` reports the serialized tuning loaded by the world's native Arrakis generator.

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

Deferred after 0.5.14
---------------------
- full cave and collapse-chamber generation;
- extremely rare sealed limestone-host water caverns;
- physical collapse and a complete mesa-to-butte lifecycle;
- final rock texture/art treatment;
- yardangs;
- thermal and salt weathering;
- full regional terrain-projected wind shelter/exposure and sand supply;
- regional wind direction changes;
- ecological/spawn use of the new provinces.

NeoForge remains 21.1.248.
