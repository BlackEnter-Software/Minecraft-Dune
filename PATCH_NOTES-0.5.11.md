# Minecraft: Dune 0.5.11 — Rock gradients, rooted geology, and interior dunes

- Preserved the user's current pushed 0.5.10 Arrakis terrain profile as the tuning baseline:
  - pure-sand basin radius 1500;
  - foreland end 3050;
  - massif start 3000 / outer end 4500;
  - six faults;
  - broken-rock range 4000–6650;
  - outer transition to 9000;
  - native dune spacing 512 and spacing variation 0.38.
- Added radial **foreland growth** so the first boulders near the basin are smaller and the
  surviving fragments become progressively larger toward `massif.start_radius`.
- Added `foreland.inner_height_scale`.
  - Controls the inner-edge vertical scale of large foreland fragments.
- Added `foreland.inner_threshold_boost`.
  - Raises both large-rock thresholds at the inner edge, shrinking their footprint/density;
    the boost fades to zero toward the massif.
- Added `foreland.growth_power`.
  - Shapes how early/late the foreland reaches full size.
- Micro-rock remains small but gains some height toward the massif.
- Changed Broken Rock size progression so decay starts at `broken_rock.start_radius` rather
  than waiting until `broken_rock.full_radius`.
- Added `broken_rock.size_decay_power`.
  - Controls how quickly large near-massif remnants transition toward small outer remnants.
- Added `native_dunes.foreland_weight`.
  - Enables low dune activity in sandy foreland gaps.
- Raised the supplied profile's `broken_rock_weight` from 0.12 to 0.22 for visible but still
  subordinate dune activity among broken-rock outliers.
- Native dunes remain locally suppressed by rock height, so they preferentially occupy sand
  between formations rather than growing through major rock bodies.
- Rooted all visible native geological formations into the underlying hard crust:
  - the generator scans downward from Y=64 until it finds stone/deepslate/bedrock;
  - sandstone and sand between that crust and a visible rock formation are replaced by stone;
  - ordinary sand-only columns keep the original Arrakis flat stratigraphy.
- Updated terrain profile version to 511 and mod version to 0.5.11.
- Added backwards-compatible codec defaults for all new JSON fields so 0.5.10 generator data
  lacking the new fields remains decodable.
- Updated the terrain profile documentation with detailed explanations of the new fields and
  the rock-foundation behavior.
