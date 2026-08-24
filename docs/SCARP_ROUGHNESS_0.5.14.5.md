# Scarp Roughness & Wall Continuity - 0.5.14.5

0.5.14.5 is the final planned above-ground erosion-polish pass before subsurface geology.

0.5.14.4 established good erosion coverage on inner/outer Shield Wall faces and regional
fault walls. The remaining visual issue is that the structural precursor can still expose
long mathematically clean curtains when local erosion happens to be weak.

This release roughens the *position of the structural wall*, not every block's height.

## Massif scarp warping

The physical inner and outer Shield Wall boundaries receive two coherent lateral offsets:

```text
broad component: scale 150 blocks, strength 7 blocks
detail component: scale 42 blocks, strength 2.5 blocks
```

These offsets shift the radial start of the scarp. They do not add noisy vertical height to
the plateau.

### JSON

```json
"massif": {
  "...": "...",
  "scarp_warp_scale": 150.0,
  "scarp_warp_strength": 7.0,
  "scarp_detail_scale": 42.0,
  "scarp_detail_strength": 2.5
}
```

The two contributions add, so the supplied profile can shift a local scarp by roughly
+-9.5 blocks in the strongest theoretical case.

Old serialized profiles decode both strengths as zero and therefore retain the old clean
0.5.14.4 boundary.

## Regional fault wall variation

Regional faults already have broad/medium centerline meander. 0.5.14.5 adds coherent
variation to the cross-section along the fault:

```json
"faults": {
  "morphology": {
    "wall_width": 14.0,
    "toe_depth": 4.0,
    "wall_variation_scale": 90.0,
    "wall_variation": 3.0
  }
}
```

The same signal also allows a much smaller outward-only expansion of the protected core edge.
The configured `core_width` remains the guaranteed minimum. With variation 3, expansion is
capped to about 1.2 blocks.

This pass does not increase global erosion strength, add per-block height noise, alter fault
floor elevation, alter dunes, or address occasional floating resistant remnants.

If seed-0 looks good after this pass, above-ground erosion architecture should be frozen and
0.5.15 can begin subsurface geology.
