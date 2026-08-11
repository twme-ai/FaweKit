# FaweKit command reference

Language: **English** | [Traditional Chinese](commands.zh-TW.md)

This reference describes the commands, FAWE masks, and compatibility syntax
implemented by FaweKit `0.1.0`. It follows the behavior of the current code,
including index bases, persistence rules, compatibility-only flags, and edge
cases.

FaweKit is an independent community project, not an official FastAsyncWorldEdit
plugin. This document covers additions and rewrites supplied by FaweKit. Refer
to FAWE's own documentation for its complete native command, mask, and pattern
syntax.

## Syntax conventions

- `//` is part of the command. For example, enter `//tpsel` in chat, not
  `/tpsel`.
- `<argument>` is required, `[argument]` is optional, and `a|b` means choose one.
- A form such as `[-abc]` accepts separate or combined flags, such as `-a -b`
  or `-ab`.
- Page and stack indexes do not all use the same base. Each command explicitly
  states whether its indexes start at `0` or `1`, and whether negatives work.
- All FaweKit commands except `//help-masks`, `//help-patterns`, and `//echo`
  require an in-game player.
- `<mask>` and `<pattern>` values use FAWE's parsers. In a FaweKit Bukkit
  command, each mask or pattern must be one argument with no spaces.

## Quick reference

| Command | Purpose | Permission |
| --- | --- | --- |
| `//tpsel`, `//seltp` | Teleport to the active selection or a stacked selection | `fawekit.tpsel` |
| `//multireplace`, `//multirepl` | Apply several replacements in one undoable edit | `fawekit.multireplace` |
| `//clipboard` | Inspect, resize, crop, and select clipboards | `fawekit.clipboard` |
| `//copynear` | Find nearby blocks, select their hull, and copy it | `fawekit.copynear` |
| `//autorotatepaste`, `//arp` | Rotate and paste according to two selection directions | `fawekit.autorotatepaste` |
| `//msel` | Manage the in-memory selection stack | `fawekit.msel` |
| `//ssel` | Persist selections in per-player YAML files | `fawekit.ssel` |
| `//bmask` | Set a biome-aware FAWE global mask | `fawekit.bmask` |
| `//help-masks` | Show a compact mask cheat sheet | `fawekit.help` |
| `//help-patterns` | Show a compact pattern cheat sheet | `fawekit.help` |
| `//echo` | Display expanded Minecraft block-name globs | `fawekit.echo` |
| `//shortcut`, `//sc` | Create and run per-player shortcuts | `fawekit.shortcut` |
| `//pin`, `//unpin` | Pin or unpin the location seen by native FAWE commands | `fawekit.pin` |
| `//schematic search` | Fuzzy-sort and list schematic files | `worldedit.schematic.load` |

