# Create: Armed Arms

Create: Armed Arms is a NeoForge mod for Minecraft 1.21.1 that lets Create mechanical arms act as automatic weapon arms.

## Gameplay

- Right-click a Create mechanical arm with a sword, axe, mace, trident, bow, crossbow, or similar weapon to arm it; the arm visually holds the equipped weapon using a hand-held item pose.
- Right-click an armed mechanical arm with another weapon to swap it directly, or with an empty hand to take the weapon back.
- Select arrow source inventories with the mechanical arm's normal input selection before placing it.
- Ranged weapons target nearby hostile mobs within the configured range, defaulting to 15 blocks to match vanilla skeleton bow range, and require line of sight.
- The arm must be powered by Create rotational energy. Bow draw time is normal at 32 RPM, and other RPM values scale the draw time as a direct multiplier down to a short minimum delay.
- Ranged weapons preload one arrow from the selected source, wait for a hostile target, draw for the scaled draw time, fire once fully drawn, then repeat. Bow shots use bow velocity and critical arrows. Crossbow shots use crossbow arrow velocity. Weapon enchantment projectile hooks are applied when the arrow spawns.
- Ranged weapons only draw while a hostile target is in range, and the arm turns toward the target while aiming.
- Ranged weapons double the mechanical arm's stress impact while equipped.
- Melee weapons attack hostile mobs within vanilla player melee range and use their normal main-hand attack damage and attack speed.

## Current Limitations

- The mod consumes normal arrow-like ammo from selected mechanical arm input targets. Firework crossbow ammo is not implemented yet.
- Only weapons recognized as melee or projectile weapons are accepted. Unsupported custom combat items are ignored instead of guessed.

## Compatibility

- Minecraft: `1.21.1`
- Loader: NeoForge `21.1.228+`
- Requires: Create `6.0.10+`
- Java: `21`

## License

Create: Armed Arms is dedicated to the public domain under CC0 1.0 Universal.

NeoForge MDK template files remain under their original MIT license; see `TEMPLATE_LICENSE.txt`.

## Releasing

- Push commits to `main` as usual.
- Bump `mod_version` in `gradle.properties`.
- Create and push a tag `v<mod_version>` (example: `v0.1.1`).
- GitHub Actions `publish.yml` will build and publish to:
  - CurseForge (requires repository secret `CURSEFORGE_TOKEN`)
  - Modrinth (requires repository secret `MODRINTH_TOKEN` and repository variable `MODRINTH_PROJECT_ID`)
