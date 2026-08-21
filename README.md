# QuickTrash

QuickTrash is a lightweight Paper 26.2 plugin that provides a temporary trash inventory for players.

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
| `/quicktrash reload` | Reload configuration | `quicktrash.admin` |

### Behavior

- Trash inventory contains 18 slots for temporary item storage
- Contents are saved to `plugins/QuickTrash/trash-data.yml` while player is online
- Items are automatically deleted after the configured timeout (default: 30 seconds)
- Shift-clicking an item deletes it immediately without confirmation
- Items classified as "valuable" require a second click within the confirmation window to delete
- Valuable items include: enchanted items, named items, custom model data, persistent data, and materials listed in config

## Configuration

The `config.yml` file generates automatically with these options:

```yaml
# Time in seconds before trash contents are auto-cleared
trash.auto-clear-seconds: 30

# GUI settings
gui:
  title: "&8QuickTrash"
  info-name: "QuickTrash"
  info-lore:
    - "&7Right-click to store items"
    - "&7Shift-click to delete instantly"
    - "&7Valuable items require confirmation"
    - "&7Time until auto-clear: {seconds}s"

# Materials considered valuable (require confirmation to delete)
valuable-items:
  materials:
    - DIAMOND
    - NETHERITE_INGOT
    - GOLD_INGOT
    - EMERALD
    - ENCHANTED_GOLDEN_APPLE
    - TOTEM_OF_UNDYING
  # Require second click within this window (seconds) to delete valuable items
  confirmation-window: 3
```

## Permissions

| Permission | Description |
|------------|-------------|
| `quicktrash.use` | Allows using `/trash` command |
| `quicktrash.admin` | Allows reloading configuration |

## Metrics

This plugin uses [bStats](https://bstats.org/plugin/bukkit/QuickTrash/9756) to collect anonymous usage statistics. No personal data is collected. You can opt-out by disabling bStats in your server's `plugins/bStats/config.yml`.

## Requirements

- PaperMC 26.2 or compatible fork (Purpur, etc.)
- Java 25
- No additional dependencies

## Building from Source

```bash
git clone https://github.com/vwtfafa/QuickTrash.git
cd QuickTrash
./gradlew build
```

The compiled plugin will be in `build/libs/QuickTrash-1.0.0.jar`.

---
*QuickTrash is open source and maintained by [vwtfafa](https://github.com/vwtfafa).*
