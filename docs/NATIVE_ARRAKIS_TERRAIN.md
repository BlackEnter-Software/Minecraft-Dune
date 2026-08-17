# Native Arrakis terrain generation — 0.5.8

## Objective

0.5.8 removes the temporary architecture where macro cliffs were added to already-generated
flat chunks with `ServerLevel#setBlock`.

Arrakis Dev now has a registered custom chunk-generator codec and creates the current macro
geology during Minecraft's normal chunk-generation pipeline.

## Files

- `registry/ModChunkGenerators.java`
  - registers `minecraftdune:arrakis_dev` in the vanilla chunk-generator codec registry.
- `worldgen/arrakis/ArrakisChunkGenerator.java`
  - extends `FlatLevelSource`;
  - retains the existing flat settings codec;
  - captures the real level seed from `createState`;
  - overlays the existing `MacroGeologyField` directly during `fillFromNoise`;
  - makes generator base-height/base-column queries aware of the native rock.
- `worldgen/world_preset/arrakis_dev.json`
  - selects `minecraftdune:arrakis_dev`.
- `MacroGeologyGenerationManager`
  - now pregenerates FULL chunks only.
- `MacroGeologyCommand`
  - no longer performs block-by-block terrain materialization.

## Why extend FlatLevelSource

Arrakis Dev already has a useful deterministic base column. Extending the vanilla flat
generator lets 0.5.8 preserve it instead of duplicating the biome, structure override,
feature/lake and layer codecs.

The custom codec wraps `FlatLevelGeneratorSettings.CODEC` under the same `settings` object
used by the previous world preset.

## Important test procedure

1. Build 0.5.8.
2. Start the development client.
3. Create a **new** Arrakis Dev world.
4. Confirm `/dune geology info` at `(0,0)` reports the central basin and Y=64.
5. Run:

```mcfunction
/dune geology generate_initial
```

6. Watch progress with:

```mcfunction
/dune geology generation status
```

7. With Distant Horizons installed manually, inspect the first macro formations.
8. Fly to an interesting boundary and run:

```mcfunction
/dune geology generate_nearest 1
```

Radius 1 should generate the current 256 x 256 geology tile and all eight neighboring tiles.

## Validation targets

For this release, only validate:

- seamless chunk boundaries;
- identical or near-identical 0.5.7 macro shape for the same world seed;
- central basin stays exactly flat;
- native cliffs appear when chunks are first generated;
- Distant Horizons sees saved/generated macro terrain;
- pregeneration is materially faster than 0.5.7 post-placement;
- no changes to the frozen transverse dune defaults.

Do not judge the detailed rock morphology yet.
