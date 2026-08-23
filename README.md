# Fox's Weapons 🦊⚔️

A Minecraft weapon mod for **Minecraft 26.2 / NeoForge**, using **GeckoLib 5** for custom 3D models and animations.

Fox's Weapons is planned to contain **15 unique weapons**. The goal is simple: every weapon should have its own mechanic, model, effects, and playstyle instead of just being another sword with a larger damage number.

> **Current alpha:** `0.4.0-alpha.1` — Tempest Bow

## Current Weapons

| Version | Weapon | Tier | Main mechanic | Repair material |
|---|---|---:|---|---|
| `0.1.0-alpha.1` | **Volcano Hammer** | 3 | Volcano Smash + Fire Resistance | Magma Cream |
| `0.2.0-alpha.1` | **Blunderbuss** | 2 | Single shot + four-pellet burst | Iron Ingot |
| `0.3.0-alpha.1` | **Soul Reaper** | 3 | Critical-hit life steal | Diamond |
| `0.4.0-alpha.1` | **Tempest Bow** | 3 | Fast draw + real lightning on impact | Copper Ingot |

**4 weapons down. 11 to go.**

## Volcano Hammer

A slow, heavy volcanic melee weapon.

- 1200 durability
- +8 Attack Damage modifier
- -3.2 Attack Speed modifier
- Custom heavy swing animation
- Right-click **Volcano Smash**
- Creates a 3×3 fire ring with a safe center
- Fire Resistance while held
- Passive lava particles while held
- Volcano Smash cooldown: 6 seconds
- Volcano Smash durability cost: 3
- Repair material: **Magma Cream**
- Intended enchantment family: **mace-style**

## Blunderbuss

A close-range firearm with two firing modes.

- 450 durability
- Uses **Iron Nuggets** as ammunition
- Maximum range: 32 blocks
- Left click: precise single shot
  - 8 damage
  - very low spread
  - 0.6-second cooldown
- Right click: four-pellet burst
  - 5 damage per pellet
  - up to 20 total damage if all four pellets connect
  - wider spread
  - 1.2-second cooldown
- Muzzle flash, smoke trails, impact particles, and knockback
- Repair material: **Iron Ingot**
- Supports durability-compatible enchantments

## Soul Reaper

A heavy scythe built around powerful melee attacks and actual vanilla critical hits.

- 1450 durability
- +20 Attack Damage modifier
- -3.1 Attack Speed modifier
- Custom heavy scythe swing animation
- Vanilla jump critical hits trigger **Soul Steal**
- Soul Steal adds **5 health points / 2.5 hearts** of damage
- Heals the wielder by up to **5 health points / 2.5 hearts**, limited by health actually lost by the victim
- Soul particles visibly travel from the victim toward the wielder
- Repair material: **Diamond**
- Intended enchantment family: **spear-like melee**

## Tempest Bow

A storm-powered bow designed around extremely fast firing and real vanilla lightning.

- 1000 durability
- Full-strength draw after only **0.25 seconds / 5 ticks**
- Uses vanilla bow ammunition
- Adds **+14 damage** to Tempest arrow hits before normal armor mitigation
- Summons **real vanilla lightning** at the exact impact location
- Works on entities, blocks, walls, roofs, trees, the ground, and missed shots
- Each Tempest arrow can summon lightning only once
- Uses normal vanilla lightning, including its normal side effects
- Custom GeckoLib draw-and-release animation
- Repair material: **Copper Ingot**
- Intended enchantment family: **bow**
- Holding a Tempest Bow in the offhand can affect arrows fired from another bow
- Yes, that survived development. It is a feature.

## Enchanting and Repair

All four current weapons have **15 enchantability** and have repair materials.

| Weapon | Repair material | Enchantment direction |
|---|---|---|
| Volcano Hammer | Magma Cream | Mace-style |
| Blunderbuss | Iron Ingot | Durability-compatible |
| Soul Reaper | Diamond | Spear-like melee |
| Tempest Bow | Copper Ingot | Bow |

Alpha builds may still receive balancing or compatibility changes.

## Requirements

- **Java 25**
- **Minecraft 26.2**
- **NeoForge 26.2.0.59**
- **GeckoLib 5.5.3**

## Build From Source

Windows PowerShell:

```powershell
.\gradlew.bat build
```

To assemble the development JAR without running the full build lifecycle:

```powershell
.\gradlew.bat assemble
```

The generated JAR is placed in:

```text
build/libs/
```

## Links

- [Modrinth](https://modrinth.com/mod/foxs-weapons)
- [Wiki](https://github.com/The-Fox-369/foxs-weapons/wiki)
- [Issues](https://github.com/The-Fox-369/foxs-weapons/issues)

## Testing Tip

> **The dummy is recommended for weapon testing. The Sulfur Cube tried. It was not effective.**

## License

**All Rights Reserved**
