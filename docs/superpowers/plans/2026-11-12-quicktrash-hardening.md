# QuickTrash 2.1 Hardening Implementation Plan

> **For agentic workers:** Execute inline with TDD where practical.

**Goal:** Ship persistence correctness, performance, Paper/dep updates, and the feature set approved after the audit.

**Architecture:** Keep manager/listener/command layers; extend TrashSession/GUI with control row + undo buffer; config-version 2 additive migration.

**Tech Stack:** Paper 26.3.build.249, Java 25, bStats, optional PlaceholderAPI

## Tasks
1. Bump paper-api + checkstyle; expand config.yml v2 + plugin.yml (folia, aliases, softdepend)
2. Persistenz: snapshot/dirty on delete & deposits; debounce flush; stats interval
3. Hot path: static valuable components, sound cache, filler cache, data-component GUI items
4. Features: undo, clear-all, blocked materials, confirm-all, rows/cooldown/worlds/sounds, admin inspect
5. Ops: update checker, PAPI expansion, richer bStats, Folia-safe viewer updates
6. Tests + `./gradlew build`
