package explosivearrows;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Falling CBU-87: drops like a heavy bomb, points its nose along the flight path and opens above the ground
 * (or at the latest on impact), scattering {@link BombletEntity bomblets} in an elliptical footprint.
 *
 * <p>Rotation convention (also used by the renderer): {@code yRot = atan2(x, z)}, {@code xRot} = climb angle
 * (positive = nose up).
 */
public class ClusterBombEntity extends Entity {
	/** Height above ground (blocks) at which the dispenser opens. */
	public static final double OPEN_HEIGHT = 20.0;
	/** Fuze arming time: the bomb never opens in the air before this. */
	public static final int ARM_TICKS = 10;
	public static final int BOMBLET_COUNT = 40;
	private static final double GRAVITY = 0.06;
	private static final double DRAG = 0.985;
	/** Max. sideways speed of the bomblets when opening in the air / on impact. */
	private static final double SPREAD_AIR = 0.9;
	private static final double SPREAD_IMPACT = 0.4;
	/** The footprint is longer along the flight direction than across it. */
	private static final double SPREAD_ALONG_FLIGHT = 1.6;
	/** Share of the bomb's own velocity the bomblets keep. */
	private static final double CARRY_VELOCITY = 0.6;
	/** Launched bombs: proximity fuze arms after this many ticks (safety distance to the shooter). */
	public static final int LAUNCHED_ARM_TICKS = 5;
	/** Launched bombs open this many ticks of flight before hitting a block. */
	private static final double PROXIMITY_TICKS = 4.0;
	/** Launched bombs open when a living entity is this close to the flight path. */
	private static final double PROXIMITY_RADIUS = 3.0;
	/** Launched bombs open low, so the bomblets get a little extra lift for hang time. */
	private static final double LAUNCHED_LIFT = 0.25;

	/** Launched bombs fly straight (no gravity, no drag) while their rocket motor burns. */
	public static final int MOTOR_TICKS = 20;

	/** Fired from the {@link ClusterBombLauncherItem}: rocket motor and proximity fuze instead of the altitude fuze. */
	private static final EntityDataAccessor<Boolean> DATA_LAUNCHED = SynchedEntityData.defineId(ClusterBombEntity.class, EntityDataSerializers.BOOLEAN);

	public ClusterBombEntity(EntityType<? extends ClusterBombEntity> type, Level level) {
		super(type, level);
	}

	public ClusterBombEntity(Level level, double x, double y, double z, Vec3 velocity, float yaw) {
		this(level, x, y, z, velocity, yaw, 0.0F);
	}

	public ClusterBombEntity(Level level, double x, double y, double z, Vec3 velocity, float yaw, float pitch) {
		this(ModEntities.CLUSTER_BOMB, level);
		this.setPos(x, y, z);
		this.xo = x;
		this.yo = y;
		this.zo = z;
		this.setDeltaMovement(velocity);
		this.setYRot(yaw);
		this.yRotO = yaw;
		this.setXRot(pitch);
		this.xRotO = pitch;
	}

	public boolean isLaunched() {
		return this.entityData.get(DATA_LAUNCHED);
	}

	public void setLaunched(boolean launched) {
		this.entityData.set(DATA_LAUNCHED, launched);
	}

	private boolean isMotorBurning() {
		return isLaunched() && this.tickCount < MOTOR_TICKS;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		entityData.define(DATA_LAUNCHED, false);
	}

	@Override
	protected double getDefaultGravity() {
		return GRAVITY;
	}

	@Override
	protected MovementEmission getMovementEmission() {
		return MovementEmission.NONE;
	}

	@Override
	public void tick() {
		super.tick();
		boolean motor = isMotorBurning();
		if (!motor) {
			this.applyGravity();
		}
		this.move(MoverType.SELF, this.getDeltaMovement());
		if (!motor) {
			this.setDeltaMovement(this.getDeltaMovement().scale(DRAG));
		}
		this.updateRotation();
		if (motor && this.level().isClientSide()) {
			// Rocket exhaust at the tail
			Vec3 tail = this.position().add(0.0, 0.25, 0.0).subtract(this.getDeltaMovement().normalize().scale(1.3));
			this.level().addParticle(ParticleTypes.FLAME, tail.x, tail.y, tail.z, 0.0, 0.0, 0.0);
			this.level().addParticle(ParticleTypes.SMOKE, tail.x, tail.y, tail.z, 0.0, 0.02, 0.0);
		}
		if (this.level() instanceof ServerLevel level) {
			boolean launched = isLaunched();
			boolean impact = this.onGround() || this.horizontalCollision || this.verticalCollision || this.isInWater() || this.isInLava();
			if (impact) {
				open(level, true);
			} else if (launched ? this.tickCount >= LAUNCHED_ARM_TICKS && isTargetAhead(level)
					: this.tickCount >= ARM_TICKS && this.getDeltaMovement().y < 0.0 && isNearGround()) {
				open(level, false);
			}
		}
	}

