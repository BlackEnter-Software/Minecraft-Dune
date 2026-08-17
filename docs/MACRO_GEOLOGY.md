# Macro geology

## 0.5.8 scope

Version 0.5.8 migrates the existing macro-geology field from a post-generation development
tool into **native Arrakis Dev chunk generation**.

The 0.5.7 `MacroGeologyField` itself remains the mathematical source of terrain elevation.
It is deterministic from the actual Minecraft world seed and absolute X/Z coordinates.

This release is deliberately an architecture/performance milestone. It does not yet perform
the next geological morphology pass.

## Native generator

Arrakis Dev now references:

```text
minecraftdune:arrakis_dev
```

instead of `minecraft:flat`.

`ArrakisChunkGenerator` subclasses vanilla `FlatLevelSource`, so the existing flat base
stratigraphy, fixed desert biome, disabled features/lakes and disabled structures are
retained.

During `fillFromNoise()`:

1. vanilla flat generation fills the configured Arrakis base layers;
2. the custom generator samples `MacroGeologyField` for each X/Z column;
3. provisional rock above Y=64 is written directly into `ChunkAccess`;
4. later normal chunk stages continue from that native terrain.

The old 0.5.7 workflow instead generated a flat chunk first and then used
`ServerLevel#setBlock` repeatedly. That path has been removed from geology generation.

## Seed handling

Chunk-generator JSON codecs do not contain the selected world's random seed.
Minecraft supplies the actual level seed to `ChunkGenerator#createState(...)`.

`ArrakisChunkGenerator` captures that seed there and then evaluates:

```text
MacroGeologyField.sample(worldSeed, absoluteX, absoluteZ)
```

for chunk terrain.

Chunk load order therefore does not affect the geology.

## Current first-region layout

This is intentionally still the 0.5.7 field:

- **0–1000 blocks:** hard-reserved Arrakeen / central basin, Y=64;
- **~1000–1500:** rock transition;
- **~1400–3000:** main Shield Wall / massif province;
- **~2800–4000:** eroded outer margin;
- **~3600–4200:** increasing open-desert weight;
- **Beyond ~4200:** open desert dominates.

Outside the protected 1000-block basin, the existing low-frequency boundary warp and
seed-dependent continuity lobes remain unchanged.

The provisional rock can add up to 176 blocks above Y=64, reaching Y=240.

## World compatibility

**Use a newly created Arrakis Dev world for 0.5.8 tests.**

Minecraft serializes the selected dimension generator into the save. A world created under
0.5.7 still contains the old flat generator even after the mod is updated.

Likewise, chunks that have already been generated keep their existing block data. Native
generation applies when a chunk is generated with the new Arrakis generator.

## Debug inspection

```mcfunction
/dune geology info
/dune geology sample 0 0
/dune geology sample 1200 0
/dune geology sample 2000 0
/dune geology sample 3200 0
/dune geology sample 3900 0
/dune geology sample 4500 0
```

These commands only evaluate the coordinate field.

## Native pregeneration commands

Pregenerate the aligned 256 x 256 geology tile containing the player:

```mcfunction
/dune geology generate
```

Pregenerate a 100 vanilla-Minecraft-chunk radius around absolute `(0,0)`:

```mcfunction
/dune geology generate_initial
```

One normal Minecraft chunk is 16 x 16 blocks, so the radius is 1600 blocks.

Pregenerate around the player's current 256 x 256 geology tile:

```mcfunction
/dune geology generate_nearest <1..12>
```

Examples:

- radius `1` -> 3 x 3 geology tiles -> 768 x 768 blocks -> 2304 Minecraft chunks;
- radius `2` -> 5 x 5 geology tiles -> 1280 x 1280 blocks;
- radius `3` -> 7 x 7 geology tiles -> 1792 x 1792 blocks.

The pregenerator requests Minecraft chunks at `ChunkStatus.FULL`. It does not run a separate
rock-materialization algorithm.

Large jobs remain spread over server ticks:

```mcfunction
/dune geology generation status
/dune geology generation cancel
```

The development limiter is currently 8 requested chunks per server tick with an approximate
30 ms per-tick job budget. A single expensive chunk can still exceed that time budget.

## Clearing

Native geology is no longer a removable debug layer. Therefore:

```mcfunction
/dune geology clear
```

does not delete blocks in 0.5.8. It explains that a clean terrain retest requires a new
Arrakis Dev world or closed-world region/chunk regeneration.

This avoids accidentally carving native terrain out of a legitimate save.

## Explicitly deferred morphology work

The following evaluation changes are intentionally **not** mixed into the native-generator
migration:

- changing the central pure-sand basin from 0–1000 to roughly 0–800;
- sparse little-maker rock / small formations around roughly 800–1000;
- narrower and more frequent rock passes;
- long fault/ravine corridors;
- one or two major sand passes through the Shield Wall;
- more abrupt massif termination;
- an additional broken-rock / mixed outer province before the open erg;
- stratigraphy, caprock, mesas, buttes, talus and erosion.

Those should be tuned after native generation and Distant Horizons behavior are verified.

## Downstream terrain architecture

The intended stack remains:

```text
regional geography
        ↓
native bedrock macro-topography
        ↓
rock morphology / faults / strata / erosion
        ↓
wind exposure + shelter
        ↓
sand availability / sand depth
        ↓
dune regime selection
        ↓
frozen/local dune synthesizers
```

The 0.5.6 transverse generator remains the current v1 local dune-synthesis baseline.
