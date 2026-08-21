# QuickTrash — Short Description (for Modrinth)

**QuickTrash** is a lightweight Paper 26.2+ plugin that provides a temporary trash inventory for players. Players can open an 18-slot container via `/trash` to store items. Contents are saved and automatically deleted after a configurable time. Valuable items (enchanted, named, or defined in config) require confirmation before deletion. Shift-click deletes items instantly. The plugin uses bStats for anonymous usage statistics.

---

# QuickTrash — Full Description (for Modrinth)

## About QuickTrash

QuickTrash is a minimalist Paper plugin that adds temporary trash functionality to Minecraft servers. It is ideal for servers that need a simple way to dispose of items without the complexity of a full management system.

## Features

- **18-Slot Trash Inventory**: Open via `/trash` a temporary container
- **Auto-cleanup**: Contents are deleted after a configurable time (default: 30 seconds)
- **Instant Delete**: Shift-click deletes items without confirmation
- **Valuable Item Protection**: Enchanted, named items and configured materials require double-click confirmation
- **bStats Integration**: Anonymous usage tracking with plugin ID 33565
- **Configurable**: Timeout, GUI texts, and valuable materials are adjustable

## Installation

1. Download the latest release
2. Place the JAR file in your `plugins/` folder
3. Restart the server
4. Adjust configuration in `plugins/QuickTrash/config.yml`
5. Restart the server again

## Usage

### Commands

| Command | Description | Permission |
|--------|-------------|------------|
| `/trash` | Opens the trash inventory | `quicktrash.use` |
| `/quicktrash version` | Shows the plugin version | — |
| `/quicktrash reload` | Reloads the configuration | `quicktrash.admin` |

### How It Works

1. Players open `/trash` and receive an 18-slot inventory
2. Items are placed into this inventory (via drag & drop)
3. After the configured time expires or when the inventory is closed, items are permanently deleted
4. Shift-click on items in the inventory deletes them instantly
5. Valuable items show a confirmation prompt

## Configuration

The `config.yml` file is automatically generated and contains the following options:

```yaml
trash:
  auto-clear-seconds: 30

gui:
  title: "&8QuickTrash"
  info-name: "QuickTrash"
  info-lore:
    - "&7Right-click to store items"
    - "&7Shift-click to delete instantly"
    - "&7Valuable items require confirmation"
    - "&7Time until auto-clear: {seconds}s"

valuable-items:
  materials:
    - DIAMOND
    - NETHERITE_INGOT
    - GOLD_INGOT
    - EMERALD
    - ENCHANTED_GOLDEN_APPLE
    - TOTEM_OF_UNDYING
  confirmation-window: 3
```

## Requirements

- **PaperMC** 26.2 or compatible fork (Purpur, etc.)
- **Java** 25
- No additional dependencies required

## License

This plugin is licensed under the MIT License. See `LICENCE` for details.

## bStats

QuickTrash sends anonymous statistics to [bStats](https://bstats.org). You can disable this in `plugins/bStats/config.yml`.