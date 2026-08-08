# Minecraft: Dune patch notes

## 0.5.0 — Arrakis Dev dune prototype

- Added deterministic prototype dune generation for the Arrakis Dev world.
- Added operator commands:
  - `/minecraftdune dunes generate transverse`
  - `/minecraftdune dunes generate barchan`
  - `/minecraftdune dunes info`
  - `/minecraftdune dunes clear`
- Added a reduced 64 x 64 sand-thickness simulation rendered into a 128 x 128 block
  region.
- Added saltation-like directional transport, lee-side erosion dampening, and repeated
  slope stabilization.
- Added deterministic region seeds derived from the world seed, region coordinates,
  and dune mode.
- Added edge blending so generated test regions return to the original Y=64 surface.
- Added documentation of the simulation architecture, limitations, and test procedure.
- Updated the mod version from `0.1.0` to `0.5.0`.

## 0.4.0 — Arrakis Dev flat world

- Added the selectable `minecraftdune:arrakis_dev` world preset.
- Added a desert surface at Y=64 with ten sand layers, ten sandstone layers, stone,
  deepslate, and bedrock.
- Disabled overworld biome features, lakes, structures, and caves for deterministic
  testing.
- Retained normal vanilla Nether and End generation.

Public commit: `e24d3d5` — `Added Arrakis-dev flatbiome for testing`.

## 0.3.0 — Muad'dib redesign

- Replaced the original entity geometry with the revised tiny kangaroo-mouse design.
- Updated the runtime Java model, texture, and editable Blockbench source.
- Added the articulated body, ears, front legs, hind legs, and segmented tail used by
  the current animations.

## 0.2.1 — Muad'dib animation correction

- Corrected the animation/model integration produced during the initial animation patch.
- Aligned the revised hierarchy with the runtime model and movement-driven animation.
- Retained stationary random one-shot actions without adding network synchronization.

## 0.2.0 — Muad'dib animation update

- Added `MuaddibMouseAnimations.java`.
- Added coordinated hopping driven by actual entity movement.
- Added a subtle idle and balance loop.
- Added random stationary `wiggle_head` and `sniff_ground` actions.
- Added client-side animation state handling to the Muad'dib entity.

The public repository combines the 0.2.0, 0.2.1, and 0.3.0 development milestones in
commit `16b56a9` — `updated muaddib model`.

## 0.1.1 — NeoForge 1.21.1 compatibility fix

- Replaced the unavailable newer `DeferredRegister.Entities` API with the generic
  NeoForge 1.21.1 entity registry.
- Registered entity types through `Registries.ENTITY_TYPE` and `DeferredHolder`.
- Passed the registered resource identifier to `EntityType.Builder.build(String)`.
- Removed the deprecated explicit event-bus selector from `ClientModEvents`.
- Added the Gradle wrapper JAR.

Public commit: `0e36dd6` — `fixed`.

## 0.1.0 — Initial development build

- Created the standalone Minecraft 1.21.1 NeoForge project.
- Added the Muad'dib desert mouse entity as a rabbit-derived test mob.
- Added the spawn egg, client renderer, initial model and texture, project structure,
  build configuration, and Blockbench workflow.

Public commit: `5574dfa` — `initial`.

## Repository history note

The public repository currently contains four historical commits:

1. `5574dfa` — initial
2. `0e36dd6` — fixed
3. `16b56a9` — updated muaddib model
4. `e24d3d5` — Added Arrakis-dev flatbiome for testing

The semantic versions above describe the project milestones rather than claiming a
one-to-one release tag for every public commit.
