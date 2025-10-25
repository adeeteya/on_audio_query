# Repository Guidelines

## Project Structure & Module Organization
This is a Melos-managed Flutter plugin workspace. Core Dart APIs live in `packages/on_audio_query/lib/`, while platform-specific adapters sit in `packages/on_audio_query_android`, `packages/on_audio_query_ios`, and `packages/on_audio_query_web`. Shared contracts remain in `packages/on_audio_query_platform_interface`. Each package includes its own `example/` app for manual validation and a co-located `test/` directory. Automation assets and CI rules are under `.github/`, and workspace-wide tooling config (Melos, pubspec, IDE settings) is rooted at the repo top level.

## Build, Test, and Development Commands
- `melos bootstrap`: Install all package dependencies and link local path overrides; run after cloning or touching pubspecs.
- `melos run analyze`: Executes `flutter analyze --no-fatal-infos` across every package; keep the tree clean before pushing.
- `melos run format`: Applies `dart format --fix` to every package; run prior to committing stylistic changes.
- `flutter test packages/on_audio_query`: Runs unit/widget tests for the core plugin; replace the path to scope per package.
- `flutter run --target example/lib/main.dart`: Launch a specific example app for manual validation on a device or emulator.

## Coding Style & Naming Conventions
Follow the standard Dart style guide: two-space indentation, trailing commas on multi-line literals, and `dart format` as the source of truth. Use `UpperCamelCase` for types, `lowerCamelCase` for members, and `snake_case` for file names (e.g., `audio_query_controller.dart`). Platform channels should mirror their platform folder (`android/src/main/kotlin/...`) to keep namespace parity with `lib/src/platforms/`.

## Testing Guidelines
Author tests next to the behavior under `test/`, naming files `<feature>_test.dart`. Prefer `package:test` and `flutter_test` for Dart logic, plus platform-integration tests when touching Android/iOS channels. Aim to cover data parsing utilities and media query edge cases (empty libraries, large collections). Run `flutter test --coverage` inside each affected package when altering query or cache code, and attach coverage deltas to PRs touching critical paths.

## Commit & Pull Request Guidelines
Commits should carry concise, imperative summaries (e.g., “Align Android build.gradle with AGP 8.3”) mirroring the existing history. Reference issues or PRs with `(#123)` when applicable and keep changes scoped to a single concern. Pull requests must include: a short overview, a checklist of manual or automated tests (`melos run analyze`, `flutter test`), screenshots or logs for UI/platform changes, and links to related issues. Ensure CI passes and reviewers can reproduce your steps with the commands listed above.
