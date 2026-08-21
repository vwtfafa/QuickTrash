# QuickTrash

QuickTrash is a small Paper 26.2 trash inventory plugin.

## Build

Requires Java 25 and Gradle. Run:

```text
./gradlew build
```

The resulting plugin is in `build/libs/QuickTrash-1.0.0.jar`.

## Behavior

`/trash` opens an 18-slot trash area. Contents persist in `plugins/QuickTrash/trash-data.yml` while the session is active and are permanently removed after the configured timeout. Shift-click deletes immediately; valuable items require a second click within the configured confirmation window unless the player has `quicktrash.bypass`.

## Commit Conventions

This project follows [Conventional Commits](https://www.conventionalcommits.org/). See `.claude/Git Commit Conventions.md` for the full guide.

Commit types:
- `feat:` — new feature
- `fix:` — bug fix
- `docs:` — documentation changes
- `chore:` — maintenance / miscellaneous
- `refactor:` — code restructuring
- `perf:` — performance improvements
- `test:` — tests added or modified
- `build:` — build system or dependency changes
- `ci:` — CI/CD and GitHub Actions changes
- `style:` — formatting/style-only changes
- `revert:` — reverting a previous commit