	/** Proximity fuze: a block is about to be hit, or something living is close to the flight path. */
	private boolean isTargetAhead(ServerLevel level) {
		Vec3 from = this.position();
		Vec3 to = from.add(this.getDeltaMovement().scale(PROXIMITY_TICKS));
		HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
		if (hit.getType() != HitResult.Type.MISS) {
			return true;
		}
		AABB near = this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(PROXIMITY_RADIUS);
		return !level.getEntitiesOfClass(LivingEntity.class, near, e -> e.isAlive() && !e.isSpectator()).isEmpty();
	}

	/** Turns the nose smoothly into the flight direction. */
	private void updateRotation() {
		Vec3 v = this.getDeltaMovement();
		double horizontal = v.horizontalDistance();
		if (horizontal > 0.02) {
			float yaw = (float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG);
			this.setYRot(Mth.rotLerp(0.3F, this.getYRot(), yaw));
		}
		if (v.length() > 0.02) {
			float pitch = (float) (Mth.atan2(v.y, horizontal) * Mth.RAD_TO_DEG);
			this.setXRot(Mth.lerp(0.15F, this.getXRot(), pitch));
		}
	}

	private boolean isNearGround() {
		Vec3 from = this.position();
		Vec3 to = from.add(0.0, -OPEN_HEIGHT, 0.0);
		HitResult hit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
		return hit.getType() != HitResult.Type.MISS;
	}

	/** Opens the dispenser: removes the bomb and scatters the bomblets. */
	void open(ServerLevel level, boolean impact) {
		if (this.isRemoved()) {
			return;
		}
		this.discard();
		Vec3 pos = this.position().add(0.0, impact ? 0.5 : 0.0, 0.0);
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 1.7F);
		level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 3, 0.5, 0.5, 0.5, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 30, 0.8, 0.4, 0.8, 0.05);

		RandomSource random = level.getRandom();
		Vec3 velocity = this.getDeltaMovement();
		double lift = impact ? 0.3 : velocity.y * 0.3 + (isLaunched() ? LAUNCHED_LIFT : 0.0);
		Vec3 carried = new Vec3(velocity.x * CARRY_VELOCITY, lift, velocity.z * CARRY_VELOCITY);
		// Flight direction on the ground (falls back to the bomb's heading when it falls straight down)
		double heading = velocity.horizontalDistanceSqr() > 1.0E-4 ? Math.atan2(velocity.x, velocity.z) : this.getYRot() * Mth.DEG_TO_RAD;
		double alongX = Math.sin(heading);
		double alongZ = Math.cos(heading);
		double spread = impact ? SPREAD_IMPACT : SPREAD_AIR;
		for (int i = 0; i < BOMBLET_COUNT; i++) {
			// Uniformly distributed in an ellipse that is stretched along the flight direction
			double angle = random.nextDouble() * Math.PI * 2.0;
			double radius = Math.sqrt(random.nextDouble()) * spread;
			double along = Math.cos(angle) * radius * SPREAD_ALONG_FLIGHT;
			double across = Math.sin(angle) * radius;
			Vec3 bombletVelocity = carried.add(
					along * alongX + across * alongZ,
					(random.nextDouble() - 0.5) * 0.2 + (impact ? random.nextDouble() * 0.3 : 0.0),
					along * alongZ - across * alongX);
			BombletEntity bomblet = new BombletEntity(level,
					pos.x + (random.nextDouble() - 0.5) * 0.6,
					pos.y + (random.nextDouble() - 0.5) * 0.6,
					pos.z + (random.nextDouble() - 0.5) * 0.6,
					bombletVelocity);
			level.addFreshEntity(bomblet);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		setLaunched(input.getBooleanOr("launched", false));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putBoolean("launched", isLaunched());
	}
}
