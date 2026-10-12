package explosivearrows;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

/**
 * Follow-up explosions of the Projectile of God: depth layers along the flight direction (for a real crater)
 * and aftershocks with lightning in the surrounding area.
 */
public final class GodStrike {
	private static final int DEPTH_LAYERS = 3;
	private static final double DEPTH_SPACING = 6.0;
	private static final float DEPTH_POWER = 20.0f;
	private static final int DEPTH_INTERVAL_TICKS = 2;

	private static final int COUNT = 12;
	private static final int FIRST_TICK = 8;
	private static final int LAST_TICK = 40;
	private static final double RADIUS = 16.0;
	private static final float POWER = 10.0f;

	private GodStrike() {
	}

	/** Impact effects immediately, depth layers and aftershocks over the following ticks. */
	static void trigger(ServerLevel level, Vec3 center, Vec3 direction) {
		RandomSource random = level.getRandom();
		impactEffects(level, center, random);

		Vec3 dir = direction.lengthSqr() < 1.0E-6 ? new Vec3(0, -1, 0) : direction.normalize();
		for (int i = 1; i <= DEPTH_LAYERS; i++) {
			Vec3 pos = center.add(dir.scale(i * DEPTH_SPACING));
			DelayedActions.schedule(level, i * DEPTH_INTERVAL_TICKS,
					l -> ExplosiveArrowEntity.explode(l, pos.x, pos.y, pos.z, DEPTH_POWER, true));
		}
		for (int i = 0; i < COUNT; i++) {
			Vec3 pos = center.add(randomInSphere(random, RADIUS));
			DelayedActions.schedule(level, FIRST_TICK + i * (LAST_TICK - FIRST_TICK) / (COUNT - 1), l -> {
				lightning(l, pos);
				ExplosiveArrowEntity.explode(l, pos.x, pos.y, pos.z, POWER, true);
			});
		}
	}

	private static void impactEffects(ServerLevel level, Vec3 center, RandomSource random) {
		lightning(level, center);
		for (int i = 0; i < 3; i++) {
			lightning(level, center.add((random.nextDouble() - 0.5) * 8, 0, (random.nextDouble() - 0.5) * 8));
		}
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, true, center.x, center.y, center.z, 25, 6.0, 3.0, 6.0, 0.0);
		level.sendParticles(ParticleTypes.LAVA, true, true, center.x, center.y, center.z, 150, 4.0, 2.0, 4.0, 0.5);
		level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, true, center.x, center.y + 2, center.z, 120, 5.0, 4.0, 5.0, 0.05);
	}

	/** Purely visual lightning (thunder, flash); damage and fire come from the explosions. */
	static void lightning(ServerLevel level, Vec3 pos) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(pos);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
	}

	/** Uniformly distributed point in a sphere with the given radius. */
	static Vec3 randomInSphere(RandomSource random, double radius) {
		while (true) {
			double x = random.nextDouble() * 2 - 1;
			double y = random.nextDouble() * 2 - 1;
			double z = random.nextDouble() * 2 - 1;
			if (x * x + y * y + z * z <= 1) {
				return new Vec3(x, y, z).scale(radius);
			}
		}
	}
}