See the [permission table](#permission-table) for Bukkit defaults.

## `//tpsel`: teleport to a selection

Alias: `//seltp`

```text
//tpsel [-s <index>] [<x> <y> <z>]
```

With no coordinates, the command searches for a safe destination:

1. It makes up to 64 random attempts outside the selection's horizontal
   bounding box but within 16 blocks of it. The block below must be solid, the
   feet and head positions must be passable, and sky light must be greater than
   `1`.
2. If that fails, it makes up to 64 attempts inside the three-dimensional
   bounding box. The destination must be safe, total light must be greater than
   `1`, and the surrounding `3 x 2 x 3` space must be passable.
3. If no attempt succeeds, the command reports an error and does not teleport.
   Non-cuboid selections use their bounding box during this search, so a point
   found in the second phase is not guaranteed to be inside the exact shape.

With three coordinates, relative values use the integer block coordinate of
the selection center as their base:

- `~` means center plus `0.5` on that axis, placing the player at block center.
- `~5` and `~-2.5` add that offset after the center plus `0.5`.
- `120` and `64.5` are absolute world coordinates.
- Coordinate mode does not perform a safety check and preserves the player's
  yaw and pitch.

`-s <index>` targets an entry in the `//msel` stack:

- `1` is the top, and `2` is the next item.
- `-1` is the bottom, and `-2` is the second item from the bottom.
- `0`, or omitting `-s`, uses the active FAWE selection.

Examples:

```text
//tpsel
//tpsel ~ ~5 ~
//tpsel -s 2
//tpsel -s -1 ~ ~2 ~
```

## `//multireplace`: conditional multi-replace

Alias: `//multirepl`

```text
//multireplace <mask> <pattern> [<mask> <pattern> ...]
```

Arguments must appear as mask-pattern pairs. The command parses every pair
before walking the active selection:

- One position can match more than one mask.
- When several pairs match, the last matching pattern wins.
- Masks test the world state from the start of the operation. This allows two
  block types to be swapped without the first replacement becoming input for
  the second.
- All changes share one EditSession and can be reverted by one `//undo`.
- The player's FAWE limits and global mask still apply.

Examples:

```text
//multireplace dirt stone stone dirt
//multireplace oak_log stripped_oak_log oak_wood stripped_oak_wood
//multireplace #skylight[15] glass #blocklight[1][15] glowstone
```

Minecraft/Bukkit does not guarantee that quotes combine arguments containing
spaces. Keep each `<mask>` and `<pattern>` itself free of spaces.

## `//clipboard`: clipboard utilities

Every subcommand requires a clipboard in the current FAWE session.

### Show dimensions

```text
//clipboard
//clipboard size
```

Both forms display the clipboard width, height, length, and origin coordinate.

### Stretch and compress

```text
//clipboard stretch <width> <height> <length>
//clipboard compress <width> <height> <length>
```

`stretch` and `compress` currently use the same resampling implementation. Both
can enlarge or shrink a clipboard; their names do not restrict the direction.
Each dimension accepts the following forms:

| Input | Meaning when the old size is `20` |
| --- | --- |
| `12` | Set the new size to 12 |
| `50%` | Set it to 50% of the old size, producing 10 |
| `~5` | Add 5 to the old size, producing 25 |
| `~-5` | Subtract 5 from the old size, producing 15 |
| `~25%` | Add 25% of the old size, producing 25 |
| `~` | Keep the old size |

Results are rounded to integers, with a minimum of `1` on every axis. Resizing
uses nearest-neighbor sampling. It does not smooth, blend, or rotate blocks.

```text
//clipboard stretch 200% 100% 200%
//clipboard compress 16 8 16
//clipboard stretch ~5 ~ ~25%
```

### Crop or expand

```text
//clipboard crop [-w <width>] [-h <height>] [-l <length>]
                 [-x <offset>] [-y <offset>] [-z <offset>]
```

- `-w`, `-h`, and `-l` set output width, height, and length. They accept the
  same absolute, percentage, and `~`-relative forms as resizing.
- `-x`, `-y`, and `-z` are integer offsets into the source. They do not accept
  percentages or `~`.
- A positive offset skips blocks on the source's low-coordinate side. A
  negative offset begins the output before the source range.
- Output positions outside the source remain air, so this command can also add
  empty padding.
- The clipboard origin is adjusted with the crop offset.

```text
//clipboard crop -w 20 -h 10 -l 20
//clipboard crop -x 2 -y 1 -z 2 -w ~-4 -h ~-1 -l ~-4
//clipboard crop -x -3 -w ~6
```

`stretch`, `compress`, and `crop` rebuild the primary clipboard. Full block
data is retained, but biomes, entities, other multi-clipboard entries, and an
existing transform are not copied. Retain the original schematic or repeat the
copy first when those data matter.

### List a multi-clipboard

```text
//clipboard list [-d] [-n] [-p <page>]
```

- Each page contains 10 entries.
- Page indexes start at `0`; the first page is `-p 0`.
- Each line shows a zero-based clipboard index, URI or fallback name, and its
  dimensions.
- `-d` and `-n` are accepted for compatibility with the proposed syntax but do
  not currently change the display.

### Select the primary clipboard

```text
//clipboard select [-n] <index>
```

Use the zero-based index shown by `//clipboard list`. The selected entry becomes
the session's sole primary ClipboardHolder. `-n` is currently a compatibility
flag and behaves the same as omitting it.

```text
//clipboard list -p 0
//clipboard select 2
```

## `//copynear`: find nearby blocks and copy their hull

```text
//copynear [-xbce] [-m <copyMask>] <searchMask> [distance]
```

The command searches a spherical radius centered on the player's current block
for positions matching `<searchMask>`. Distance defaults to `64` and must be
between `1` and `256`. Before searching, it estimates checks from the complete
bounding cube. The search is rejected if that estimate exceeds the player's
FAWE `MAX_CHECKS` limit.

After matches are found:

- One to three matches produce the smallest cuboid containing them.
- Four or more matches produce a convex polyhedral selection.
- The new region immediately becomes the active selection and sends a CUI
  update.
- Region contents are copied into a memory-optimized clipboard. Positions in a
  convex region's bounding box but outside the exact region remain air.
- If there are no matches, the clipboard is not changed.

Flags:

| Flag | Behavior |
| --- | --- |
| `-x` | Build the region from matches but do not write blocks matching `<searchMask>` to the clipboard |
| `-b` | Copy biomes |
| `-c` | Put the origin at the region's X/Z center and minimum Y instead of the normal FAWE placement position |
| `-e` | Copy entities inside the region |
| `-m <copyMask>` | Write only positions that match this additional mask |

When `-x` and `-m` are combined, a copied position must match `<copyMask>` and
must not match `<searchMask>`. For example, find an ore body and copy its stone
surroundings without the ore itself:

```text
//copynear -x -m stone diamond_ore 32
//copynear -xbce #tag[logs] 48
```

Each mask must be a single argument without spaces. Searching for air or using
a broad mask over a large radius can quickly hit `MAX_CHECKS` or create a large
clipboard.

## `//autorotatepaste`: rotate from one selection direction to another

Alias: `//arp`

```text
//autorotatepaste [-abenosr] [-m <sourceMask>]
```

Basic workflow:

1. Create the first directional selection and run native `//copy`. FaweKit
   records the direction from the selection's primary point to its second point.
2. Create a new directional selection. Its primary point is the default paste
   destination.
3. Run `//arp`. The command finds the lowest-cost combination of 90-degree X,
   Y, and Z rotations that aligns the old direction with the new direction. It
   combines that rotation with the current clipboard transform and pastes.

Only the positive, negative, or zero sign of each direction axis is compared;
length is ignored. The command fails when the two directions cannot be aligned
by axis-aligned 90-degree rotations. Cuboids use `pos1` to `pos2`. Polygonal and
convex selections use their representative final point, while other region
types use their maximum point.

Flags:

| Flag | Behavior |
| --- | --- |
| `-a` | Ignore air in the source clipboard |
| `-b` | Paste biomes |
| `-e` | Paste entities |
| `-n` | Do not edit the world; only select the transformed cuboid bounds |
| `-o` | Use the clipboard's stored origin coordinate as the destination |
| `-s` | Select the pasted content's transformed cuboid bounds afterward |
| `-r` | Use FAWE's normal placement position instead of the new selection's primary point |
| `-m <sourceMask>` | Paste only source positions matching this mask |

`-o` and `-r` are mutually exclusive. Without either flag, the clipboard origin
is aligned to the current selection's primary point.

```text
//copy
//arp -as
//arp -r -abe
//arp -n
```

Each execution composes a new rotation with the ClipboardHolder's existing
transform, so repeated executions accumulate rotations. Repeat `//copy` or
reload the schematic when a calculation must start from an unrotated state.
After FaweKit starts, it must observe at least one `//copy` with a valid
direction before `//arp` can run.

## `//msel`: selection stack

```text
//msel <push|pop|combine|delete|clear|list|undo|redo> ...
```

The top item has positive index `1`, followed by `2`, `3`, and so on. For read
and delete operations, `-1` is the bottom and `-2` is the second item from the
bottom. The stack exists only in server memory and is lost after a server or
plugin restart. Use `//ssel save -m` to persist it.

### `push`

```text
//msel push [index]
```

Pushes a clone of the active selection. Omitting the index, or using `0`, adds
it at the top. A positive index inserts before that item, with values past the
bottom clamped to the bottom. A negative index inserts after the item counted
backward from the bottom.

### `pop`

```text
//msel pop [count|all]
```

The default count is 1. One popped item becomes the active selection. Multiple
items, or `all`, become a combined union region that remains usable by native
FAWE edit commands. Count must be between `1` and the current stack size.

### `combine`

```text
//msel combine
```

Pops the complete stack and turns all items into the active union region. An
empty stack cannot be combined. This is equivalent to `//msel pop all`.

### `delete` and `clear`

```text
//msel delete <index>
//msel clear
```

`delete` removes one entry and accepts positive or negative indexes. `clear`
removes every stack entry. Neither operation clears the active FAWE selection.

### `list`

```text
//msel list [-dn] [-p <page>]
```

Each page contains 10 entries, and page indexes start at `0`. Every line shows
a one-based item index, region type, and bounding coordinates. Clicking a line
runs `//tpsel -s <index>` to teleport to that selection. `-d` and `-n` are
accepted compatibility display flags and currently have no effect.

### `undo` and `redo`

```text
//msel undo
//msel redo
```

Up to 50 stack mutations are retained. This history affects only stack content;
it is not the block-editing `//undo`, and it does not restore the active
selection set by `pop` or `combine`. A new stack mutation clears redo history.

Example workflow:

```text
//msel push
//msel push
//msel list
//msel combine
//msel undo
```

## `//ssel`: saved selections

```text
//ssel <save|load|list|search|move|delete|clear|formats> ...
```

Each player's files are stored under:

```text
plugins/FaweKit/selections/<player UUID>/<name>.sel.yml
```

A name must be 1 to 64 characters and may contain only ASCII letters, digits,
dots, underscores, and hyphens. `.` and `..` are not valid names.

### Save and load

```text
//ssel save [-m] <name>
//ssel load <name>
```

- `save` writes the active selection, world name, and format version. An
  existing file with the same name is overwritten.
- `-m` also writes the current `//msel` stack.
- `load` restores the selection in the world recorded by the file. That world
  must currently be loaded.
- If the file contains a stack, loading replaces the current stack and creates
  a mutation that `//msel undo` can restore. If the file has no stack section,
  the current stack remains unchanged.
- Loading a selection from another world does not teleport the player. Travel
  to that world before operating on it.

Supported region types are cuboid, polygonal, convex polyhedral, cylindrical,
ellipsoidal, and the combined union regions produced by `//msel`.

```text
//ssel save castle
//ssel save -m village-plan
//ssel load village-plan
```

### List and search

```text
//ssel list [filter]
//ssel search <text>
```

Both perform a case-insensitive name substring filter and sort by name. `list`
shows all entries when the filter is omitted. `search` requires one text
argument without spaces. Results are not paginated.

### Rename and delete

```text
//ssel move <oldName> <newName>
//ssel delete <name>
```

`move` renames the file. The source must exist, and an existing destination is
not overwritten. `delete` removes the named file. It still reports completion
when the file did not exist.

### Clear the active selection and inspect formats

```text
//ssel clear
//ssel formats
```

`clear` clears only the active selection. It does not delete saved files or the
`//msel` stack. `formats` displays the only currently supported persistence
format: YAML (`.sel.yml`).

## `//bmask`: biome global mask

```text
//bmask <biome>[,<biome>...]
//bmask clear
//bmask
```

The first form replaces the current FAWE LocalSession global mask with a biome
mask. Subsequent edits are allowed only at positions in the listed biomes. The
`minecraft:` namespace is optional, matching is case-insensitive, and spaces
between comma-separated chat arguments are removed.

```text
//bmask plains,forest
//bmask minecraft:desert,minecraft:badlands
```

Both `//bmask clear` and a `//bmask` with no arguments set the global mask to
null. This clears any current global mask, not only one previously created by
`//bmask`.

## Environmental masks

FaweKit registers these masks in FAWE's MaskFactory, so they can be used in any
native FAWE command that accepts a mask. They have no separate permission; the
native command being executed remains responsible for permission checks.

### Masks without arguments

| Mask | Match condition |
| --- | --- |
| `#visible` | At least one of the six orthogonally adjacent positions is not opaque |
| `#sky` | Every position above it through the world's maximum Y is air; non-air blocks such as glass obstruct it |
| `#transparent` | The current block material is not opaque |
| `#conductive` | The current block material is opaque and is not `minecraft:observer` |
| `#haslight` | Sky light or emitted light is greater than `0` |
| `#nolight` | Both sky light and emitted light equal `0` |

### Numeric range masks

```text
#skylight[<level>]
#skylight[<minimum>][<maximum>]
```

All five masks below support the same syntax and require at least one bracketed
argument. One value matches that exact value. Two values form an inclusive
range. Values must be integers from `0` through `15`, and minimum cannot exceed
maximum.

| Mask | Value read at the position |
| --- | --- |
| `#skylight` | Sky light |
| `#blocklight` | Emitted light reported by the FAWE extent |
| `#light` | The greater of sky light and emitted light |
| `#emitslight` | The current block material's own light value |
| `#opacity` | Opacity |

Examples:

```text
//replace #visible stone
//replace #skylight[15] glass
//replace #light[0][3] glowstone
//multireplace #emitslight[1][15] sea_lantern #nolight air
```

## `//help-masks`: mask cheat sheet

```text
//help-masks
```

Displays a fixed compact example list covering:

- A single block and its inverse: `stone`, `!stone`
- OR and AND conditions: `stone,dirt`, `stone dirt`
- `#existing` and `#surface`
- `>stone` and `<stone`
- A Minecraft tag: `#tag[mineable/pickaxe]`
- Constraining edits by biome with `//bmask`

This is a cheat sheet, not a complete list of every FAWE mask. Its permission
is open to all players by default, and it can also be run from the console.

## `//help-patterns`: pattern cheat sheet

```text
//help-patterns
```

Displays a fixed compact example list covering:

- A single block and weighted random: `stone`, `70%stone,30%dirt`
- `#clipboard` and `#existing`
- Copying compatible block states: `^stone`
- An offset pattern: `#offset[1][0][0][stone]`
- A repeating sequence: `#linear[stone,dirt]`

This is a cheat sheet, not a complete list of every FAWE pattern. Its
permission is open to all players by default, and it can also be run from the
console.

## `//echo`: expand block-name globs

```text
//echo <text...>
```

The command examines each whitespace-delimited argument and displays it after
expanding Minecraft block-name globs:

- `*` matches zero or more arbitrary characters.
- `?` matches exactly one arbitrary character.
- Only the `minecraft` namespace is searched. `minecraft:` may be included or
  omitted.
- A glob resource name must use lowercase alphanumerics, underscore, dot,
  comma, hyphen, `*`, or `?`.
- Matches are sorted and joined with commas. A token with no matches is left
  unchanged.

```text
//echo //replace mud_brick_* stone
//echo minecraft:*_log
```

Output begins with `@> `. `//echo` only displays the expanded text. It does not
execute it or add glob support to other commands. The console can run it.

## `//shortcut`: per-player shortcuts

Alias: `//sc`

```text
//sc <new|delete|move|list|search|history|export|name> ...
```

A normal name may contain ASCII letters, digits, dots, underscores, and
hyphens, with a length from 1 through 64. Names are case-insensitive and stored
in lowercase. A name beginning with `#` is a text-fragment shortcut described
below. `new`, `delete`, `move`, `list`, `search`, `history`, `export`, and
`import` are always parsed as subcommands and should not be used as normal
shortcut names.

### Create, overwrite, delete, and rename

```text
//sc new <name> <command-or-text...>
//sc delete <name>
//sc move <oldName> <newName>
```

- `new` creates a shortcut and overwrites an existing shortcut with the same
  name.
- `delete` requires the shortcut to exist.
- `move` renames a shortcut. If the destination exists, its content is
  overwritten.

```text
//sc new walls //walls ${1:-stone}
//sc move walls buildwalls
//sc delete buildwalls
```

### Execute a command shortcut

```text
//sc <name> [argument...]
```

There is no separate `execute` subcommand. Put the shortcut name directly after
`//sc`. The expanded result may begin with `/` or omit it. Bukkit ultimately
dispatches the result as the player, so permissions of the target command still
apply.

Parameter expansion forms:

| Form | Behavior |
| --- | --- |
| `${1}`, `${2}` | First or second call argument; missing arguments become empty strings |
| `${@}` | All call arguments joined with spaces |
| `${1:-default}` | Use `default` when the argument is empty |
| `${1:+alternate}` | Use `alternate` when an argument is present; otherwise use an empty string |
| `${1:?message}` | Cancel and display `message` when the argument is missing |

```text
//sc new walls //walls ${1:-stone}
//sc walls
//sc walls deepslate
//sc new required //set ${1:?Provide a block}
//sc new passthrough //replace ${@}
```

### `#` text-fragment shortcuts

```text
//sc new #ground stone,dirt,grass_block
//set #ground
```

A name beginning with `#` cannot be executed as `//sc #ground`. FaweKit looks
for that token in other commands sent by the player and performs a textual
replacement. The primary use is reusing FAWE masks and patterns. A fragment can
refer to another `#` fragment; expansion is bounded to prevent recursion beyond
five levels. `//shortcut` and `//sc` themselves skip fragment expansion so that
fragments can be created and managed.

### List, search, and history

```text
//sc list [filter]
//sc search [filter]
//sc history [filter]
```

- `list` and `search` behave identically. They perform a case-insensitive
  substring filter against names or contents, sort by name, and do not paginate.
- History retains the latest 200 non-cancelled commands, newest first. Exactly
  identical consecutive commands are recorded once.
- `history` displays at most the first 20 matching entries.

### Export

```text
//sc export
```

Writes shortcut content to:

```text
plugins/FaweKit/shortcuts/exports/<player UUID>.yml
```

The export replaces that player's previous export and contains shortcuts only,
not command history. `//sc import` is intentionally disabled and does not load
from a URL or file.

Shortcuts and history are normally persisted in:

```text
plugins/FaweKit/shortcuts/<player UUID>.yml
```

## `//pin` and `//unpin`: pin the native FAWE command location

```text
//pin
//unpin
```

`//pin` records the player's current FAWE location. While it is active, FaweKit
executes later native `//` commands through FAWE's
LocationMaskedPlayerWrapper. Those commands see the pinned location, but the
player is not teleported, and the existing FAWE session and permissions remain
unchanged.

This is useful when the player needs to move for visibility while `//paste`, a
brush, or another native command must continue using the original location.
`//unpin` removes the pin.

Limitations:

- Only native FAWE double-slash commands are intercepted.
- Normal `/tp`, other Bukkit commands, and the player's physical location are
  unaffected.
- FaweKit's own commands in this reference are not affected and still see the
  player's current location.
- Pin state exists only in memory and is lost after a plugin or server restart.

## Compatibility syntax

The following inputs do not implement a second editing engine. FaweKit rewrites
the command string and passes it to the installed FAWE native command, which
remains responsible for arguments, limits, execution, and permissions.

| Input syntax | Rewritten command |
| --- | --- |
| `//repeat ...` | `//stack ...` |
| `//unextend ...` | `//contract ...` |
| `//seldraw ...` | `//drawsel ...` |
| `//upload ...` | `//download ...` |
| `//tracemask ...` | `/tracemask ...`, using one slash |
| `//sel clear` | `//sel` |
| `//gmask clear` | `//gmask` |
| `//rotate <y> clockwise [other rotations...]` | `//rotate <y> [other rotations...]` |
| `//rotate <y> counterclockwise [other rotations...]` | `//rotate <-y> [other rotations...]` |

The clockwise form requires an integer or decimal `<y>` value.
`counterclockwise` negates it, while everything after the third argument is
preserved. For example:

```text
//rotate 90 clockwise
//rotate 90 counterclockwise 0 0
```

becomes:

```text
//rotate 90
//rotate -90 0 0
```

`//sel clear` and `//gmask clear` are rewritten only when they contain exactly
those two words. Extra arguments leave the input for FAWE to handle directly.

## `//schematic search`: search schematic files

```text
//schematic search [-dfn] [-p <page>] <text...>
```

This adds a search form to FAWE's native `//schematic` command:

- Files are walked recursively from FAWE's configured schematic root.
- When per-player schematics are enabled in FAWE, the player's UUID is appended
  to that root.
- `.schem`, `.schematic`, and `.mcedit` files are included.
- Results are first scored by case-insensitive ordered-character matching.
  Candidates that do not contain the characters in order use edit distance as
  a fallback. This ranks every file instead of strictly filtering, so weak
  matches can appear on later pages.
- Each page contains 10 results, and page indexes start at `0`.
- Clicking a result runs native `//schematic load <relative path>`, with the
  extension removed from the load name.
- `-d`, `-f`, and `-n` are accepted compatibility display flags and currently
  have no effect.

```text
//schematic search castle
//schematic search -p 1 medieval house
//schematic search -dfn tree
```

This feature uses FAWE's native `worldedit.schematic.load` permission and has no
additional `fawekit.*` node. Search text must follow `search`. An exact
`//schematic search` with no trailing query is left for the native FAWE command
to handle.

## Permission table

| Permission | Bukkit default | Scope |
| --- | --- | --- |
| `fawekit.tpsel` | `op` | `//tpsel`, `//seltp` |
| `fawekit.multireplace` | `op` | `//multireplace`, `//multirepl` |
| `fawekit.clipboard` | `op` | Every `//clipboard` subcommand |
| `fawekit.copynear` | `op` | `//copynear` |
| `fawekit.autorotatepaste` | `op` | `//autorotatepaste`, `//arp` |
| `fawekit.msel` | `op` | Every `//msel` subcommand |
| `fawekit.ssel` | `op` | Every `//ssel` subcommand |
| `fawekit.bmask` | `op` | `//bmask` |
| `fawekit.help` | `true` | `//help-masks`, `//help-patterns` |
| `fawekit.echo` | `op` | `//echo` |
| `fawekit.shortcut` | `op` | `//shortcut`, `//sc` |
| `fawekit.pin` | `op` | `//pin`, `//unpin` |
| `worldedit.schematic.load` | Defined by FAWE | `//schematic search` and loading its results |

Environmental masks and compatibility rewrites have no independent
`fawekit.*` permission. The native FAWE command that consumes them performs its
normal permission checks. The `op` and `true` values above are Bukkit defaults
from `plugin.yml`; a permissions plugin may override them.
