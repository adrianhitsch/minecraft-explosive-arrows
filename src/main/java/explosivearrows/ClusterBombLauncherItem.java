package explosivearrows;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Shoulder-fired launcher for the CBU-87.
 * <ul>
 * <li>Right mouse button (hold): look through the scope (see the scope mixins).</li>
 * <li>Left mouse button: fires the loaded bomb ({@link LauncherPayloads.Fire}) along the view direction.</li>
 * <li>Reload key: loads a {@link ModItems#CBU87} from the inventory ({@link LauncherPayloads.Reload}); takes
 * {@link #RELOAD_TICKS}, animated on the client from {@link ModComponents#LAUNCHER_RELOAD_START}.</li>
 * </ul>
 * Fired bombs have a rocket motor and a proximity fuze (see {@link ClusterBombEntity#setLaunched}).
 * Recoil pushes the shooter back; the backblast burns and hurts whoever stands behind.
 */
public class ClusterBombLauncherItem extends Item {
	public static final double LAUNCH_SPEED = 3.0;
	public static final int RELOAD_TICKS = 50;
	/** Recoil impulse, applied by the client (it controls the player's movement). */
	public static final double RECOIL = 0.6;
	/** Length of the danger zone behind the shooter (blocks). */
	public static final double BACKBLAST_RANGE = 5.0;
	public static final float BACKBLAST_DAMAGE = 6.0F;

	public ClusterBombLauncherItem(Properties properties) {
		super(properties);
	}

	public static boolean isLoaded(ItemStack stack) {
		return stack.getOrDefault(ModComponents.LAUNCHER_LOADED, false);
	}

	/** Reload progress 0..1, or -1 if no reload is running (a stale start, e.g. after a restart, counts as none). */
	public static float reloadProgress(ItemStack stack, float gameTime) {
		Long start = stack.get(ModComponents.LAUNCHER_RELOAD_START);
		if (start == null) {
			return -1.0F;
		}
		float progress = (gameTime - start) / RELOAD_TICKS;
		return progress < 0.0F || progress > 1.0F ? -1.0F : progress;
	}

	// --- Scope: hold right click ---

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		player.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 0.8F);
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.SPYGLASS;
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
		entity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0F, 0.8F);
		return true;
	}

	@Override
	public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
		// Loading/firing changes the components; the launcher should not bob out of the hand each time
		return false;
	}

	// --- Fire: left click ---

	/** Fires the loaded bomb. Returns whether a bomb was fired. */
	public static boolean fire(Player player, ItemStack launcher) {
		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}
		if (!isLoaded(launcher) || reloadProgress(launcher, level.getGameTime()) >= 0.0F) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.2F);
			return false;
		}
		Vec3 look = player.getLookAngle();
		Vec3 muzzle = player.getEyePosition().add(look.scale(1.5)).subtract(0.0, 0.25, 0.0);
		Vec3 velocity = look.scale(LAUNCH_SPEED).add(player.getKnownMovement());
		float yaw = (float) (Mth.atan2(look.x, look.z) * Mth.RAD_TO_DEG);
		ClusterBombEntity bomb = new ClusterBombEntity(level, muzzle.x, muzzle.y - 0.25, muzzle.z, velocity, yaw, -player.getXRot());
		bomb.setLaunched(true);
		level.addFreshEntity(bomb);
		launcher.set(ModComponents.LAUNCHER_LOADED, false);
		fireEffects(level, player, look, muzzle);
		level.gameEvent(player, GameEvent.PROJECTILE_SHOOT, player.position());
		return true;
	}

	private static void fireEffects(ServerLevel level, Player player, Vec3 look, Vec3 muzzle) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 1.9F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 3.0F, 0.5F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 8, 0.2, 0.2, 0.2, 0.02);
		// Backblast: smoke and flames out of the rear of the tube, burns everything right behind the shooter
		Vec3 rear = player.getEyePosition().subtract(look.scale(1.2)).subtract(0.0, 0.25, 0.0);
		for (int i = 0; i < 6; i++) {
			Vec3 p = rear.subtract(look.scale(i * 0.7));
			level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 4, 0.15 + i * 0.08, 0.15 + i * 0.08, 0.15 + i * 0.08, 0.02);
			level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 3, 0.2 + i * 0.1, 0.2 + i * 0.1, 0.2 + i * 0.1, 0.02);
		}
		Vec3 end = rear.subtract(look.scale(BACKBLAST_RANGE));
		AABB zone = new AABB(rear, end).inflate(1.0);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, zone, e -> e != player && e.isAlive())) {
			// Only inside a cone behind the tube
			Vec3 toVictim = victim.getBoundingBox().getCenter().subtract(rear);
			double behind = -toVictim.dot(look);
			if (behind > 0.0 && behind < BACKBLAST_RANGE && toVictim.add(look.scale(behind)).length() < 0.8 + behind * 0.3) {
				victim.hurtServer(level, level.damageSources().inFire(), BACKBLAST_DAMAGE);
				victim.igniteForTicks(80);
				victim.push(look.scale(-0.8));
			}
		}
	}

	// --- Reload: reload key ---

	/** Starts loading a bomb from the inventory; it is taken and loaded when the animation is finished. */
	public static boolean startReload(Player player, ItemStack launcher) {
		if (!(player.level() instanceof ServerLevel level) || isLoaded(launcher) || reloadProgress(launcher, level.getGameTime()) >= 0.0F) {
			return false;
		}
		if (findAmmo(player) == null && !player.hasInfiniteMaterials()) {
			player.sendOverlayMessage(Component.translatable("message.explosivearrows.no_cbu87"));
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.2F);
			return false;
		}
		long start = level.getGameTime();
		launcher.set(ModComponents.LAUNCHER_RELOAD_START, start);
		// Sounds matching the animation: swing out, bomb slides in, latch, swing back
		playAt(level, player, 0, SoundEvents.CROSSBOW_LOADING_START.value(), 0.6F);
		playAt(level, player, RELOAD_TICKS * 30 / 100, SoundEvents.PISTON_CONTRACT, 0.7F);
		playAt(level, player, RELOAD_TICKS * 78 / 100, SoundEvents.IRON_DOOR_CLOSE, 1.4F);
		playAt(level, player, RELOAD_TICKS * 82 / 100, SoundEvents.CROSSBOW_LOADING_END.value(), 0.7F);
		DelayedActions.schedule(level, RELOAD_TICKS, l -> finishReload(player, launcher, start));
		return true;
	}

	private static void playAt(ServerLevel level, Player player, int delay, SoundEvent sound, float pitch) {
		DelayedActions.schedule(level, delay, l -> {
			if (player.isAlive()) {
				l.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, pitch);
			}
		});
	}

	private static void finishReload(Player player, ItemStack launcher, long start) {
		Long current = launcher.get(ModComponents.LAUNCHER_RELOAD_START);
		if (current == null || current != start) {
			return;
		}
		launcher.remove(ModComponents.LAUNCHER_RELOAD_START);
		// Put away in the meantime: reload aborted
		if (!player.isAlive() || player.getMainHandItem() != launcher) {
			return;
		}
		ItemStack ammo = findAmmo(player);
		if (ammo == null && !player.hasInfiniteMaterials()) {
			return;
		}
		if (ammo != null && !player.hasInfiniteMaterials()) {
			ammo.shrink(1);
		}
		launcher.set(ModComponents.LAUNCHER_LOADED, true);
	}

	/** Offhand first, then the inventory. */
	private static @Nullable ItemStack findAmmo(Player player) {
		if (player.getOffhandItem().is(ModItems.CBU87)) {
			return player.getOffhandItem();
		}
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(ModItems.CBU87)) {
				return stack;
			}
		}
		return null;
	}
}
