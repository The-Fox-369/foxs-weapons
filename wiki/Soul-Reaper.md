# 💀 Soul Reaper

**Release:** `0.3.0-alpha.1`

**Tier:** Tier 3

The **Soul Reaper** is a heavy scythe built around slow, powerful melee attacks and soul-stealing critical hits.

---

## Main Attack

The Soul Reaper is intentionally slow.

It is designed to hit hard rather than attack rapidly.

It also uses a custom player swing animation.

---

## Ability — Soul Steal

Soul Steal activates on a real **vanilla jump critical hit**.

The weapon does not guess whether an attack was a critical based on fall distance or timing.

Minecraft's actual critical-hit event is used.

When a valid Soul Reaper critical connects:

```text
Critical hit
↓
Extra damage
↓
Health stolen from victim
↓
Health transferred to wielder
↓
Soul particles travel toward player
```

---

## Soul Effects

A successful Soul Steal creates visible soul particles:

- around the victim
- between the victim and wielder
- around the wielder when the soul arrives

---

## Important

A normal hit does **not** trigger Soul Steal.

The attack must be recognised by Minecraft as a vanilla critical hit.

---

## Durability

**1450**

---

## Repair Material

**Diamond**

---

## Enchantments

The Soul Reaper is being treated as a **spear-like melee weapon** for enchantment compatibility.

Enchantments are still being tested during development.

---

## Recipe

```text
D S D
  I
  B
```

- `D` — Diamond
- `S` — Wither Skeleton Skull
- `I` — Iron Ingot
- `B` — Blaze Rod

---

## Development Note

The latest GitHub source contains balance changes ahead of the uploaded `0.3.0-alpha.1` build.

Values shown by development builds may therefore differ from the currently uploaded alpha.

---

[Back to Home](Home)