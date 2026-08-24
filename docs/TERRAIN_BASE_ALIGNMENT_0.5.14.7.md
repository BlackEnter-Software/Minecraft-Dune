# Terrain Base Alignment - 0.5.14.7

0.5.14.7 is a deliberately small alignment pass after the 0.5.14.6 erosion cleanup.

It changes two independent vertical references without moving Arrakis' global sand datum.

## Global sand datum stays fixed

`MacroGeologyField.BASE_SURFACE_Y` remains **64**.

The flat Arrakis base stratigraphy therefore remains:

```text
Y 55..64  sand
Y 45..54  sandstone
Y  0..44  stone
Y -63..-1 deepslate
Y -64      bedrock
```

The basin, foreland, broken-rock desert and existing dune reference level are not translated.

## Shield Wall / massif alignment

A new serialized top-level settings group is used:

```json
"base_alignment": {
  "massif_vertical_offset": -4.0
}
```

Negative values lower only the main Shield Wall/massif height field before faults and erosion
operate on it. The source profile starts at **-4 blocks**.

This is intentionally top-level rather than a seventeenth field inside `massif`: the existing
`MassifSettings` codec is already at DataFixerUpper's 16-field `RecordCodecBuilder.group`
limit.

The offset is applied before sand-corridor suppression, regional fault carving, fault
shoulder/toe shaping, fractures, face exposure, erosion, orphan-remnant suppression and talus.

Old serialized worlds omit `base_alignment`; it decodes to `0.0`, retaining their original
0.5.14.6 geometry in newly generated chunks.

## Regional fault floors

The active source profile changes `rocky_floor_height` from `4.0` to `0.0`.

For a full fault core the target height is now zero blocks above `BASE_SURFACE_Y`, so native
rock is not written above the Y=64 sand datum. The flat Arrakis sand layer remains visible as
the fault floor.

`faults.sandy_floor_threshold` is preserved for serialization/diagnostics and possible later
use. Fault wall width, toe depth, core width, centerline warping and wall variation are
unchanged.

## Future aeolian deposition

0.5.14.7 does **not** pile sand against cliffs.

Aeolian deposition is reserved for approximately **0.5.16**, after 0.5.15 subsurface geology.
That pass can use prevailing wind direction, cliff normals, obstacle height, wind shelter,
sand supply and dune regime to form windward aprons, lee drifts and fault-canyon deposits.
