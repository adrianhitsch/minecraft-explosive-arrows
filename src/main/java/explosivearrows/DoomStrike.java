package explosivearrows;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * "Do Not Use!": countdown, then main explosion, hollowed-out crater, shockwave and a rain of
 * Projectiles of God.
 */
public final class DoomStrike {
	private static final int COUNTDOWN_TICKS = 60;

	/** Crater radius: ~20 times the volume of a Projectile of God crater. */
	private static final int CRATER_RADIUS = 32;
	private static final int CRATER_LAYERS_PER_TICK = 8;

	private static final int[] SHOCKWAVE_RADII = {36, 46, 56, 66};
	private static final int SHOCKWAVE_INTERVAL_TICKS = 4;
	private static final float SHOCKWAVE_POWER = 12.0f;

	private static final int GOD_RAIN_COUNT = 20;
	private static final int GOD_RAIN_START = 20;
	private static final int GOD_RAIN_INTERVAL_TICKS = 8;
	private static final double GOD_RAIN_RADIUS = 48.0;
	private static final double GOD_RAIN_HEIGHT = 70.0;

	private DoomStrike() {
	}

	static void trigger(ServerLevel level, Vec3 center) {
		// A thunderstorm rolls in
		level.getServer().setWeatherParameters(0, 6000, true, true);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 20.0f, 0.5f);

		for (int t = 0; t < COUNTDOWN_TICKS; t += 10) {
			float pitch = 0.5f + 1.5f * t / COUNTDOWN_TICKS;
			DelayedActions.schedule(level, t, l -> countdownPulse(l, center, pitch));
		}
		DelayedActions.schedule(level, COUNTDOWN_TICKS - 10, l ->
				l.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 20.0f, 0.5f));
		DelayedActions.schedule(level, COUNTDOWN_TICKS, l -> detonate(l, center));
	}

	private static void countdownPulse(ServerLevel level, Vec3 center, float pitch) {
		level.playSound(null, center.x, center.y, center.z, SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 20.0f, pitch);
		// Light beam and contracting particle ring
		level.sendParticles(ParticleTypes.END_ROD, true, true, center.x, center.y + 25, center.z, 300, 0.3, 25.0, 0.3, 0.02);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, true, center.x, center.y + 1, center.z, 400, 8.0, 1.0, 8.0, 0.5);
	}

	private static void detonate(ServerLevel level, Vec3 center) {
		RandomSource random = level.getRandom();
		ExplosiveArrowType type = ExplosiveArrowType.DOOM;
		ExplosiveArrowEntity.explode(level, center.x, center.y, center.z, type.explosionPower(), type.createsFire());

		level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 30.0f, 0.5f);
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4;
			GodStrike.lightning(level, center.add(Math.cos(angle) * 12, 0, Math.sin(angle) * 12));
		}
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, true, center.x, center.y, center.z, 80, 16.0, 8.0, 16.0, 0.0);
		level.sendParticles(ParticleTypes.SONIC_BOOM, true, true, center.x, center.y + 2, center.z, 60, 10.0, 4.0, 10.0, 0.0);
		level.sendParticles(ParticleTypes.LAVA, true, true, center.x, center.y, center.z, 400, 10.0, 4.0, 10.0, 1.0);
		level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, true, center.x, center.y + 4, center.z, 300, 12.0, 8.0, 12.0, 0.08);

		if (level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			BlockPos centerPos = BlockPos.containing(center);
			for (int top = CRATER_RADIUS, tick = 0; top >= -CRATER_RADIUS; top -= CRATER_LAYERS_PER_TICK, tick++) {
				int fromDy = top;
				DelayedActions.schedule(level, tick, l -> carveLayers(l, centerPos, fromDy));
			}
		}

		for (int i = 0; i < SHOCKWAVE_RADII.length; i++) {
			int radius = SHOCKWAVE_RADII[i];
			DelayedActions.schedule(level, 2 + i * SHOCKWAVE_INTERVAL_TICKS, l -> shockwaveRing(l, center, radius));
		}

		for (int i = 0; i < GOD_RAIN_COUNT; i++) {
			DelayedActions.schedule(level, GOD_RAIN_START + i * GOD_RAIN_INTERVAL_TICKS + random.nextInt(GOD_RAIN_INTERVAL_TICKS),
					l -> dropGodArrow(l, center));
		}
	}

	/** Removes the blocks of the crater sphere from {@code fromDy} downwards ({@link #CRATER_LAYERS_PER_TICK} layers). */
	private static void carveLayers(ServerLevel level, BlockPos center, int fromDy) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int toDy = Math.max(fromDy - CRATER_LAYERS_PER_TICK + 1, -CRATER_RADIUS);
		for (int dy = fromDy; dy >= toDy; dy--) {
			int y = center.getY() + dy;
			if (level.isOutsideBuildHeight(y)) {
				continue;
			}
			for (int dx = -CRATER_RADIUS; dx <= CRATER_RADIUS; dx++) {
				for (int dz = -CRATER_RADIUS; dz <= CRATER_RADIUS; dz++) {
					pos.set(center.getX() + dx, y, center.getZ() + dz);
					// Irregular edge so the crater does not look like a perfect sphere
					double radius = CRATER_RADIUS - (Mth.getSeed(pos) & 0xFF) / 255.0 * 3.0;
					if (dx * dx + dy * dy + dz * dz > radius * radius) {
						continue;
					}
					BlockState state = level.getBlockState(pos);
					// Unbreakable blocks (bedrock, barriers, portal frames) stay
					if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
						continue;
					}
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	private static void shockwaveRing(ServerLevel level, Vec3 center, int radius) {
		int count = radius / 2;
		double offset = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < count; i++) {
			double angle = offset + i * Math.PI * 2 / count;
			int x = Mth.floor(center.x + Math.cos(angle) * radius);
			int z = Mth.floor(center.z + Math.sin(angle) * radius);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
			if (y > level.getMinY()) {
				ExplosiveArrowEntity.explode(level, x + 0.5, y, z + 0.5, SHOCKWAVE_POWER, true);
			}
		}
	}

	/** A burning Projectile of God falls straight down from the sky. */
	private static void dropGodArrow(ServerLevel level, Vec3 center) {
		RandomSource random = level.getRandom();
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = Math.sqrt(random.nextDouble()) * GOD_RAIN_RADIUS;
		double x = center.x + Math.cos(angle) * distance;
		double z = center.z + Math.sin(angle) * distance;
		double y = Math.min(center.y + GOD_RAIN_HEIGHT, level.getMaxY() - 2);
		ExplosiveArrowEntity arrow = new ExplosiveArrowEntity(level, x, y, z, new ItemStack(ModItems.GOD_ARROW), null, ExplosiveArrowType.GOD);
		arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		arrow.setDeltaMovement((random.nextDouble() - 0.5) * 0.3, -3.0, (random.nextDouble() - 0.5) * 0.3);
		arrow.setRemainingFireTicks(2000);
		level.addFreshEntity(arrow);
	}
}
