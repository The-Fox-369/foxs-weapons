# Fox's Weapons 🦊⚔️

**Fox's Weapons** is a NeoForge weapon mod for Minecraft 26.2 focused on weapons that actually have their own distinct mechanics, custom models, and playstyles instead of simply being another sword with a bigger damage number.

The finished base mod is planned to contain **15 unique weapons**, divided across three progression tiers.

---

> ⚠️ **IMPORTANT: DOCUMENTATION UPDATE NOTICE**
> 
> Maintaining documentation across the GitHub README, the GitHub Wiki, and Modrinth as a **solo developer** has become too tiring and time-consuming. 
> 
> **The GitHub README and GitHub Wiki are now OBSOLETE and will NO LONGER BE UPDATED.**
> 
> Please consult the official **[Modrinth Page](https://modrinth.com/mod/foxs-weapons)** for all future up-to-date documentation, changelogs, weapon stats, and guides. The Modrinth description will continue to be updated with every new release.

---

## 📜 Addons & Usage Policy

- **Addons & Compatibility:** You are completely welcome to make addons, compatibility patches, and custom content for *Fox's Weapons*!
- **Credit Required:** You must clearly credit *Fox's Weapons* and link back to the official [Modrinth Page](https://modrinth.com/mod/foxs-weapons). Never reupload the base mod or claim ownership of the original code or assets.
- **Strictly Non-Commercial:** Neither *Fox's Weapons* nor any addon built for it may be sold, put behind paywalls (including early access), or monetized in any way. Keep it 100% free for the community.

---

## 🎯 Current Alpha: `0.6.0-alpha.1` — Sling Pocket

### Currently Implemented Weapons (6 / 15)

#### 🌋 Volcano Hammer (Tier 3)
A slow volcanic demolition mallet.
- Custom heavy swing animation
- Right-click **Volcano Smash**: Creates a 3×3 fire ring with a safe center
- Grants Fire Resistance while held + passive lava particles
- 1200 durability | Repairable with **Magma Cream** | Mace-style enchantments

#### 💥 Blunderbuss (Tier 2)
A close-range firearm with dual firing modes.
- Left-click: accurate single shot | Right-click: four-pellet blast
- Uses **Iron Nuggets** as ammunition | 32-block max range
- Muzzle flash, smoke trails, impact effects, and strong knockback
- 450 durability | Repairable with **Iron Ingots** | Durability enchantments

#### 💀 Soul Reaper (Tier 3)
A heavy vampiric scythe built around vanilla critical hits.
- Vanilla jump criticals trigger **Soul Steal**
- Soul Steal adds +5 damage (+2.5 hearts) and heals the wielder by up to 5 HP
- Visible soul particles travel from the victim toward the wielder
- 1450 durability | Repairable with **Diamonds** | Spear-like melee enchantments

#### ⚡ Tempest Bow (Tier 3)
A storm-powered bow designed with thoroughly questionable levels of power.
- Full-strength draw in **0.25 seconds / 5 ticks**
- Adds **+14 Tempest damage** and summons **real vanilla lightning** on impact (entities, blocks, or missed shots)
- Custom GeckoLib draw-and-release animation
- 1000 durability | Repairable with **Copper Ingots** | Bow enchantments

#### 🕸️ Weighted Net (Tier 1)
A ranged control weapon that deals **zero direct damage**.
- Right-click to throw; tethers a living target to the wielder (cannot break from distance)
- Right-click again to release, applying **Rope Burns** (temporarily reduces movement speed)
- Custom bundled 3D model, custom projectile, and visible rope tether effect

#### 🪨 Sling Pocket (Tier 1)
A primitive ranged weapon containing wood, rubber, and rock.
- Fires instantly with **no charge-up** using stone-based ammunition
- Strong projectile knockback with custom stone projectiles, 3D model, and poses
- **Can hit Endermen** (Yes, this is intentional. They dodge arrows, not aggressive rocks).

---

## 🧪 Status Effects

### Rope Burns
Applied when a target tethered by the Weighted Net is released.
- Reduces movement speed for approximately **5 seconds**.

---

## 🗺️ Planned 15-Weapon Progression

| # | Weapon | Tier | Status |
|---|---|---|---|
| 1 | Bone Shiv | Tier 1 | Planned |
| 2 | Flint Spear | Tier 1 | Planned |
| 3 | Sling Pocket | Tier 1 | ✅ Implemented (`0.6.0-alpha.1`) |
| 4 | Weighted Net | Tier 1 | ✅ Implemented (`0.5.0-alpha.1`) |
| 5 | Spiked Club | Tier 1 | Planned |
| 6 | Heavy Greatsword | Tier 2 | Planned |
| 7 | Poisoned Needle | Tier 2 | Planned |
| 8 | Blunderbuss | Tier 2 | ✅ Implemented |
| 9 | Iron Vanguard Shield | Tier 2 | Planned |
| 10 | Smoke Bomb | Tier 2 | Planned |
| 11 | Soul Reaper | Tier 3 | ✅ Implemented |
| 12 | Tempest Bow | Tier 3 | ✅ Implemented |
| 13 | Volcano Hammer | Tier 3 | ✅ Implemented |
| 14 | Ender Rapier / Staff | Tier 3 | Planned |
| 15 | Withering Katana | Tier 3 | Planned |

---

## 🛠️ Repair and Enchanting

| Weapon | Repair Material | Enchantment Family |
|---|---|---|
| Volcano Hammer | Magma Cream | Mace-style |
| Blunderbuss | Iron Ingot | Durability-compatible |
| Soul Reaper | Diamond | Spear-like melee |
| Tempest Bow | Copper Ingot | Bow |
| Weighted Net | TBD | TBD |
| Sling Pocket | TBD | TBD |

---

## 📋 Requirements

- **Minecraft:** `26.2`
- **NeoForge:** `26.2.0.59`
- **Java:** `25`
- **GeckoLib:** `5.5.4`

---

## 🛠️ Build From Source

To build the development JAR locally:

```bash
# On Linux / macOS:
./gradlew build

# On Windows PowerShell:
.\gradlew.bat build
```

The generated JAR will be located in `build/libs/`.

---

## 🔗 Official Links

- **Modrinth (Primary Source of Truth):** [modrinth.com/mod/foxs-weapons](https://modrinth.com/mod/foxs-weapons)
- **Source Code:** [github.com/The-Fox-369/foxs-weapons](https://github.com/The-Fox-369/foxs-weapons)
- **Issue Tracker:** [github.com/The-Fox-369/foxs-weapons/issues](https://github.com/The-Fox-369/foxs-weapons/issues)

> **Testing Tip:** The target dummy is recommended for weapon testing. The Sulfur Cube tried. It was not effective.

---

## 📄 License

**All Rights Reserved** (See Addon & Usage Policy for third-party integration terms).
