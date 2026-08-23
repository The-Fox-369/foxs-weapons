# 💥 Blunderbuss

**Release:** `0.2.0-alpha.1`

**Tier:** Tier 2

The **Blunderbuss** is a close-range firearm built around two firing modes.

It is strongest when used aggressively at short range.

---

## Ammunition

The Blunderbuss uses:

**Iron Nuggets**

One trigger pull consumes one Iron Nugget.

In Creative Mode, ammunition is unlimited.

---

## Left Click — Precise Shot

Left click fires:

- 1 pellet
- very low spread
- long effective accuracy
- fast cooldown

This is the more reliable option for targets that are not standing directly in front of your face.

---

## Right Click — Burst

Right click fires:

**4 pellets**

The pellets have much greater spread than the precise shot.

At close range, several or all pellets can hit the same target.

At longer range, the burst becomes much less predictable.

---

## Range

Maximum pellet range:

**32 blocks**

Pellets stop when they hit solid blocks.

---

## Pellet System

The Blunderbuss does not spawn physical bullet entities.

Its shots are calculated procedurally:

```text
Fire
↓
Generate pellet direction
↓
Raycast toward blocks
↓
Check living entities
↓
Find nearest valid impact
↓
Spawn tracer / impact particles
↓
Apply damage
```

This keeps the weapon fast and avoids creating unnecessary projectile entities.

---

## Multiple Pellet Hits

If several burst pellets hit the same target during one shot, their damage is combined into one damage event.

This avoids Minecraft's normal damage invulnerability interfering with pellets fired on the same tick.

More pellets hitting the target also means stronger knockback.

---

## Durability

**450**

One durability is consumed per trigger pull.

---

## Repair Material

**Iron Ingot**

---

## Enchantments

The Blunderbuss currently uses durability-compatible enchantments.

Gun-specific enchantments may be considered later rather than pretending the Blunderbuss is secretly a bow.

---

## Recipe

```text
I I I
W G I
W W S
```

- `I` — Iron Ingot
- `W` — Wooden Plank
- `G` — Gunpowder
- `S` — Stick

---

## Development Note

Damage values may differ between uploaded alpha releases and the latest development source while balance testing continues.

---

[Back to Home](Home)