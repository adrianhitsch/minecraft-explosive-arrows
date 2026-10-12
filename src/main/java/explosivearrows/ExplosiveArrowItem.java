package explosivearrows;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ExplosiveArrowItem extends ArrowItem {
	private final ExplosiveArrowType arrowType;

	public ExplosiveArrowItem(ExplosiveArrowType arrowType, Properties properties) {
		super(properties);
		this.arrowType = arrowType;
	}

	public ExplosiveArrowType getArrowType() {
		return arrowType;
	}

	@Override
	public AbstractArrow createArrow(Level level, ItemStack itemStack, LivingEntity owner, @Nullable ItemStack firedFromWeapon) {
		return new ExplosiveArrowEntity(level, owner, itemStack.copyWithCount(1), firedFromWeapon, arrowType);
	}

	@Override
	public Projectile asProjectile(Level level, Position position, ItemStack itemStack, Direction direction) {
		ExplosiveArrowEntity arrow = new ExplosiveArrowEntity(level, position.x(), position.y(), position.z(), itemStack.copyWithCount(1), null, arrowType);
		arrow.pickup = AbstractArrow.Pickup.ALLOWED;
		return arrow;
	}
}
