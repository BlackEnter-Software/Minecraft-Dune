# Macro geology — 0.5.14 active framework

## Scope

The 0.5.10 province tuning remains the macro surface-height envelope. Version 0.5.13 added
coherent lithology and through-going local fractures; 0.5.14 now consumes those fields in a
removal-only 3D escarpment operator. The tuned province radii, regional fault geometry,
0.5.12 fault-floor correction, sand passes and native dunes remain intact.

See [LITHOLOGY_AND_FRACTURES.md](LITHOLOGY_AND_FRACTURES.md) for material, resistance, fissure,
optional Create limestone and future cavern groundwork. See
[ESCARPMENT_EROSION.md](ESCARPMENT_EROSION.md) for the active erosion and talus layer.

The main changes are:

- terrain parameters move into a serialized `ArrakisTerrainSettings` profile;
- more small rock in the Inner Rock Foreland;
- longer Broken Rock Desert with progressive size/density decay;
- stronger fault-line meander without widening the faults;
- intermittent fully sandy fault floors;
- native transverse dunes between and beyond rock;
- lithology-aware steep faces, resistant benches, bounded undercuts and localized talus on
  eligible massif / large Broken Rock edges.

The macro field still supplies one upper rock envelope per X/Z column. Three-dimensional
rock-air-rock geometry is owned by `EscarpmentErosionField` inside that envelope.

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
  "terrain": { "...": "serialized Arrakis profile" }
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

| Serialized radial range | Overlapping field | Current role |
|---:|---|---|
| 0–1500 | Central Basin | Exact flat sand reservation. |
| 1500–3050 | Inner Rock Foreland | Basin transition completes at 2000; formations grow toward the massif. |
| 3000–4500 | Shield Wall / Main Massif | Full by 3150; outer fade begins at 4000. |
| 1400–5850 | Regional fault network | Full from 2000; fades after 4500. |
| 4000–6650 | Broken Rock Desert | Full contribution near 5500; remnants shrink/fade after 6000. |
| 6000–9000 | Sand–Rock Transition | Sparse low rock mixed with increasing dune activity. |
| 8500–9000 | Open Erg transition | Open erg begins at 8500 and reaches full suitability at 9000. |
| 9000+ | Open Erg | Native transverse dunes at full outer-desert suitability. |

The boundaries remain continuous warped fields, not Minecraft biome boundaries.

## Inner Rock Foreland

0.5.9 had a single main small-formation field. 0.5.10 keeps that field and adds a second,
smaller noise scale.

The profile currently uses:

```text
large scale          = 200 blocks
detail scale         = 50 blocks
micro scale          = 40 blocks
large thresholds     = 0.08–0.40
micro thresholds     = 0.45–0.52
large height         = 5–35 blocks
micro maximum height = 4 blocks
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

The Broken Rock field begins at radius 4000, reaches full contribution near 5500, and fades
between 6000 and 6650. A separate low-remnant outer transition spans 6000–9000; the open-erg
gate starts at 8500 and is complete at 9000.

## Massif / mesa steepness

0.5.14 implements the first true escarpment layer on top of the preserved macro envelope. The
target is resistant ultra-hard remnant rock after long-term coronal-wind erosion. Eligible
massif and sufficiently large Broken Rock faces can therefore include:

- long near-vertical escarpments;
- locally unclimbable walls;
- caprock-supported shelves;
- undercut sections;
- locally negative-angle / overhanging faces where softer lower material has been removed;
- localized talus where collapse products plausibly accumulate.

These properties come from per-Y occupancy and differential material retreat rather than a
sharper height-field exponent. The operator is removal-only, caps differential/material offset
around its selected face, requires supported resistant caps for undercuts, retains the
foundation layers and shallow one- or two-block outcrops, and excludes strong
regional-fault/sand-pass carving. The cap does not measure the full smooth macro apron replaced
when a candidate becomes a steep face. Overlapping fissures modestly deepen/strengthen their
intersection, but fracture-driven face retreat fades below their design depth. Smaller Broken
Rock remnants keep simpler morphology.

Localized talus is low-side gated. It starts above surviving rock and any full native-dune
blocks; if it overlaps the optional fractional dune layer, that partial layer is omitted so the
gravel remains supported.

## Diagnostics

```mcfunction
/dune geology
/dune geology info
/dune geology sample <x> <z>
/dune geology profile
```

`sample` reports computed terrain state at a coordinate. It now includes surviving surface Y,
exposed lithology/resistance, fissure intersection strength, escarpment strength/local relief,
bounded differential offset, coarse wind/fracture erosion, undercut potential and talus
suitability/depth. `profile` reports the serialized settings currently loaded by the world's
generator.

Because the coordinate form accepts X/Z rather than Y, the erosion line describes the
surface/face candidate. Per-Y occupancy may still form an undercut below that reported surface.

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
