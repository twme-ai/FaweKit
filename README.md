# FaweKit

[![Build](https://github.com/twme-ai/FaweKit/actions/workflows/build.yml/badge.svg)](https://github.com/twme-ai/FaweKit/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Command reference: [English](docs/commands.md) | [繁體中文](docs/commands.zh-TW.md)

An open-source Bukkit/Paper extension for FastAsyncWorldEdit that implements the
still-useful user-facing proposals from FAWE discussion 1886 without forking or
patching FAWE.

FaweKit is an independent community project. It is not affiliated with or
endorsed by the FastAsyncWorldEdit project.

## Requirements

- Java 21
- Paper or Spigot 1.21.x
- FastAsyncWorldEdit 2.13 or newer

Build with `./gradlew build`, then place `build/libs/FaweKit-0.1.0.jar` beside
FAWE in the server `plugins` directory.
The protocol-level test procedure is documented in
[integration/README.md](integration/README.md).

## Documentation

- [Complete English command reference](docs/commands.md): syntax, parameters,
  flags, permissions, examples, persistence, and edge cases for every FaweKit
  command.
- [繁體中文完整指令手冊](docs/commands.zh-TW.md): syntax, parameters, flags,
  permissions, examples, persistence, and edge cases for every FaweKit command.
- [Feature scope audit](docs/coverage.md): implemented, superseded, and rejected
  items from the source proposal.

## Implemented commands

- `//tpsel [<x> <y> <z>]`: finds a lit, safe point around/in the selection, or
  resolves coordinates relative to its center.
- `//multireplace <mask> <pattern> ...`: applies all pairs against the original
  region state in one edit session; the last matching pair wins.
- `//clipboard size`
- `//clipboard stretch|compress <width|%> <height|%> <length|%>`; sizes also
  accept relative forms such as `~5` and `~25%`.
- `//clipboard crop [-w n] [-h n] [-l n] [-x n] [-y n] [-z n]`
- `//clipboard list [-p page]` and `//clipboard select [-n] <index>` for FAWE
  multi-clipboards.
- `//copynear [-xbce] [-m <copyMask>] <searchMask> [distance=64]`: finds nearby
  matching blocks, selects their convex hull (or a cuboid below four matches),
  and copies it. `-x` excludes the matched blocks themselves.
- `//autorotatepaste` (`//arp`): after `//copy`, rotates the clipboard in
  90-degree increments from the copied selection direction to the new selection
  direction and aligns its origin to the new selection's primary point. It
  supports `-abenosr` and `-m`; `-r` uses the normal FAWE placement position.
- `//msel push|pop|combine|delete|clear|list|undo|redo`: maintains a selection
  stack. Its list is paginated and entries teleport when clicked. Combined
  selections remain usable by native FAWE edit commands.
- `//tpsel -s <index>` targets an item in the selection stack; negative indexes
  count backward from its end.
- `//ssel save [-m] <name>`, `load`, `list`, `search`, `move`, `delete`, `clear`,
  and `formats`: safely persists cuboid, polygon, convex, cylinder, ellipsoid, and
  combined selections. `-m` includes the current selection stack.
- `//bmask <biome,...>` sets a biome-aware FAWE global mask; `clear` removes it.
- `#visible`, `#sky`, `#transparent`, `#conductive`, `#skylight[min][max]`,
  `#blocklight[min][max]`, `#light[min][max]`, `#emitslight[min][max]`,
  `#opacity[min][max]`, `#haslight`, and `#nolight` are registered in FAWE's
  mask factory and work in native commands.
- `//help-masks`, `//help-patterns`, and `//echo <command...>` provide compact
  parser references and expand block-name globs such as `mud_brick_*`.
- `//shortcut` / `//sc` supports create, execute, rename, delete, list/search,
  bounded history, and YAML export. `${n}`, `${n:-default}`, `${n:+alternate}`,
  `${n:?error}`, and `${@}` parameters are supported. Names beginning with `#`
  expand inside any FAWE mask or pattern command.
- `//pin` and `//unpin` freeze the location seen by native FAWE commands without
  teleporting the player; permissions and the player's normal FAWE session remain intact.
- Compatibility syntax includes `//repeat`, `//unextend`, `//seldraw`,
  `//tracemask`, `//upload`, `//rotate <y> clockwise|counterclockwise`, and
  explicit `//sel clear` / `//gmask clear`. Each is rewritten to a native FAWE
  command, so FAWE remains responsible for permissions and execution.
- `//schematic search [-dfn] [-p page] <text>` fuzzy-sorts files from FAWE's
  configured schematic directory and produces clickable native load commands.

## License

FaweKit is available under the [MIT License](LICENSE).
