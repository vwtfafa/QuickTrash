# QuickTrash — Short Description (for Modrinth)

**QuickTrash 2.0.0** is a lightweight Paper 26.3 plugin that provides a temporary trash inventory for players. Players can open an 18-slot container via `/trash` to store items. Contents are saved and automatically deleted after a configurable time. Valuable items (enchanted, named, or defined in config) require confirmation before deletion. Shift-click deletes items instantly. The plugin uses bStats for anonymous usage statistics.

---

# QuickTrash — Full Description (for Modrinth)

## About QuickTrash

QuickTrash is a minimalist Paper plugin that adds temporary trash functionality to Minecraft servers. It is ideal for servers that need a simple way to dispose of items without the complexity of a full management system.

## Features

- **18-Slot Trash Inventory**: Open via `/trash` a temporary container
- **Auto-cleanup**: Contents are deleted after a configurable time (default: 30 seconds)
- **Instant Delete**: Shift-click deletes items without confirmation
- **Valuable Item Protection**: Enchanted, named, data-component-customized, and configured items use a confirmation dialog
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
| `/quicktrash stats` | Shows deleted-item statistics | — |
| `/quicktrash reload` | Reloads the configuration | `quicktrash.admin` |

### How It Works

1. Players open `/trash` and receive an 18-slot inventory
2. Items are stored by shift-clicking them or dragging them into the container
3. Contents are saved per player and deleted automatically after the configured time (default: 30 seconds)
4. Shift-clicking an item in the trash deletes it instantly
5. Valuable items show a confirmation dialog before being moved or deleted

## Configuration

The `config.yml` file is automatically generated and contains the following options:

```yaml
metrics:
  enabled: true

trash:
  auto-clear-seconds: 30

gui:
  title: '<dark_gray>QuickTrash'
  info-name: '<aqua>QuickTrash'
  sounds:
    open: block.ender_chest.open
    delete: block.lava.extinguish
  info-lore:
    - '<gray>Items are deleted after <white>{seconds}s<gray>.'
    - '<gray>Shift-click an item to delete it now.'

valuable-items:
  enabled: true
  require-confirmation: true
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

## Requirements

- **PaperMC** 26.3 (Build 206 or newer) or a compatible fork (Purpur, etc.)
- **Java** 25
- No additional dependencies required

## License

This plugin is licensed under the MIT License. See `LICENCE` for details.

## bStats

QuickTrash sends anonymous statistics to [bStats](https://bstats.org). You can disable this in `plugins/bStats/config.yml`.