# Changelog

## 1.0.1 — 2026-10-28

- Performance: hut icons and labels are drawn in two batches per map per frame instead of two flushes per hut
  (spark: ~3% of the client render thread before, with icons on). No visual or config changes; where huts
  overlap, labels now always sit above neighbouring badges.

## 1.0.0 — 2026-10-21

First release of MapColonies.

- Hut icons (the hut's own block item, or a lettered square) at each footprint centre on both maps, with labels
  under them; the Town Hall shows the colony name. New client options: `buildingIcons`, `iconsOnMinimap`,
  `iconsOnWorldMap`, `labelModeMinimap`, `labelModeWorldMap` (`ALWAYS`/`HOVER`/`NONE`), `worldMapLabelMinZoom`,
  `iconScale`, `iconBackground` (black badge with white outline behind hut icons), `labelScale`.
- Hostile and Neutral colony ranks no longer count as members.
- The sync channel is optional, so mismatched installs can still join.
- MineColonies 1.1.1403 or newer is required.

- Colony claims drawn on Xaero's Minimap and World Map in each colony's team colour, with borders between colonies.
- Server-authoritative sync: full sync on login, then a diff every `syncIntervalSeconds`.
- Colonies you are a member of (any MineColonies member rank) are drawn stronger than other colonies.
- Hut footprints (block-accurate rectangles from each building's corners) on both maps, with hover labels
  (`name` or `name + level`), per-type or colony colouring, a minimum level and a hidden-types list.
- Server config: `showAllColonies`, `showOtherColoniesBuildings`, `syncIntervalSeconds`.
- Client config: per-map toggles, opacities, outline-only mode and the building options.
