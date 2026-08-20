# Macro geology — 0.5.13 active framework

## Scope

The 0.5.10 province tuning remains the macro surface-height baseline. Version 0.5.13 adds
coherent lithology and through-going local primary fractures with finite branches, without
changing the tuned province radii, fault geometry, fault-floor correction, sand passes,
broken-rock values, or native dunes.

See [LITHOLOGY_AND_FRACTURES.md](LITHOLOGY_AND_FRACTURES.md) for the active 0.5.13 material,
resistance, fissure, optional Create limestone, talus and future cavern framework.

The main changes are:

- terrain parameters move into a serialized `ArrakisTerrainSettings` profile;
- more small rock in the Inner Rock Foreland;
- longer Broken Rock Desert with progressive size/density decay;
- stronger fault-line meander without widening the faults;
- intermittent fully sandy fault floors;
- native transverse dune spacing increases from 350 to 525 blocks.

Detailed sandstone erosion and true escarpment geometry remain separate future work.

## Serialized settings

The new-world source profile lives in:

```text
src/main/resources/data/minecraftdune/worldgen/world_preset/arrakis_dev.json
```

under:

```json
"generator": {
  "type": "minecraftdune:arrakis_dev",
  "settings": { "...": "flat Arrakis base" },
  "terrain": { "...": "0.5.10 profile" }
}
```

`ArrakisChunkGenerator.CODEC` serializes the `terrain` object as part of the dimension
generator. This is preferable to a process-global config because the world retains the
terrain parameters it was created with.

The `terrain` field is optional when decoding older worlds; missing data falls back to the
current default profile. Existing generated chunks are never rewritten, so create a new
Arrakis Dev world for clean cross-version comparisons.

## Province sequence

The current profile is approximately:

| Range | Province | 0.5.10 intent |
|---:|---|---|
| 0–800 | Central Basin | Exact flat sand reservation. |
| 800–~1150 | Inner Rock Foreland | Medium knobs plus denser 2–9 block micro-rock. |
| ~1000–3020 | Shield Wall / Main Massif | Keep the successful large 0.5.9 scale. |
| ~2450–3660 | Faulted Margin | Same fault width; substantially stronger lateral meander. |
| ~2920–5650 | Broken Rock Desert | Large remnants near the massif, smaller/noisier remnants outward. |
| ~4450–6500 | Sand–Rock Transition | Sparse low rock mixed with increasing dune activity. |
| ~5850+ | Open Erg | Native dunes increasingly dominant; full suitability near 6700. |

The boundaries remain continuous warped fields, not Minecraft biome boundaries.

## Inner Rock Foreland

0.5.9 had a single main small-formation field. 0.5.10 keeps that field and adds a second,
smaller noise scale.

The profile currently uses:

```text
large scale          = 145 blocks
detail scale         = 62 blocks
micro scale          = 30 blocks
large height         = 4–28 blocks
micro height         = approximately 1.5–9 blocks
```

The micro threshold is deliberately higher than the first experimental 0.5.10 value so the
foreland remains predominantly sand rather than becoming a continuous boulder field.

## Fault network

The useful 0.5.9 fault width is retained:

```text
core width           = 30 blocks
outer carve width    = 105 blocks
```

The centerline itself now has three deformation scales:

```text
broad warp           = 240 blocks at ~1150-block scale
medium warp          = 90 blocks at ~360-block scale
sinusoidal component = 75 blocks at ~780-block scale
```

The broad and medium components are evaluated primarily as functions of distance **along**
each fault. This bends the trace instead of only roughening the walls of a nearly straight
line.

### Fault floors

Fault carving is applied after all rock contributors, including Broken Rock outliers. That
prevents a late outlier from creating a narrow transverse "rock fence" across a carved basin.

Each fault also receives a low-frequency along-fault floor field:

- some segments retain a very low resistant rocky floor;
- some center segments suppress the rock completely and expose the Y=64 sand surface.

This should create a mix of rocky structural ravines and sand-floored caldera/fault basins.

## Broken Rock Desert

The Broken Rock Desert now persists much farther outward.

Instead of one outlier field fading to zero, 0.5.10 blends:

1. a large-remnant field;
2. a second micro-remnant field.

A radial `brokenProgress` value increases outward. It simultaneously:

- raises the threshold for large formations;
- reduces their maximum height;
- gives more relative influence to the micro-remnant field;
- raises the micro threshold farther outward.

The intended sequence is:

```text
large detached remnants
        ↓
medium outliers
        ↓
small low formations
        ↓
rare micro-remnants
        ↓
sand-rock transition
```

The current rock envelope fades between roughly effective radius 5200 and 5650. A separate
low-remnant transition continues toward roughly 6500.

## Massif / mesa steepness

0.5.10 deliberately does **not** attempt the true mesa/escarpment geometry yet.

The target for that future pass is resistant ultra-hard remnant rock after long-term coronal
wind erosion. The intended morphology can therefore include:

- long near-vertical escarpments;
- locally unclimbable walls;
- caprock-supported shelves;
- undercut sections;
- locally negative-angle / overhanging faces where softer lower material has been removed;
- talus only where collapse products have accumulated.

Those properties require a 3D rock/erosion operator rather than simply making the current
height-field smoothstep sharper.

## Diagnostics

```mcfunction
/dune geology
/dune geology info
/dune geology sample <x> <z>
/dune geology profile
```

`sample` reports computed terrain state at a coordinate. `profile` reports the main
serialized settings currently loaded by the world's generator.

Sample diagnostics now include `fault_sand` in addition to the existing fault carve mask.

## Pregeneration

Native chunk pregeneration is unchanged:

```mcfunction
/dune geology generate
/dune geology generate_initial
/dune geology generate_nearest <1..12>
/dune geology generation status
/dune geology generation cancel
```

Terrain itself remains part of normal native chunk generation.
