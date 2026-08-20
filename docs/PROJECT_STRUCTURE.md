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
│  │  └─ MassifFractureField.java
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
the native generator does not run it per chunk.
