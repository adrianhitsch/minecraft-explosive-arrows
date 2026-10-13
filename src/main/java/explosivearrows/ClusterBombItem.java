package explosivearrows;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * CBU-87 cluster bomb. Right-click into the air drops it (it keeps the player's speed, ideal with elytra),
 * right-click on a block places an inert one to look at.
 */
public class ClusterBombItem extends Item {
	public ClusterBombItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel) {
			// Below the feet when flying, otherwise it would get stuck in the ground
			double y = player.onGround() ? player.getY() + 0.1 : player.getY() - 0.6;
			// Player yaw: direction = (-sin, cos) -> bomb convention atan2(x, z)
			ClusterBombEntity bomb = new ClusterBombEntity(level, player.getX(), y, player.getZ(), player.getKnownMovement(), -player.getYRot());
			serverLevel.addFreshEntity(bomb);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1.0F, 0.7F);
			level.gameEvent(player, GameEvent.PROJECTILE_SHOOT, player.position());
		}
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Direction face = context.getClickedFace();
		Vec3 pos;
		if (face == Direction.UP) {
			pos = context.getClickLocation();
		} else {
			BlockPos target = context.getClickedPos().relative(face);
			pos = new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
		}
		Player player = context.getPlayer();
		float yaw = player != null ? -player.getYRot() : 0.0F;
		PlacedClusterBombEntity placed = new PlacedClusterBombEntity(level, pos.x, pos.y, pos.z, yaw);
		if (!level.noCollision(placed, placed.getBoundingBox())) {
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel) {
			level.addFreshEntity(placed);
			level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.IRON_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
			level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
		}
		context.getItemInHand().consume(1, player);
		return InteractionResult.SUCCESS;
	}

	/** Dispenser: drops the bomb out of the front side; it falls and opens like one dropped by a player. */
	static final class DispenseBehavior extends DefaultDispenseItemBehavior {
		@Override
		protected ItemStack execute(BlockSource source, ItemStack dispensed) {
			Direction facing = source.state().getValue(DispenserBlock.FACING);
			Vec3 center = source.center();
			Vec3 step = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
			// Bounding box (0.5) completely outside the dispenser
			Vec3 pos = center.add(step.scale(0.85)).subtract(0.0, 0.25, 0.0);
			// Direction.toYRot uses the mob convention (-sin, cos), the bomb atan2(x, z)
			float yaw = facing.getAxis().isHorizontal() ? -facing.toYRot() : 0.0F;
			ClusterBombEntity bomb = new ClusterBombEntity(source.level(), pos.x, pos.y, pos.z, step.scale(0.3), yaw);
			source.level().addFreshEntity(bomb);
			dispensed.shrink(1);
			return dispensed;
		}

		@Override
		protected void playSound(BlockSource source) {
			source.level().playSound(null, source.pos(), SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 0.6F);
		}
	}
}
