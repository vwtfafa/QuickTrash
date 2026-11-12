# QuickTrash

QuickTrash 2.1.0 is a lightweight Paper 26.3 plugin that provides a temporary trash inventory for players.

## Features

- Configurable trash inventory (`/trash`, aliases like `/bin`)
- Session persistence with NBT item blobs and debounced YAML flush
- Shift-click delete, clear-all button, undo after clear
- Valuable-item Paper dialogs (optional confirm-all)
- Blocked materials, per-world disable, open cooldown
- Admin open/clear for other players
- Folia-supported schedulers, update checker, PlaceholderAPI hooks, bStats charts

## Installation

1. Download the latest release from [GitHub Releases](https://github.com/vwtfafa/QuickTrash/releases)
2. Place the `.jar` file in your server's `plugins/` directory
3. Restart your server
4. Edit `plugins/QuickTrash/config.yml` as needed
5. Run `/quicktrash reload` (or restart) to apply changes

## Usage

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/trash` | Open your trash inventory | `quicktrash.use` |
| `/quicktrash version` | Display plugin version | — |
| `/quicktrash stats` | Display deleted-item stats | — |
| `/quicktrash reload` | Reload configuration | `quicktrash.admin` |
| `/quicktrash clear [player]` | Clear trash (own or other) | `quicktrash.use` / `quicktrash.admin.others` |
| `/quicktrash open <player>` | Inspect another player's trash | `quicktrash.admin.others` |

### Placeholders (PlaceholderAPI)

| Placeholder | Description |
|-------------|-------------|
| `%quicktrash_player%` / `%quicktrash_deleted%` | Player deleted-item count |
| `%quicktrash_total%` | Server total deleted items |
| `%quicktrash_sessions%` | Active trash sessions |

### Behavior

- Content rows are configurable (`trash.rows`, default 2 → 18 slots) plus a control row (undo / info / clear)
- Contents are saved to `plugins/QuickTrash/trash-data.yml` as NBT blobs
- Items auto-delete after the configured timeout; undo restores the last clear briefly
- Shift-click deletes immediately (unless confirm-all / valuable rules apply)
- Valuable items use a Paper confirmation dialog

## Permissions

| Permission | Description |
|------------|-------------|
| `quicktrash.use` | Use `/trash` |
| `quicktrash.admin` | Reload configuration |
| `quicktrash.admin.others` | Open/clear other players' trash |
| `quicktrash.bypass` | Bypass valuable confirmations |

## Metrics

This plugin uses [bStats](https://bstats.org/plugin/bukkit/QuickTrash/33565). Opt out via `metrics.enabled: false` or the global bStats config.

## Requirements

- PaperMC 26.3 (Build 249 or newer recommended) or compatible fork
- Java 25
- Optional: PlaceholderAPI

## Building from Source

```bash
git clone https://github.com/vwtfafa/QuickTrash.git
cd QuickTrash
./gradlew build
```

The compiled plugin will be in `build/libs/QuickTrash-2.1.0.jar`.

---
*QuickTrash is open source and maintained by [vwtfafa](https://github.com/vwtfafa).*
