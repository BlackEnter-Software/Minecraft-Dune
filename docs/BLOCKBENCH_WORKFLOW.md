# Blockbench workflow

## Editable source

Open this file in Blockbench:

```text
blockbench/muaddib_mouse.bbmodel
```

Project format: **Modded Entity**

Texture resolution: **64 × 32**

## Why the previous file was upside down

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

## Hierarchy

```text
root
├─ body
│  ├─ head
│  │  ├─ left_ear
│  │  └─ right_ear
│  └─ tail_base
│     └─ tail_tip
├─ left_hind_leg
├─ right_hind_leg
├─ left_front_leg
└─ right_front_leg
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

When replacing geometry in `MuaddibMouseModel.java`, preserve:

- `LAYER_LOCATION`
- the model part field names
- the constructor's `getChild` hierarchy
- `root()`
- `setupAnim(...)`, unless deliberately replacing the animations
- the 64 × 32 `LayerDefinition` texture dimensions


## Animation actions in this revision

The project now contains four useful Blockbench actions:

- `idle` — looping breathing, ear motion, and tail balance
- `hop` — looping kangaroo-mouse locomotion preview
- `wiggle_head` — the supplied one-shot head/ear action
- `sniff_ground` — the supplied one-shot ground-sniffing action

Minecraft does not load `.bbmodel` animations at runtime. Equivalent keyframes
are compiled in `MuaddibMouseAnimations.java`. After editing animation timing in
Blockbench, the Java definition must also be regenerated or updated.
