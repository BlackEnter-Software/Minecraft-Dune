# Minecraft: Dune 0.5.8 — Native Arrakis terrain generation

- Registered the `minecraftdune:arrakis_dev` chunk-generator codec.
- Added `ArrakisChunkGenerator`, extending vanilla `FlatLevelSource`.
- Preserved the 0.5.7 `MacroGeologyField` mathematics and provisional plain-stone output.
- Captured the real world seed in `ChunkGenerator#createState` and continued using absolute
  X/Z coordinates for deterministic macro terrain.
- Moved macro-rock creation into `fillFromNoise`, writing directly to `ChunkAccess`.
- Updated generator base-height and base-column queries to include native macro relief.
- Changed the Arrakis Dev world preset from `minecraft:flat` to
  `minecraftdune:arrakis_dev` while retaining the existing flat settings object.
- Converted `/dune geology generate` into native pregeneration of the player's current
  aligned 256 x 256 geology tile.
- Kept `/dune geology generate_initial` as a 100 vanilla-Minecraft-chunk / 1600-block
  pregeneration radius around absolute `(0,0)`.
- Kept `/dune geology generate_nearest <radius>` player-centered; radius 1 is 3 x 3 geology
  tiles.
- Large jobs now request ordinary `ChunkStatus.FULL` chunks and no longer run a second
  terrain-materialization pass.
- Retained `/dune geology generation status` and `cancel`.
- Changed `/dune geology clear` into a compatibility explanation because native terrain is
  no longer a removable post-generation layer.
- Existing 0.5.7 worlds do not migrate automatically; create a new Arrakis Dev world for
  native-generator testing.
- Preserved NeoForge 21.1.248, the existing runClient JVM arguments, empty third-party
  Gradle runtime dependency list, layered sand assets, debug cameras/screenshots, barchan
  prototype, and the frozen transverse 0.5.6 v1 defaults.
- Deferred the evaluated morphology changes (0–800 basin, 800–1000 sparse rock, faults,
  sandy passes, abrupt breakups and the additional outer mixed province) to the next pass.
