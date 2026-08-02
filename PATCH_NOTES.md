# 1.21.1 registry compatibility fix

Changed `ModEntityTypes` from the newer:

```java
DeferredRegister.Entities
DeferredRegister.createEntities(...)
registerEntityType(...)
```

to the NeoForge 1.21.1-compatible generic registry:

```java
DeferredRegister<EntityType<?>>
DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID)
ENTITY_TYPES.register(...)
```

The entity builder now receives the registered `ResourceLocation` and passes
`id.toString()` to `EntityType.Builder.build(String)`.

Also removed the deprecated `bus = EventBusSubscriber.Bus.MOD` annotation
parameter. NeoForge 21.1.181 and newer routes annotated event methods to the
correct bus based on the event type.
