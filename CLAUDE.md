# Explosive Arrows

Fabric mod for Minecraft 26.3 (Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.4, Java 25).
What the mod does: `README.md`; exact values are constants in the code (`ExplosiveArrowType`, `GodStrike`,
`DoomStrike`). Automated tests: server GameTests in `src/gametest/`. Only change existing tests after checking with
the user; adding new tests is fine.

The project language is English (code, comments, docs, commit messages). The user communicates in German.

## Building and testing
- Requires JDK 25 (`JAVA_HOME` or `java` on the PATH). Shells started before the JDK was installed may not know
  `JAVA_HOME` yet: set `JAVA_HOME` for the session or use the scripts.
- Scripts: `build.cmd`/`build.sh` (build + tests, Windows opens `build\libs` afterwards) and `client.cmd`/`client.sh`
  (runClient). Java detection lives in `scripts/find-java.{cmd,sh}`.
- `build` also runs the GameTests (~15 s). Done = `build` green.
- Testing in game: start `runClient` in its own terminal tab; the user plays and gives feedback.
- CI: `.github/workflows/build.yml` builds on push/PR and uploads the jar as an artifact.

## Look it up instead of guessing
The Minecraft APIs have changed a lot; don't rely on knowledge of older versions.
- If available locally: `mc-sources/common`, `mc-sources/client` (decompiled 26.3 sources) and
  `mc-sources/resources` (vanilla data/assets). The folder is gitignored and must never be committed.
  Good references: `world/entity/projectile/arrow/SpectralArrow.java`, `world/item/SpectralArrowItem.java`,
  `world/item/Items.java`, `world/entity/EntityTypes.java`, `client/renderer/entity/EntityRenderers.java`.
- If needed, read Fabric API signatures with `javap` from the jars in the Gradle cache.
- Search specifically with Grep/Glob instead of scanning whole files in loops.

## Known details about 26.3
- Entity type constants live in `EntityTypes` (no longer `EntityType`), e.g. `EntityTypes.PIG`.
- Items need their ID in the properties (`setId(ResourceKey)`), entity types are built with `build(ResourceKey)`.
- Respect `mob_griefing`: explode with `Level.ExplosionInteraction.MOB` (like the Creeper); only create fire if
  `GameRules.MOB_GRIEFING` is on (like the Ghast fireball).
- Dispenser: vanilla only registers projectile behavior for its own items →
  call `DispenserBlock.registerProjectileBehavior(item)` for each arrow.
- Bow ammunition = item tag `#minecraft:arrows` → `src/main/resources/data/minecraft/tags/item/arrows.json`
  (tags from all mods are merged automatically).
- The arrow entity needs a client renderer (`EntityRendererRegistry`), otherwise the client crashes when it flies.
- Item models use the new format: `assets/<ns>/items/<name>.json` + `assets/<ns>/models/item/<name>.json`.
- `AbstractArrow` calls `getDefaultPickupItem()` from its constructor, before subclass fields are set.
- `AbstractArrow.onHitEntity` discards the arrow itself on a hit → don't check `isRemoved()` after `super.onHit`.
- Fabric API names (Mojang mappings): `ServerTickEvents.END_LEVEL_TICK`, `CreativeModeTabEvents.modifyOutputEvent`.
- Explosions lose power very quickly in stone; large craters therefore need several explosions or direct
  carving (`DoomStrike`).

## Structure
- `ExplosiveArrowType`: power and fire per arrow; `ModItems`/`ModEntities`: registration.
- `ExplosiveArrowEntity`: impact logic; `GodStrike` and `DoomStrike`: follow-up effects of the big arrows.
- `DelayedActions`: tick scheduler for delayed explosions and the countdown.
- CBU-87: `ClusterBombItem` (drop/place/dispenser), `ClusterBombEntity` (falling, opens), `BombletEntity`
  (incl. duds), `PlacedClusterBombEntity` (inert display), `ClusterBombLauncherItem` (fires bombs with
  `launched = true` → rocket motor + proximity fuze instead of the altitude fuze). Client mixin `AvatarRendererMixin`
  gives the launcher the crossbow-hold arm pose. Rotation convention of these entities: `yRot = atan2(x, z)`,
  `xRot` positive = nose up (not the mob convention).
- Launcher controls: right click = item use with `SPYGLASS` animation; `PlayerMixin` makes `isScoping()` true for it,
  `AbstractClientPlayerMixin` (zoom) and `HudMixin` (reticle) adjust the spyglass behaviour. Left click
  (`ClientPreAttackCallback`) and the reload key send `LauncherPayloads` to the server. State lives in item
  components (`ModComponents`: loaded, reload start time); `LauncherSpecialRenderer` animates the reload from them.
  Reload completion is scheduled with `DelayedActions`. The reticle's hold-over marks are computed in
  `scripts/generate-textures.py` from the bomb ballistics (keep its constants in sync).
- Round models: `client/Mesh` (lathe/plate/box mesh drawn via `submitCustomGeometry`), geometry in
  `client/CbuModels`. The item uses a `minecraft:special` model registered via `SpecialModelRenderers.ID_MAPPER`
  (Fabric API has no registry for it). Texture layout must match `scripts/generate-textures.py`
  (pure Python, no dependencies; re-run after changing it).
