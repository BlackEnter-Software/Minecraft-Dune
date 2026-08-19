# Minecraft: Dune 0.5.10 — Terrain profile + morphology tuning

- Added `ArrakisTerrainSettings` and serialized it in the `minecraftdune:arrakis_dev`
  chunk-generator codec.
- Added an explicit `terrain` object to the Arrakis Dev world preset JSON.
- Added `/dune geology profile` to report the main terrain parameters loaded by the world.
- Kept `/dune geology sample <x> <z>` as a computed diagnostic rather than configuration.
- Extended the Inner Rock Foreland to roughly 1150 blocks and added a second micro-rock
  field for more numerous 2–9 block formations between the existing larger knobs.
- Retained the successful main Shield Wall height scale.
- Kept the useful 0.5.9 fault width, but substantially increased lateral centerline meander
  with broad, medium and sinusoidal along-fault warp components.
- Made fault carving a final rock operation so Broken Rock outliers cannot form narrow
  transverse "fences" across already-carved basins.
- Added intermittent fault segments that cut fully to the Y=64 sand surface; other segments
  retain a very low resistant rocky floor.
- Extended the Broken Rock Desert from the previous ~4450 outer scale to roughly 5650.
- Split Broken Rock morphology into large and micro-remnant fields. Large formations become
  progressively rarer/lower outward while micro-remnants gain relative importance before
  also fading.
- Extended the low-remnant Sand–Rock Transition to roughly 6500 and moved full Open Erg
  suitability outward to roughly 6700.
- Increased **native world-generation transverse dune spacing** from 350 to 525 blocks.
- Preserved the finite `DuneSimulation` laboratory baseline at 350-block spacing.
- Preserved native sixteenth-layer dune surfaces and the existing 24-degree development wind.
- Deliberately deferred true escarpment/mesa geometry. The later erosion pass is expected to
  support ultra-hard remnant rock, near-vertical walls, undercut shelves and locally
  negative-angle/overhanging faces where softer material has been removed.
- NeoForge remains 21.1.248.
