package explosivearrows;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Inert CBU-87 lying on the ground, just to look at. A hit picks it up again (in survival as an item). */
public class PlacedClusterBombEntity extends Entity {
	public PlacedClusterBombEntity(EntityType<? extends PlacedClusterBombEntity> type, Level level) {
		super(type, level);
	}

	public PlacedClusterBombEntity(Level level, double x, double y, double z, float yaw) {
		this(ModEntities.PLACED_CLUSTER_BOMB, level);
		this.setPos(x, y, z);
		this.setYRot(yaw);
		this.yRotO = yaw;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
	}

	@Override
	protected double getDefaultGravity() {
		return 0.04;
	}

	@Override
	public void tick() {
		super.tick();
		this.applyGravity();
		this.move(MoverType.SELF, this.getDeltaMovement());
		this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.98, 0.5));
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public boolean ignoreExplosion(Explosion explosion) {
		return true;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (this.isRemoved() || source.is(DamageTypeTags.IS_EXPLOSION) || !(source.getEntity() instanceof Player player)) {
			return false;
		}
		if (!player.getAbilities().instabuild) {
			this.spawnAtLocation(level, new ItemStack(ModItems.CBU87));
		}
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.IRON_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
		this.discard();
		return true;
	}

	@Override
	public @Nullable ItemStack getPickResult() {
		return new ItemStack(ModItems.CBU87);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
