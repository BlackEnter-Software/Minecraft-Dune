# Native transverse dunes — 0.5.10

## Role

`NativeTransverseDuneField` remains the fast, infinite, chunk-order-independent dune
implementation used by normal Arrakis world generation.

The finite 64×64 `DuneSimulation` remains a separate development laboratory and is not
changed by this release.

## 0.5.10 spacing change

Planetary dune spacing is increased by 50%:

```text
0.5.9 native spacing = 350 blocks
0.5.10 native spacing = 525 blocks
```

The laboratory default remains:

```text
DuneSimulation spacing = 350 blocks
```

This distinction is intentional. The laboratory profile remains the calibrated reference,
while the planetary native field can use a larger geographic scale.

## Active native profile

The 0.5.10 world preset stores:

```text
maximum height       = 30 blocks
dune spacing         = 525 blocks
spacing variation    = 0.18
ridge sharpness      = 3.0
valley cutoff        = 0.20
slope asymmetry      = 0.82
wind angle           = 24 degrees
surface resolution   = 1/16 block
```

These values now come from `ArrakisTerrainSettings.NativeDuneSettings` rather than static
world-generation constants.

## Serialization

The source values are in the Arrakis Dev world preset:

```text
src/main/resources/data/minecraftdune/worldgen/world_preset/arrakis_dev.json
```

under:

```json
"terrain": {
  "native_dunes": {
    "max_height": 30.0,
    "spacing": 525.0,
    "spacing_variation": 0.18,
    "ridge_sharpness": 3.0,
    "valley_cutoff": 0.20,
    "slope_asymmetry": 0.82,
    "wind_angle_degrees": 24.0,
    "broken_rock_weight": 0.12,
    "transition_weight": 0.68
  }
}
```

The generator codec stores the profile with the world.

## Geographic activation

The Broken Rock Desert now extends farther than in 0.5.9, so weak dune activity can coexist
with rock over a longer distance.

The full sequence is approximately:

```text
Broken Rock Desert  ~2920–5650
Sand-Rock Transition ~4450–6500
Open Erg begins      ~5850
Full erg suitability ~6700
```

Rock height continues to suppress local dune suitability.

## Future work

The fixed 24-degree development wind remains temporary. Later dune work should consume:

- regional wind direction;
- wind exposure/shelter from rock topography;
- sand supply;
- sand depth;
- possible long-term transport/evolution fields.

The 0.5.10 release changes spacing only; it does not add those systems.
