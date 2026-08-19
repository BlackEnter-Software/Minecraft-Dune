# Arrakis terrain profile — 0.5.10

## Why the profile exists

Native terrain had accumulated enough tunable constants that continuing to encode them only
inside Java would make testing and world reproducibility difficult.

0.5.10 introduces `ArrakisTerrainSettings`, serialized by the custom Arrakis chunk
generator.

## Authoritative source for new worlds

```text
src/main/resources/data/minecraftdune/worldgen/world_preset/arrakis_dev.json
```

The `terrain` object is consumed by:

- `MacroGeologyField`;
- `NativeTransverseDuneField`;
- `ArrakisChunkGenerator`;
- `/dune geology profile`;
- `/dune geology sample`.

## Groups

The profile is divided into:

```text
basin
foreland
massif
faults
sand_passes
broken_rock
outer_transition
native_dunes
```

This organization is deliberate. Later erosion, stratigraphy and wind settings can be
added as additional groups without turning one record into an unmanageable flat parameter
list.

## World persistence

The custom generator codec now contains both:

```text
settings = FlatLevelGeneratorSettings
terrain  = ArrakisTerrainSettings
```

When a world is created, those generator parameters are serialized with the dimension
generator.

This means future local edits to the default world preset do not silently rewrite terrain
that is already saved in a world.

Already generated chunks always retain their blocks. For visual development comparisons,
continue creating a new Arrakis Dev world after changing terrain-generation parameters.

## Diagnostic command

```mcfunction
/dune geology profile
```

reports the main loaded values. It is intended as a quick verification that a world is
actually using the profile you expect.

The coordinate-specific command remains:

```mcfunction
/dune geology sample <x> <z>
```

Computed sample output should not itself be treated as configuration. It is derived from the
serialized profile + world seed + absolute coordinate.
