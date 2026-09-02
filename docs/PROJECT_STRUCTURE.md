# Project structure

```text
src/main/java/com/blackenter/minecraftdune/
├─ MinecraftDune.java
├─ worldgen/
│  ├─ arrakis/
│  │  ├─ ArrakisChunkGenerator.java
│  │  ├─ ArrakisTerrainEvaluator.java
│  │  ├─ ArrakisTerrainSettings.java
│  │  ├─ ArrakisTerrainSettingsValidator.java
│  │  ├─ ArrakisTerrainCommand.java
│  │  └─ TerrainGenerationMetrics.java
│  ├─ geology/
│  │  ├─ MacroGeologyField.java
│  │  ├─ LithologyField.java
│  │  ├─ LithologyBlockPalette.java
│  │  ├─ MassifFractureField.java
│  │  ├─ EscarpmentErosionField.java
│  │  └─ MacroGeologyCommand.java
│  ├─ dune/
│  │  └─ NativeTransverseDuneField.java
│  └─ prototype/
│     └─ DuneSimulation.java
├─ entity/
│  ├─ DesertHareEntity.java
│  └─ MuaddibMouseEntity.java
├─ registry/
│  ├─ ModEntityTypes.java
│  └─ ModItems.java
└─ client/
   ├─ ClientModEvents.java
   ├─ model/
   │  ├─ DesertHareAnimations.java
   │  ├─ DesertHareModel.java
   │  └─ MuaddibMouseModel.java
   └─ renderer/
      ├─ DesertHareRenderer.java
      └─ MuaddibMouseRenderer.java
```

`client` must contain rendering-only code. Common entity and registry code
must not reference client classes, so the mod can later run on a dedicated
server.

Native terrain classes under `worldgen` must remain deterministic from the world seed,
serialized profile, and absolute coordinates. `DuneSimulation` is the frozen finite laboratory;
the native generator does not run it per chunk. `EscarpmentErosionField` is a removal-only
analytic occupancy field: it performs fixed coarse probes and per-Y tests inside native
rock-bearing columns, then `ArrakisChunkGenerator` writes the surviving rock and localized
talus directly to `ChunkAccess`. The final column composer keeps full dune blocks below talus
and omits only an overlapping fractional dune layer; shallow one- or two-block rock outcrops
remain protected by the occupancy field.

## Current hardening architecture

`ArrakisChunkGenerator` retains Minecraft hooks, seed initialization, palette lookup,
foundation detection and native column/chunk writes. `ArrakisTerrainEvaluator` owns
analytical columns, raw and orphan-filtered occupancy, height composition and the bounded
FastUtil cache. Each evaluator has one immutable seed/profile and one operation's lifetime;
no terrain cache crosses worlds or generation operations.

Current composition (actual-contact mode is explicitly opt-in):

```text
macro geometry + lithology -> fractures -> major/surface erosion -> orphan-filtered rock
final pre-talus rock -> bounded directional contact + connected relief probe -> basal apron
legacy mode: structural scarp contact + macro relief -> basal apron
rock + local scree + basal apron + dunes -> native writer / base-column / height query
```

The writer roots rock into the flat foundation below Y65. Public `rockOccupies` describes
only final pre-talus native rock above Y64; it is not a read of the generated world.
Local scree/dune substrate heights still use the pre-orphan column top as before.
The pre-talus stage never requests composed columns. Each cache entry holds its immutable
pre-talus column plus lazy final height, wall-band presence and composed result. The single
64/1024-entry limit covers both stages. The structural field provides only side/direction
and conservative eligibility bands in actual-contact mode; it does not place the apron.
See the [contact implementation report](BASAL_REMNANTS_ACTUAL_CONTACT_REPORT.md).

`ArrakisTerrainSettings` owns records, codecs and defaults; its validator owns unchanged
semantic constraints (including search-compatible spread/inset bounds for the new mode).
`ArrakisTerrainCommand` formats shared evaluator results and may
read an already-loaded chunk's hard substrate, without forcing generation.

Build-blocking validation lives at:

```text
src/test/java/com/blackenter/minecraftdune/worldgen/geology/
├─ ArrakisProfileValidation.java
├─ BasalRemnantValidation.java
├─ ActualTalusContactValidation.java
└─ EscarpmentErosionValidation.java
src/test/java/com/blackenter/minecraftdune/worldgen/arrakis/
├─ BasalContactPipelineValidation.java
└─ ArrakisTerrainEvaluatorValidation.java
src/test/java/com/blackenter/minecraftdune/worldgen/prototype/
└─ DunePrototypeStateValidation.java
```

`check` runs `validateArrakisTerrain`, `validateArrakisEvaluator` and
`validateDunePrototypeState`. Codec tests are called by `validateArrakisTerrain`.
Optional `diagnoseArrakisTerrain` runs `ArrakisVisualRegressionDiagnostics` for screenshot
searches; `diagnoseArrakisContact` runs `ArrakisContactDiagnostics` for analytical timings,
stage-separated transects and bounded contact measurements. Neither diagnostic is a
dependency of `check`. See the [hardening report](HARDENING_0.5.14.8_REPORT.md).
