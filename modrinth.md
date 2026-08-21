# QuickTrash — Kurzbeschreibung (für Modrinth)

**QuickTrash** ist ein leichtes Paper 26.2+ Plugin, das einen temporären Trash-Inventory für Spieler bereitstellt. Spieler können über `/trash` ein 18-Slot großes Behältnis öffnen, in das sie Gegenstände ablegen können. Inhalte werden gespeichert und nach einer konfigurierbaren Zeit automatisch gelöscht. Wertvolle Gegenstände (verzaubert, benannt oder in der Konfiguration definiert) erfordern eine Bestätigung vor dem Löschen. Shift-Klick löscht Gegenstände sofort. Das Plugin verwendet bStats für anonyme Nutzungsstatistiken.

---

# QuickTrash — Ausführliche Beschreibung (für Modrinth)

## Über QuickTrash

QuickTrash ist ein minimalistisches Paper-Plugin, das eine temporäre Trash-Funktionalität für Minecraft-Server hinzufügt. Es ist ideal für Server, die einen einfachen Weg zum Entsorgen von Gegenständen ohne die Komplexität eines vollständigen Verwaltungssystems benötigen.

## Funktionen

- **18-Slot Trash-Inventory**: Öffne über `/trash` einen temporären Behälter
- **Automatische Bereinigung**: Inhalte werden nach einer konfigurierbaren Zeit (Standard: 30 Sekunden) gelöscht
- **Sofortiges Löschen**: Shift-Klick löscht Gegenstände ohne Bestätigung
- **Wertgegenstände-Schutz**: Verzauster, benannter Gegenstände und definierte Materialien erfordern eine zweistufige Bestätigung
- **bStats-Integration**: Anonyme Nutzungsstatistiken mit Plugin-ID 33565
- **Konfigurierbar**: Timeout, GUI-Texte und wertvolle Materialien anpassbar

## Installation

1. Lade die neueste Version herunter
2. Platziere die JAR-Datei in deinen `plugins/` Ordner
3. Starte den Server neu
4. Passe die Konfiguration in `plugins/QuickTrash/config.yml` an
5. Starte den Server erneut neu

## Verwendung

### Befehle

| Befehl | Beschreibung | Berechtigung |
|--------|-------------|--------------|
| `/trash` | Öffnet das Trash-Inventar | `quicktrash.use` |
| `/quicktrash version` | Zeigt die Plugin-Version an | — |
| `/quicktrash reload` | Lädt die Konfiguration neu | `quicktrash.admin` |

### Wie es funktioniert

1. Spieler öffnen `/trash` und erhalten ein 18-Slot großes Inventar
2. Gegenstände werden in dieses Inventar gelegt (per Drag & Drop)
3. Nach Ablauf der konfigurierten Zeit oder beim Schließen des Inventars werden die Gegenstände dauerhaft gelöscht
4. Shift-Klick auf Gegenstände im Inventar löscht sie sofort
5. Wertvolle Gegenstände zeigen eine Bestätigungsanforderung an

## Konfiguration

Die `config.yml` wird automatisch erstellt und enthält folgende Optionen:

```yaml
trash:
  auto-clear-seconds: 30

gui:
  title: "&8QuickTrash"
  info-name: "QuickTrash"
  info-lore:
    - "&7Rechtsklick zum Speichern"
    - "&7Shift-Klick zum sofortigen Löschen"
    - "&7Wertvolle Gegenstände benötigen Bestätigung"
    - "&7Zeit bis zur Automatik-Löschung: {seconds}s"

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

## Voraussetzungen

- **PaperMC** 26.2 oder kompatibler Fork (Purpur, etc.)
- **Java** 25
- Keine zusätzlichen Dependencies erforderlich

## Lizenz

Dieses Plugin steht unter der MIT-Lizenz. Siehe `LICENCE` für Details.

## bStats

QuickTrash sendet anonyme Statistiken an [bStats](https://bstats.org). Du kannst dies in der Datei `plugins/bStats/config.yml` deaktivieren.