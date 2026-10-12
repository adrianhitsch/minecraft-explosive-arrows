# Explosive Arrows

Fabric mod for **Minecraft 26.3**: four explosive arrows for the vanilla bow, crossbow and dispenser – from a small
bang to the end of the world.

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

The exact values are constants in the code, see
[ExplosiveArrowType](src/main/java/explosivearrows/ExplosiveArrowType.java),
[GodStrike](src/main/java/explosivearrows/GodStrike.java) and
[DoomStrike](src/main/java/explosivearrows/DoomStrike.java).

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

[CC0 1.0](LICENSE). The mod contains no textures of its own; it uses Minecraft's own arrow textures.
