# Project structure

```text
src/main/java/com/blackenter/minecraftdune/
├─ MinecraftDune.java
├─ entity/
│  └─ MuaddibMouseEntity.java
├─ registry/
│  ├─ ModEntityTypes.java
│  └─ ModItems.java
└─ client/
   ├─ ClientModEvents.java
   ├─ model/
   │  └─ MuaddibMouseModel.java
   └─ renderer/
      └─ MuaddibMouseRenderer.java
```

`client` must contain rendering-only code. Common entity and registry code
must not reference client classes, so the mod can later run on a dedicated
server.
