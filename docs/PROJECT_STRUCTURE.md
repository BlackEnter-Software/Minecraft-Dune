# Project structure

```text
src/main/java/com/blackenter/minecraftdune/
├─ MinecraftDune.java
├─ worldgen/
│  ├─ arrakis/
│  │  ├─ ArrakisChunkGenerator.java
│  │  └─ ArrakisTerrainSettings.java
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

The dependency-free smoke validation for this layer lives at:

```text
src/test/java/com/blackenter/minecraftdune/worldgen/geology/
└─ EscarpmentErosionValidation.java
```
