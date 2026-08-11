# Discussion 1886 scope audit

Source: <https://github.com/IntellectualSites/FastAsyncWorldEdit/discussions/1886>

The supplied `#discussioncomment-17966138` anchor is not present in GitHub's
public discussion data as of 2026-08-10: the discussion API reports three
comments and the comment endpoint returns 404. This project therefore treats
the discussion body as the source specification.

This is a third-party Bukkit/Paper plugin. Internal engine refactors, fixes to
FAWE internals, build tasks, and Fabric/Forge/Sponge/Nukkit providers are outside
the plugin boundary. They are not silently treated as plugin features.

## Implemented

- `//autorotatepaste` / `//arp`, including `-abenosr` and `-m`
- `//tpsel` / `//seltp`, including selection-stack indexes
- `//multireplace`
- `//clipboard size`, `stretch`, `compress`, `crop`, `list`, and `select`
- `//copynear`, including `-xbce`, `-m`, convex selection, and copy
- `//msel` stack operations and a paginated, clickable `//tpsel -s` list
- `//ssel` save/load/list/search/move/delete/clear/formats with optional stack state
- `//bmask`, `//help-masks`, `//help-patterns`, and block-glob `//echo`
- `//shortcut` commands, mask/pattern expansion, parameters, history, and export
- `//pin` / `//unpin` through FAWE's location-masked actor wrapper
- environmental masks: visible, sky, transparency, conductivity, light ranges,
  emitted light, and opacity
- compatibility aliases and arguments: upload, repeat, unextend, tracemask,
  seldraw, clockwise/counterclockwise rotate, and explicit sel/gmask clear
- fuzzy, paginated, clickable `//schematic search`

## Removed because current FAWE replaces the proposal

- CFI / image drawing: current FAWE ships the CFI command subsystem.
- `//unbind`: current FAWE has a native `unbind` command.
- Brush saving: current FAWE has brush save/load commands.
- Block/entity NBT patterns and modern block states: supported by current parsers.
- Parenthesized rich masks and combined masks: supported by current rich parsers.
- `#liquid`: current FAWE ships `LiquidMaskParser`.
- Core fixes, performance toggles, relighting, biome packet updates, translations,
  configuration fields, expression VM changes, and platform/build work: these
  require changes inside FAWE and cannot be correctly supplied by a plugin API.
- Expression buffers and the proposed block-ID-returning expression functions
  require new expression VM state and functions inside FAWE. Existing native
  `//generate`, `//generatebiome`, and `#buffer2d` cover adjacent use cases but
  are not mislabeled as exact implementations.

## Rejected or security-gated

- `//actor`: unrestricted fake actors are a privilege-escalation primitive. Any
  implementation must use explicit actor profiles and normal FAWE permissions.
- Reddit scraping/pastebin import: obsolete external APIs and unsafe arbitrary
  downloads. URL image input can be offered with allowlists and size limits.
- The proposed 3D fourth-dimensional polynomial smoother has no sufficiently
  precise algorithm in the discussion to implement reproducibly. A documented,
  tested replacement algorithm is required rather than claiming equivalence.
