package explosivearrows;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * BLU-97 bomblet from a {@link ClusterBombEntity}. Falls on its decelerator (ballute) and explodes on impact.
 * A few are duds: they stay on the ground (clearly visible) and go off when something living steps on them,
 * when they are hit (punch, arrow) – like a mine.
 */
public class BombletEntity extends Entity {
	public static final float EXPLOSION_POWER = 2.2F;
	/** Chance that the zirconium incendiary ring sets fire (only with mob_griefing). */
	public static final float INCENDIARY_CHANCE = 0.35F;
	public static final float DUD_CHANCE = 0.08F;
	/** Ticks between triggering a dud (click) and the explosion. */
	public static final int DUD_FUSE_TICKS = 6;
	private static final double GRAVITY = 0.04;
	/** The ballute brakes strongly: low terminal speed, sideways drift dies off quickly. */
	private static final double DRAG_HORIZONTAL = 0.92;
	private static final double DRAG_VERTICAL = 0.94;

	private static final EntityDataAccessor<Boolean> DATA_LANDED = SynchedEntityData.defineId(BombletEntity.class, EntityDataSerializers.BOOLEAN);

	private boolean dud;
	/** Ticks until a triggered dud explodes, -1 = not triggered. */
	private int fuse = -1;

	public BombletEntity(EntityType<? extends BombletEntity> type, Level level) {
		super(type, level);
	}

	public BombletEntity(Level level, double x, double y, double z, Vec3 velocity) {
		this(ModEntities.BOMBLET, level);
		this.setPos(x, y, z);
		this.xo = x;
		this.yo = y;
		this.zo = z;
		this.setDeltaMovement(velocity);
		this.dud = level.getRandom().nextFloat() < DUD_CHANCE;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		entityData.define(DATA_LANDED, false);
	}

	/** True for a dud lying on the ground. */
	public boolean isLanded() {
		return this.entityData.get(DATA_LANDED);
	}

	public boolean isDud() {
		return dud;
	}

	public void setDud(boolean dud) {
		this.dud = dud;
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
		if (isLanded()) {
			tickLanded();
		} else {
			tickFalling();
		}
	}

	private void tickFalling() {
		this.applyGravity();
		if (this.level() instanceof ServerLevel level) {
			AABB path = this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(0.1);
			if (!level.getEntitiesOfClass(LivingEntity.class, path, BombletEntity::canTrigger).isEmpty()) {
				explode(level);
				return;
			}
		}
		this.move(MoverType.SELF, this.getDeltaMovement());
		Vec3 v = this.getDeltaMovement();
		this.setDeltaMovement(v.x * DRAG_HORIZONTAL, v.y * DRAG_VERTICAL, v.z * DRAG_HORIZONTAL);
		if (this.level() instanceof ServerLevel level
				&& (this.onGround() || this.horizontalCollision || this.verticalCollision || this.isInWater() || this.isInLava())) {
			if (dud && !this.isInLava()) {
				this.entityData.set(DATA_LANDED, true);
				this.setDeltaMovement(Vec3.ZERO);
			} else {
				explode(level);
			}
		}
	}

	/** Dud on the ground: lies still (falls if the ground disappears) and waits for a victim. */
	private void tickLanded() {
		this.applyGravity();
		this.move(MoverType.SELF, this.getDeltaMovement());
		this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.98, 0.5));
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		if (fuse > 0) {
			if (--fuse == 0) {
				explode(level);
			}
			return;
		}
		AABB touch = this.getBoundingBox().inflate(0.05, 0.2, 0.05);
		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, touch, BombletEntity::canTrigger);
		if (!victims.isEmpty() || this.isInLava()) {
			trigger(level);
		}
	}

	private static boolean canTrigger(LivingEntity entity) {
		return entity.isAlive() && !entity.isSpectator();
	}

	/** Arms a dud: a short click, then the explosion. */
	void trigger(ServerLevel level) {
		if (fuse >= 0) {
			return;
		}
		fuse = DUD_FUSE_TICKS;
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.6F);
		level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.2, this.getZ(), 5, 0.05, 0.05, 0.05, 0.01);
	}

	private void explode(ServerLevel level) {
		this.discard();
		boolean fire = level.getRandom().nextFloat() < INCENDIARY_CHANCE;
		ExplosiveArrowEntity.explode(level, this.getX(), this.getY() + 0.1, this.getZ(), EXPLOSION_POWER, fire);
	}

	@Override
	public boolean isPickable() {
		// Lets arrows and punches hit a dud
		return isLanded() && !this.isRemoved();
	}

	@Override
	public boolean ignoreExplosion(Explosion explosion) {
		// Neighbouring explosions neither push nor set off duds, so they stay on the field as mines
		return true;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (!isLanded() || source.is(DamageTypeTags.IS_EXPLOSION)) {
			return false;
		}
		trigger(level);
		return true;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.entityData.set(DATA_LANDED, input.getBooleanOr("landed", false));
		this.dud = input.getBooleanOr("dud", false);
		this.fuse = input.getIntOr("fuse", -1);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putBoolean("landed", isLanded());
		output.putBoolean("dud", dud);
		output.putInt("fuse", fuse);
	}
}
