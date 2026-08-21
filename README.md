# Fox's Weapons 🦊⚔️

A Minecraft weapon mod for **Minecraft 26.2 / NeoForge**, using **GeckoLib 5** for custom 3D models and animations.

Fox's Weapons is planned to contain **15 unique weapons**, each with its own mechanics, model, effects, and playstyle instead of simply being another stronger sword.

## Current Alpha Lineup

| Version | Weapon | Status |
|---|---|---|
| `0.1.0-alpha.1` | **Volcano Hammer** | Uploaded to Modrinth |
| `0.2.0-alpha.1` | **Blunderbuss** | Uploaded to Modrinth |
| `0.3.0-alpha.1` | **Soul Reaper** | Uploaded to Modrinth / current source version |

The GitHub source currently contains all three weapons above. Alpha builds may still receive balancing, animation, visual, recipe, or mechanical changes.

## Weapons

### 🔥 Volcano Hammer

A slow, heavy volcanic melee weapon.

- Custom 3D model and heavy swing animation
- Right-click **Volcano Smash**
- Creates a 3×3 fire ring
- Lava, flame, and smoke effects
- Fire Resistance while held
- Passive lava particles

### 💥 Blunderbuss

A close-range firearm with two firing modes.

- Uses **Iron Nuggets** as ammunition
- Left click: accurate single shot
- Right click: four-pellet burst
- Custom firing animation
- Muzzle flash, smoke trails, impact particles, and knockback

### 💀 Soul Reaper

A heavy scythe focused on critical hits and life stealing.

- Custom 3D model and scythe animation
- Vanilla jump critical hits trigger life steal
- Steals **½ heart** from the victim and transfers it to the wielder
- Soul particles visibly travel from the victim toward the attacker

## Requirements

- Java 25
- Minecraft 26.2
- NeoForge 26.2.0.59
- GeckoLib 5.5.3

## Build From Source

```bash
./gradlew build
```

On Windows PowerShell:

```powershell
.\gradlew.bat build
```

To assemble the development JAR without running the full build lifecycle:

```powershell
.\gradlew.bat assemble
```

If Gradle dependencies or IDE indexes become stale:

```bash
./gradlew clean build --refresh-dependencies
```

Generated folders such as `build/`, `.gradle/`, and `run/` are intentionally excluded from Git.

## Links

- [Modrinth](https://modrinth.com/mod/foxs-weapons)
- [Wiki](https://github.com/The-Fox-369/foxs-weapons/wiki)
- [Issues](https://github.com/The-Fox-369/foxs-weapons/issues)

## Testing Tip

> The dummy is recommended for weapon testing. The Sulfur Cube tried. It was not effective.

See [CHANGELOG.md](CHANGELOG.md) for the alpha release history.
