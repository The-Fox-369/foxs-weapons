# FoxsWeapons 🦊🔨

A Minecraft weapon mod for **Minecraft 26.2 / NeoForge**, using **GeckoLib 5** for custom 3D models and animations.

## Current weapon

- **Volcano Hammer** — custom model, texture, renderer, and animation asset.
- **Blunderbuss** — custom model, texture, renderer, and animation asset.

## Requirements

- Java 25
- Minecraft 26.2
- NeoForge 26.2.0.59
- GeckoLib 5.5.3

## Build

```bash
./gradlew build
```

On Windows PowerShell:

```powershell
.\gradlew.bat build
```

If Gradle dependencies or IDE indexes become stale:

```bash
./gradlew clean build --refresh-dependencies
```

Generated folders such as `build/`, `.gradle/`, and `run/` are intentionally excluded from Git.
