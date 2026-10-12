package explosivearrows;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Arrow that explodes on impact (block or entity) and disappears. */
public class ExplosiveArrowEntity extends AbstractArrow {
	private ExplosiveArrowType arrowType = ExplosiveArrowType.SMALL;
	private boolean exploded;

	public ExplosiveArrowEntity(EntityType<? extends ExplosiveArrowEntity> type, Level level) {
		super(type, level);
	}

	public ExplosiveArrowEntity(Level level, LivingEntity owner, ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon, ExplosiveArrowType arrowType) {
		super(ModEntities.EXPLOSIVE_ARROW, owner, level, pickupItemStack, firedFromWeapon);
		this.arrowType = arrowType;
	}

	public ExplosiveArrowEntity(Level level, double x, double y, double z, ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon, ExplosiveArrowType arrowType) {
		super(ModEntities.EXPLOSIVE_ARROW, x, y, z, level, pickupItemStack, firedFromWeapon);
		this.arrowType = arrowType;
	}

	public ExplosiveArrowType getArrowType() {
		return arrowType;
	}

	@Override
	protected void onHit(HitResult hitResult) {
		// Remember before super: onHitBlock zeroes the movement
		Vec3 direction = this.getDeltaMovement();
		// super may already discard the arrow (mob hit), so use our own flag instead of isRemoved()
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel level && !exploded) {
			exploded = true;
			switch (arrowType) {
				case SMALL, NORMAL -> explode(level, this.getX(), this.getY(), this.getZ(), arrowType.explosionPower(), arrowType.createsFire());
				case GOD -> {
					explode(level, this.getX(), this.getY(), this.getZ(), arrowType.explosionPower(), arrowType.createsFire());
					GodStrike.trigger(level, this.position(), direction);
				}
				// Only explodes after the countdown
				case DOOM -> DoomStrike.trigger(level, this.position());
			}
			this.discard();
		}
	}

	/** Explosion that respects {@code mob_griefing} (blocks via ExplosionInteraction.MOB, fire only with griefing). */
	static void explode(ServerLevel level, double x, double y, double z, float power, boolean fire) {
		boolean griefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
		level.explode(null, x, y, z, power, fire && griefing, Level.ExplosionInteraction.MOB);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		String name = input.getStringOr("ExplosiveType", ExplosiveArrowType.SMALL.name());
		try {
			this.arrowType = ExplosiveArrowType.valueOf(name);
		} catch (IllegalArgumentException e) {
			this.arrowType = ExplosiveArrowType.SMALL;
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("ExplosiveType", arrowType.name());
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		// Already called from the AbstractArrow constructor, before arrowType is set
		return new ItemStack(ModItems.forType(arrowType != null ? arrowType : ExplosiveArrowType.SMALL));
	}
}
