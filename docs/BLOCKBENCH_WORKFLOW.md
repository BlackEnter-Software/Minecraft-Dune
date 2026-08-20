# Blockbench workflow

## Editable sources

Desert Hare:

```text
blockbench/desert_hare.bbmodel
```

Project format: **Modded Entity**, texture resolution: **64 × 32**. This is the renamed
original runtime model and contains the four established animations.

Muad'dib:

```text
blockbench/muaddib_mouse.bbmodel
blockbench/java/muaddib_mouse.java
blockbench/java/muaddib_mouse.png
```

The `.bbmodel` file is the editable source. The Java and PNG files are the current exported
geometry and texture used by the runtime adaptation. Its texture resolution is **32 × 32**.

## Desert Hare coordinate correction

Minecraft Java entity model coordinates traditionally place the model floor
near Java model Y=24 and use positive local Y downward. Blockbench displays
its editable workspace with positive Y upward.

The corrected project converts complete Java-space Y coordinates with:

```text
Blockbench Y = 24 - Java world Y
```

This correction was applied separately to cube bounds and pivots. Rotating
an already inverted root group is not equivalent because it can reverse
animation behavior and leave pivots inconveniently oriented.

## Stable Desert Hare hierarchy

```text
root
├─ body
│  ├─ head
│  │  ├─ left_ear
│  │  └─ right_ear
│  ├─ tail_base
│  │  └─ tail_middle
│  │     └─ tail_tip
│  └─ front_leg
│     ├─ left_front_leg
│     └─ right_front_leg
├─ left_hind_rump
│  └─ left_hind_stifle
│     └─ left_hind_feet
└─ right_hind_rump
   └─ right_hinde_stifle
      └─ right_hind_feet
```

Keep these part names stable unless the Java constructor and animation code
are updated at the same time.

## Exporting changes

Use:

```text
File -> Export -> Export Java Entity
```

Blockbench's generated Java should be treated as geometry output, not as a
full replacement for the renderer, registration, or entity behavior code.

When replacing Desert Hare geometry in `DesertHareModel.java`, preserve:

- `LAYER_LOCATION`
- the model part field names
- the constructor's `getChild` hierarchy
- `root()`
- `setupAnim(...)`, unless deliberately replacing the animations
- the 64 × 32 `LayerDefinition` texture dimensions.

When refreshing the Muad'dib export, adapt its generated geometry into
`MuaddibMouseModel.java` rather than copying the exported class over the runtime file. Preserve
the `minecraftdune` layer location, `HierarchicalModel` superclass, `root()`, renderer wiring,
and 32 × 32 texture dimensions. Blockbench exports geometry but does not generate NeoForge
registration or entity behavior.


## Desert Hare animation actions

The project now contains four useful Blockbench actions:

- `idle` — looping breathing, ear motion, and tail balance
- `hop` — looping Desert Hare locomotion preview
- `wiggle_head` — the supplied one-shot head/ear action
- `sniff_ground` — the supplied one-shot ground-sniffing action

Minecraft does not load `.bbmodel` animations at runtime. Equivalent keyframes
are compiled in `DesertHareAnimations.java`. After editing animation timing in
Blockbench, the Java definition must also be regenerated or updated.

The current Muad'dib Blockbench project contains no keyed animation. Its runtime model applies
a small synchronized hop, head tracking, and idle tail movement procedurally until authored
Muad'dib animation actions are exported.
