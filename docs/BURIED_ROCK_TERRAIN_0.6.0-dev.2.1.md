# 0.6.0-dev.2.1 — World validation and terrain compatibility

Scope: review High priorities 1–3. Terrain geometry and numeric tuning remain those of
commit `a164c8a`, the corrected dev.2 baseline. No Medium/Low roadmap features are included.

## Reliable dev saves

`run-arrakis-dev.bat` delegates to `scripts/Start-ArrakisDev.ps1`. A version such as
`0.6.0-dev.2.1` produces `Arrakis-dev_0_6_0-dev_2_1_DDMMYY`, matching Minecraft's save-folder
normalization. Normal launch reopens that exact folder. `--fresh` removes only that day's
exact versioned dev folder; other dates, versions and suffixed duplicates remain untouched.
The Java launcher refuses an unexpected sanitized/collision-suffixed target before creation.

The PowerShell helper validates the resolved target and refuses linked save paths before
recursive removal. The launcher regression test covers create, reopen, fresh replacement,
unrelated collision preservation, incomplete creation, and unsafe version input.

## Saved terrain compatibility

`terrain.terrain_algorithm_revision` is separate from `profile_version` and `mod_version`.

| Saved settings | Behavior |
| --- | --- |
| Legacy profiles <=5148, missing/zero revision | Existing legacy behavior retained |
| Profile 6000, missing/zero revision, morphology and coherent talus disabled | Unambiguous dev.1; resolves and saves as revision 1 |
| Profile 6000, missing/zero revision, either dev.2 switch enabled | Rejected: initial and corrected dev.2 cannot be distinguished |
| Revision 1 | Preserved dev.1 path; dev.2 opt-ins rejected |
| Revision 2 | Corrected dev.2 evaluator, used by new dev.2.1 worlds |
| Unknown revision or legacy profile with a nonzero revision | Rejected with an explicit compatibility error |

Keep ambiguous dev.2 worlds with their original mod build or create a fresh world. Do not
edit their revision marker to bypass the check: that cannot repair existing chunk seams.
This release intentionally does not guess or migrate ambiguous worlds.

Revisions 1 and 2 currently share code with explicit restrictions selecting the preserved
dev.1 path. Frozen fixtures/fingerprints cover all cells in representative columns, raw and
eroded roofs, and sediment heights over two seeds. The dev.2 fixture comes from `a164c8a`
with only the new revision marker added. It is not the current preset loaded under another
name: future preset tuning cannot silently update the compatibility reference.
A second revision-2 fixture preserves the settings actually serialized during the runtime
test, including omitted default-valued fields. Its fingerprint also detects changes to
codec defaults that would reinterpret previously saved settings.

Future developers must assign a new revision for changes to generated blocks or heights,
including geology, erosion, sediment, lithology and deposition. Retain the previous behavior
behind its old revision or explicitly reject it. Do not advance a revision constant while
continuing to accept old identifiers through changed behavior. Fingerprints detect sampled
regressions; they are not a proof covering every coordinate or optional mod palette.

## Reproducible runtime acceptance

Run from the project root on Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Test-ArrakisRuntime.ps1
```

This launches Minecraft twice using the isolated `build/terrain-validation-client` directory.
Each invocation uses a unique seed-0 save and report directory. It never deletes a player save
or changes the normal `run` directory's options, camera presets, or mod configuration.
The first launch creates the Arrakis preset; the second uses Minecraft's normal world-open
flow to reopen its exact folder. This avoids first-run onboarding blocking Quick Play in a
new test installation. Only the experimental-settings notice for this disposable validation
save is automatically continued; other world-load warnings are not bypassed. Each launch
closes after success or a reported validation failure.

The server-thread check verifies the save path, seed, generator type, profile and algorithm,
then compares every block in nine complete chunks against `getBaseColumn`, and every column's
WORLD_SURFACE, OCEAN_FLOOR and MOTION_BLOCKING heightmaps against `getBaseHeight`:

- 2,304 columns, 884,736 blocks and 6,912 heightmap values per launch.
- Basin, inner cliff/gully/shoulder, plateau, outer cliff, negative coordinates and far erg.
- Reload requires each target chunk to exist in region storage before requesting it, so a
  missing chunk cannot be silently regenerated and counted as a persistence success.
- Full chunk hashes and decoded terrain settings must match between launches.

The first launch also captures `inner-wall.png`, `cliff-foot.png`, `shoulder.png`, and
`outer-wall.png`. The runner waits for the surrounding 15-by-15 chunk area and render work to
settle, uses noon lighting, disables clouds, and records camera/settings/mod metadata.
Screenshots are real Minecraft frames. They do not rely on Distant Horizons, shaders, or an
analytical surface renderer. A capture pass records technical readiness, not artistic approval.

Reports appear under `build/terrain-validation-client/validation/<unique-world-folder>/`:
`generated.json`, `reloaded.json`, capture manifests, PNGs and phase-specific pass/failure files.
Inspect the reports and screenshots as well as the process result. The Gradle client task alone
can exit successfully after a reported validation failure; the PowerShell runner checks markers.

The exhaustive runtime comparison deliberately blocks the isolated server thread and may
produce a "Can't keep up" log. Its elapsed time is not a normal generation benchmark.
Runtime tests are separate from `test`/`check` because they require a graphical Minecraft client.
The ordinary `test` task runs the launcher, compatibility and analytical regressions.

## Validation results

Runtime validation passed on 7 September 2026 using Java 21.0.11, Minecraft 1.21.1,
NeoForge 21.1.248 and Minecraft: Dune 0.6.0-dev.2.1, with no extra mods or shaders.
Run folder: `Arrakis-validation_20260907_193946_2fc67882`.

Both phases checked 884,736 blocks and 6,912 heightmap values with zero mismatches.
The generated and reloaded reports were byte-for-byte identical (SHA-256
`dbcd4ff4e8b51696716c82acf5b549144b555ded2968b9ab50ba72c69c93752a`).
All nine reload targets were present in region storage. A closed-channel diagnostic appeared
during automatic client shutdown, after the checks passed; the server then completed its saves.

All four in-game screenshots were inspected. Local recessed channels, rock-face texture,
stepped shoulders, and cliff-foot deposits are visible. Broad slopes still contain smooth
areas and regular Minecraft stair stepping. The 12-chunk view distance clips distant terrain,
so these are local acceptance views, not a whole-wall panorama or a Distant Horizons test.
They establish a repeatable visual baseline; matching the older artistic target remains a
separate tuning decision. No erosion strength or geometry was changed for these captures.

Both normal dev reopening and the test share Minecraft's explicit world-open flow because
Quick Play can remain behind first-run onboarding in a new client installation.

`gradlew.bat build` passed in 7m 26s, including the buried-rock, legacy terrain, legacy
evaluator, dune prototype, and Windows launcher checks. A separate final `gradlew.bat test`
passed after adding the serialized-settings fixture; its fingerprint matched the corrected
dev.2 baseline. `git diff --check` passed. The artifact is
`build/libs/minecraftdune-0.6.0-dev.2.1.jar`.

No commits, staging, or pushes were performed. The baseline HEAD remains `a164c8a`.
