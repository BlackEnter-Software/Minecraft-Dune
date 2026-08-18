# Macro geology — 0.5.9 geological provinces

## Scope

Version 0.5.9 is the first morphology pass on top of the native Arrakis chunk generator.
The goal is not detailed sandstone erosion yet. It is to make the *sequence of landscapes*
read correctly at planetary travel scale while keeping generation continuous, deterministic,
and inexpensive.

All fields are derived from the world seed plus absolute X/Z coordinates. Chunk generation
order therefore has no effect on the result.

## Province sequence

The first Gameplay Arrakis region now uses overlapping continuous weights rather than one
massif mask fading directly into open desert:

| Approximate range | Province | Intended read |
|---:|---|---|
| `0–800` | Central Basin | Pure flat sand reserved for Arrakeen. |
| `800–~1120` | Inner Rock Foreland | Mostly sand with sparse knobs/shelves and small rock formations. |
| `~1000–3020` | Shield Wall / Main Massif | The majestic high rock body retained from 0.5.8. |
| `~2450–3660` | Faulted Margin | Overlaps the outer massif; long narrow structural ravines become important. |
| `~2920–4450` | Broken Rock Desert | Independent outliers/remnants after the main massif ends. |
| `~3900–5400` | Sand–Rock Transition | Lower, smaller remnant rock mixed with increasingly active sand. |
| `~4700+` | Open Erg | Transverse dune field takes over; full dune suitability is reached near 5250. |

These are not Minecraft biome borders. They are environmental/geological weights available
to later systems such as rock morphology, sand supply, ecology, settlements, and wind.

Boundary positions are distorted by a very-low-frequency seed-dependent field. The strict
exception is the first 800 blocks: that radius is protected before any warp is evaluated.

## Central basin and inner foreland

The pure-sand reservation is reduced from 1000 to **800 blocks**.

From roughly 800 to 1100 blocks a separate small-formation operator creates disconnected
rock rather than a miniature copy of the Shield Wall. The development targets are:

- roughly 10–20% rock occurrence depending on seed/direction;
- typical formation height about 5–28 blocks;
- scales of tens to low hundreds of blocks;
- continuous sand between formations.

This is intended to become a useful ecological/early-worm zone later, but 0.5.9 does not add
little-maker spawning or ecology rules.

## Shield Wall / main massif

The main massif keeps approximately the same vertical authority as 0.5.8. Its provisional
rock can still reach up to the existing 176-block development ceiling above Y=64.

The key topology change is that the massif is made *more continuous*. Broad missing sectors
are no longer responsible for most crossings. Crossings are created by explicit operators:

1. narrow structural fault ravines;
2. two broad seed-dependent sandy corridors.

This should make the massif read as one major geological body cut by real passages rather
than a loose collection of rounded mountains.

## Fault ravines

0.5.9 evaluates four long seeded fault traces. Each has:

- its own non-radial orientation;
- a seed-dependent offset so it normally does not pass through `(0,0)`;
- broad low-frequency lateral warping;
- a narrow carved center and wider transition walls.

Faults normally carve high rock down toward a low **rocky floor** instead of deleting the
rock completely. They are therefore intended to read as ravines / structural cuts / rocky
passes rather than sand gates.

This is still an abstract fault operator. Actual bedding offsets, talus, fracture zones,
slot-canyon erosion, and seismic scar morphology remain future work.

## Sandy Shield Wall corridors

Two major seed-dependent corridors are generated roughly opposite one another around the
central basin. Their centerlines curve slowly with radius and their width is measured in
world blocks, so they do not become enormous wedges as they move outward.

At the corridor center, provisional rock is suppressed completely. The corridors continue
through the broken-rock zone, providing actual sand-connected routes between the inner and
outer deserts.

Later wind/sand-supply systems can use the same corridor mask as a preferred aeolian
transport gateway.

## Abrupt outer breakup

The 0.5.8 massif faded gradually into the open desert. 0.5.9 separates two processes:

```text
main massif body
        ↓
comparatively abrupt outer termination
        ↓
independent detached rock outliers
        ↓
broken rock desert
        ↓
smaller transition remnants
        ↓
open erg
```

The main massif radial envelope drops over roughly a hundred effective-radius blocks around
its outer boundary. The next region is produced by a separate outlier noise field with
provisional relief around 12–67 blocks, rather than by simply reducing the height of the
main massif.

A second smaller-remnant field operates in the sand–rock transition with provisional relief
around 4–26 blocks.

## Native dune suitability

`MacroGeologyField.Sample` exposes `duneSuitability` in addition to the geological fields.
The initial rule is intentionally simple:

- broken-rock desert contributes weak dune activity;
- sand–rock transition contributes moderate/strong activity;
- open erg contributes full activity;
- existing rock height suppresses dune activity strongly.

This field is consumed by `NativeTransverseDuneField`; the iterative laboratory
`DuneSimulation` remains separate.

## Debug commands

The geology branch is registered by merging it into the canonical `/dune` root. In 0.5.9,
`/dune geology` itself is also executable and is equivalent to `info`.

```mcfunction
/dune geology
/dune geology info
/dune geology sample <x> <z>
```

The output now includes:

- dominant province;
- province weights;
- rock height and formation masks;
- fault mask;
- sand-pass mask;
- boundary warp;
- dune suitability and native transverse dune height.

Pregeneration remains available:

```mcfunction
/dune geology generate
/dune geology generate_initial
/dune geology generate_nearest <1..12>
/dune geology generation status
/dune geology generation cancel
```

The commands request normal FULL chunks only; terrain is created by the native generator.

## Deferred detailed rock morphology

0.5.9 deliberately does **not** yet implement:

- sandstone strata/bedding;
- resistant caprock;
- true plateau/mesa/butte morphology;
- talus and scree;
- thermal/salt weathering;
- yardangs;
- water/fluvial incision;
- terrain-projected regional wind.

The next rock pass should operate on top of these macro provinces rather than replacing
them.
