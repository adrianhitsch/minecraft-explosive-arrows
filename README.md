# Explosive Arrows

Fabric mod for **Minecraft 26.3**: four explosive arrows for the vanilla bow, crossbow and dispenser – from a small
bang to the end of the world – and a CBU-87 cluster bomb.

## The arrows

| Arrow | Recipe (shapeless) | What happens |
|---|---|---|
| **Small Explosive Arrow** | Arrow + Gunpowder | Explosion like 1 TNT |
| **Explosive Arrow** | Arrow + 2× Gunpowder | Much bigger explosion (power 8.6) |
| **Projectile of God** | Arrow + Nether Star + TNT | Power 35 with fire, lightning, explosions into the depth and 12 aftershocks around the impact |
| **Do Not Use!** | Projectile of God + Nether Star + Dragon's Breath + End Crystal | See below. Seriously. |

All arrows explode when they hit a block or a living entity and disappear afterwards. If the game rule
`mob_griefing` is `false`, they only deal damage and don't destroy any blocks.

The item names are available in English and German.

### "Do Not Use!"

About 20 times as strong as the Projectile of God:

1. Impact, then silence. A thunderstorm rolls in, the Wither screams and a 3-second countdown begins.
2. Explosion with power 80: a sphere with a radius of ~32 blocks is completely hollowed out.
3. A shockwave of explosion rings travels up to 66 blocks outwards.
4. 20 burning Projectiles of God fall from the sky.

> ⚠️ The main explosion hits living entities up to about 160 blocks away. Anyone within bow range will most likely
> not survive. Chests inside the crater disappear including their contents. Best tried in a separate creative-mode
> test world.

## CBU-87 cluster bomb

Recipe (shaped): Iron Block on top and bottom, TNT – Fire Charge – TNT in the middle row. A true-to-scale (2.3 m),
round 3D model of the US CBU-87/B Combined Effects Munition.

- **Drop:** right-click into the air. The bomb keeps your speed, so drop it while flying with elytra. It noses down,
  spins up on its canted fins and opens about 20 blocks above the ground (on impact at the latest).
- **Dispenser:** drops the bomb out of its front side. Mount it high for an air burst.
- **Place:** right-click on a block puts down an inert bomb to look at. Hit it to pick it up again.

- **Launcher:** the **CBU-87 Launcher** (recipe: Iron Ingots on top and bottom, Flint and Steel – empty – Tripwire
  Hook in the middle row) with a scope:
  - **Hold right mouse button** to look through the scope (4x zoom). The red dot is the aiming point for up to about
    60 blocks; the marks below it are the hold-over points for 80 and 100 blocks (at the default FOV of 70).
  - **Left mouse button** fires the loaded bomb. With the launcher in hand you cannot mine or hit.
  - **R** (changeable under Controls) reloads: takes 2.5 seconds and a CBU-87 from your inventory (offhand first, in
    creative mode none). The tube swings into view and you see the bomb (fins folded) slide in.

  A fired bomb flies straight under rocket power for one second, then follows a ballistic arc. Its proximity fuze
  opens it shortly before it would hit a block or as soon as a mob is within 3 blocks of its path. The recoil pushes
  you back, and the **backblast** burns anyone standing up to 5 blocks behind you.

When it opens, 40 BLU-97 bomblets drift down on their ballutes and spread out in an ellipse that is stretched along
the flight direction. Each bomblet explodes on impact (a bit more than half a TNT) and may start a fire. About 8 % are
**duds**: they stay on the ground (yellow, easy to spot) and go off as soon as someone steps on them or hits them.

The exact values are constants in the code, see
[ExplosiveArrowType](src/main/java/explosivearrows/ExplosiveArrowType.java),
[GodStrike](src/main/java/explosivearrows/GodStrike.java) and
[DoomStrike](src/main/java/explosivearrows/DoomStrike.java),
[ClusterBombEntity](src/main/java/explosivearrows/ClusterBombEntity.java) and
[BombletEntity](src/main/java/explosivearrows/BombletEntity.java).

## Installation

1. Install Minecraft **26.3** with **Fabric Loader 0.19.5** or newer.
2. Put **[Fabric API](https://modrinth.com/mod/fabric-api)** for 26.3 into your `mods` folder. The mod won't start
   without it.
3. Put `explosivearrows-<version>.jar` into your `mods` folder (Windows: `%appdata%\.minecraft\mods`).

You can get the jar file from **Releases** or build it yourself (see below). Every push to `main`/`develop` is also
built automatically; the file is then attached to the run under **Actions** as the artifact "explosivearrows".

In multiplayer, the server **and** all players need the mod.

## Building

Requirement: **JDK 25**, e.g. from [Adoptium](https://adoptium.net).

| | Windows | Linux / macOS |
|---|---|---|
| Build and test | `build.cmd` (double-click) | `./build.sh` |
| Try it in game | `client.cmd` (double-click) | `./client.sh` |

The scripts find Java on their own (`JAVA_HOME` or `java` on the PATH, on macOS also via `java_home`). The finished
mod is then located at `build/libs/explosivearrows-<version>.jar`; the `-sources.jar` next to it only contains the
source code.

Without the scripts you can use Gradle directly: `./gradlew build` or `./gradlew runClient`.

## Development

- Code: `src/main/java/explosivearrows/`, client renderer in `src/client/`.
- Automated tests: server GameTests in `src/gametest/`. They run with every `build` (~15 s).
- Notes on the Minecraft 26.3 APIs and the project: [CLAUDE.md](CLAUDE.md).

## License

[CC0 1.0](LICENSE). The arrows use Minecraft's own arrow textures. The CBU-87 and BLU-97 textures are generated by
`scripts/generate-textures.py`.
