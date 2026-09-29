# QuickTrash

QuickTrash 2.0.0-beta.1 is a lightweight Paper 26.3 plugin that provides a temporary trash inventory for players.

## Features

- 18-slot trash inventory accessible via `/trash`
- Items persist during session and auto-delete after timeout
- Shift-click for instant deletion
- Valuable item protection with confirmation requirement
- bStats integration for anonymous usage tracking
- Configurable timeout, GUI, and valuable items list

## Installation

1. Download the latest release from [GitHub Releases](https://github.com/vwtfafa/QuickTrash/releases)
2. Place the `.jar` file in your server's `plugins/` directory
3. Restart your server (or reload the plugin)
4. Edit `plugins/QuickTrash/config.yml` to adjust settings as needed
5. Restart/reload again to apply changes

## Usage

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/trash` | Open the trash inventory | `quicktrash.use` |
| `/quicktrash version` | Display plugin version | — |
| `/quicktrash stats` | Display deleted-item stats | — |
| `/quicktrash reload` | Reload configuration | `quicktrash.admin` |

### Behavior

- Trash inventory contains 18 slots for temporary item storage
- Contents are saved to `plugins/QuickTrash/trash-data.yml` while player is online
- Items are automatically deleted after the configured timeout (default: 30 seconds)
- Shift-clicking an item deletes it immediately without confirmation
- Valuable items use a Paper confirmation dialog before being moved to trash or permanently deleted
- Valuable items include: enchanted items, named items, custom model data, persistent data, and materials listed in config

## Configuration

The `config.yml` file generates automatically with these options:

```yaml
metrics:
  enabled: true

# Time in seconds before trash contents are auto-cleared
trash:
  auto-clear-seconds: 30

# GUI settings (MiniMessage format, {seconds} is replaced in info-lore)
gui:
  title: '<dark_gray>QuickTrash'
  info-name: '<aqua>QuickTrash'
  sounds:
    open: block.ender_chest.open
    delete: block.lava.extinguish
  info-lore:
    - '<gray>Items are deleted after <white>{seconds}s<gray>.'
    - '<gray>Shift-click an item to delete it now.'

# Valuable item protection (enchanted/named/custom-model/persistent data always count,
# materials below additionally depend on mode: WHITELIST or BLACKLIST)
valuable-items:
  enabled: true
  require-confirmation: true
  # Confirmation dialog action expires after this many seconds
  confirmation-timeout-seconds: 5
  mode: WHITELIST
  materials:
    - DIAMOND
    - EMERALD
    - NETHERITE_INGOT
    - NETHERITE_SWORD
    - NETHERITE_PICKAXE
    - ENCHANTED_GOLDEN_APPLE
```

## Permissions

| Permission | Description |
|------------|-------------|
| `quicktrash.use` | Allows using `/trash` command |
| `quicktrash.admin` | Allows reloading configuration |
| `quicktrash.bypass` | Bypasses valuable item confirmations |

## Metrics

This plugin uses [bStats](https://bstats.org/plugin/bukkit/QuickTrash/33565) to collect anonymous usage statistics. No personal data is collected. You can opt-out per-plugin via `metrics.enabled: false` in `plugins/QuickTrash/config.yml` or globally by disabling bStats in your server's `plugins/bStats/config.yml`.

## Requirements

- PaperMC 26.3 Beta or compatible fork (Purpur, etc.)
- Java 25
- No additional dependencies

## Building from Source

```bash
git clone https://github.com/vwtfafa/QuickTrash.git
cd QuickTrash
./gradlew build
```

The compiled plugin will be in `build/libs/QuickTrash-2.0.0-beta.1.jar`.

---
*QuickTrash is open source and maintained by [vwtfafa](https://github.com/vwtfafa).*
