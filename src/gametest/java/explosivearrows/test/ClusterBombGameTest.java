package explosivearrows.test;

import java.util.List;

import explosivearrows.BombletEntity;
import explosivearrows.ClusterBombEntity;
import explosivearrows.ClusterBombLauncherItem;
import explosivearrows.ModItems;
import explosivearrows.PlacedClusterBombEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * GameTests for the CBU-87 cluster bomb. Test area: 8x8x8, stone floor at y=0 and y=1 (FLOOR_Y).
 * Bomblets are created with {@code setDud} explicitly, so the random dud chance does not make tests flaky.
 */
public class ClusterBombGameTest {
	private static final int FLOOR_Y = 1;
	private static final int FLOOR_BLOCKS = 64;

	@GameTest
	public void clusterBombIsRegistered(GameTestHelper helper) {
		Identifier id = Identifier.fromNamespaceAndPath("explosivearrows", "cbu87");
		helper.assertTrue(BuiltInRegistries.ITEM.getValue(id) == ModItems.CBU87, "Item not registered as " + id);
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
		helper.assertTrue(helper.getLevel().recipeAccess().byKey(key).isPresent(), "Recipe missing: " + id);
		helper.succeed();
	}

	@GameTest(maxTicks = 100, padding = 32)
	public void bombOpensAboveGroundAndReleasesBomblets(GameTestHelper helper) {
		buildFloor(helper);
		Vec3 pos = helper.absoluteVec(new Vec3(3.5, 30.0, 3.5));
		helper.getLevel().addFreshEntity(new ClusterBombEntity(helper.getLevel(), pos.x, pos.y, pos.z, Vec3.ZERO, 0.0F));
		double floorTop = helper.absoluteVec(new Vec3(0, FLOOR_Y + 1, 0)).y;
		helper.succeedWhen(() -> {
			helper.assertTrue(entities(helper, ClusterBombEntity.class, 30).isEmpty(), "Bomb has not opened yet");
			List<BombletEntity> bomblets = entities(helper, BombletEntity.class, 30);
			helper.assertTrue(bomblets.size() == ClusterBombEntity.BOMBLET_COUNT,
					"Expected " + ClusterBombEntity.BOMBLET_COUNT + " bomblets, found " + bomblets.size());
			for (BombletEntity bomblet : bomblets) {
				helper.assertTrue(bomblet.getY() > floorTop + 10, "Bomb did not open high above the ground");
			}
			// Remove them before they land in neighbouring tests
			bomblets.forEach(Entity::discard);
		});
	}

	@GameTest(maxTicks = 100, padding = 16)
	public void bombletExplodesOnImpact(GameTestHelper helper) {
		buildFloor(helper);
		spawnBomblet(helper, 5.0, false);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(countFloor(helper) < FLOOR_BLOCKS, "Bomblet did not destroy any block");
			helper.assertTrue(entities(helper, BombletEntity.class, 2).isEmpty(), "Bomblet still there after impact");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 16, environment = "explosivearrows-test:no_mob_griefing")
	public void bombletRespectsMobGriefingRule(GameTestHelper helper) {
		buildFloor(helper);
		spawnBomblet(helper, 5.0, false);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(countFloor(helper) == FLOOR_BLOCKS, "No blocks may be destroyed with mob_griefing=false");
			helper.assertTrue(entities(helper, BombletEntity.class, 2).isEmpty(), "Bomblet still there after impact");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 120, padding = 16)
	public void dudLiesStillUntilSteppedOn(GameTestHelper helper) {
		buildFloor(helper);
		BombletEntity dud = spawnBomblet(helper, 3.0, true);
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(dud.isAlive() && dud.isLanded(), "Dud should lie on the ground");
			helper.assertTrue(countFloor(helper) == FLOOR_BLOCKS, "Dud must not explode on its own");
			Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, 3.5f, 2.0f, 3.5f);
			helper.runAfterDelay(20, () -> {
				helper.assertTrue(!dud.isAlive(), "Dud did not go off when a mob stepped on it");
				helper.assertTrue(!pig.isAlive(), "Pig survived the dud");
				helper.succeed();
			});
		});
	}

	@GameTest(maxTicks = 100, padding = 16)
	public void dispenserDropsClusterBomb(GameTestHelper helper) {
		buildFloor(helper);
		BlockPos dispenserPos = new BlockPos(3, 7, 3);
		helper.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.DOWN));
		DispenserBlockEntity dispenser = helper.getBlockEntity(dispenserPos, DispenserBlockEntity.class);
		dispenser.setItem(0, new ItemStack(ModItems.CBU87));
		helper.pulseRedstone(new BlockPos(4, 7, 3), 2);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(dispenser.isEmpty(), "Dispenser did not drop the bomb");
			helper.assertTrue(countFloor(helper) < FLOOR_BLOCKS, "Dropped bomb did not blast the floor");
			// Duds may remain
			entities(helper, BombletEntity.class, 8).forEach(Entity::discard);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 16)
	public void placedBombIsInert(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		BlockPos floor = helper.absolutePos(new BlockPos(3, FLOOR_Y, 3));
		Vec3 click = Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0);
		ItemStack stack = new ItemStack(ModItems.CBU87);
		stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(click, Direction.UP, floor, false)));
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(entities(helper, PlacedClusterBombEntity.class, 2).size() == 1, "Placed bomb missing");
			helper.assertTrue(entities(helper, BombletEntity.class, 2).isEmpty() && countFloor(helper) == FLOOR_BLOCKS,
					"Placed bomb must not go off");
			helper.succeed();
		});
	}

	@GameTest
	public void launcherIsRegistered(GameTestHelper helper) {
		Identifier id = Identifier.fromNamespaceAndPath("explosivearrows", "cbu87_launcher");
		helper.assertTrue(BuiltInRegistries.ITEM.getValue(id) == ModItems.CBU87_LAUNCHER, "Item not registered as " + id);
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
		helper.assertTrue(helper.getLevel().recipeAccess().byKey(key).isPresent(), "Recipe missing: " + id);
		helper.succeed();
	}

	@GameTest(maxTicks = 120, padding = 16)
	public void launcherFiresOnlyAfterReload(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(3.5, 2.0, 3.5)));
		ItemStack launcher = new ItemStack(ModItems.CBU87_LAUNCHER);
		player.setItemInHand(InteractionHand.MAIN_HAND, launcher);
		ItemStack ammo = new ItemStack(ModItems.CBU87, 2);
		player.getInventory().setItem(5, ammo);
		helper.assertTrue(!ClusterBombLauncherItem.fire(player, launcher), "Empty launcher must not fire");
		helper.assertTrue(ClusterBombLauncherItem.startReload(player, launcher), "Reload did not start");
		helper.assertTrue(!ClusterBombLauncherItem.isLoaded(launcher) && ammo.getCount() == 2, "Reload must take time");
		helper.runAfterDelay(ClusterBombLauncherItem.RELOAD_TICKS + 2, () -> {
			helper.assertTrue(ClusterBombLauncherItem.isLoaded(launcher), "Launcher not loaded after the reload time");
			helper.assertTrue(ammo.getCount() == 1, "Reload did not take a bomb from the inventory");
			helper.assertTrue(ClusterBombLauncherItem.fire(player, launcher), "Loaded launcher did not fire");
			List<ClusterBombEntity> bombs = entities(helper, ClusterBombEntity.class, 4);
			helper.assertTrue(bombs.size() == 1 && bombs.getFirst().isLaunched(), "Launcher did not fire a launched bomb");
			helper.assertTrue(!ClusterBombLauncherItem.isLoaded(launcher), "Launcher still loaded after firing");
			bombs.forEach(Entity::discard);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40, padding = 16)
	public void launcherWithoutAmmoCannotReload(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(3.5, 2.0, 3.5)));
		ItemStack launcher = new ItemStack(ModItems.CBU87_LAUNCHER);
		player.setItemInHand(InteractionHand.MAIN_HAND, launcher);
		helper.assertTrue(!ClusterBombLauncherItem.startReload(player, launcher), "Reload started without ammunition");
		helper.assertTrue(!ClusterBombLauncherItem.fire(player, launcher), "Launcher fired without ammunition");
		helper.assertTrue(entities(helper, ClusterBombEntity.class, 4).isEmpty(), "Launcher fired without ammunition");
		helper.succeed();
	}

	@GameTest(maxTicks = 60, padding = 32)
	public void launchedBombOpensBeforeReachingTarget(GameTestHelper helper) {
		buildFloor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, 3.5f, 2.0f, 3.5f);
		// Flies towards the pig high enough not to hit the floor before passing it (only the proximity fuze can open it)
		Vec3 start = helper.absoluteVec(new Vec3(3.5, 4.5, -4.5));
		ClusterBombEntity bomb = new ClusterBombEntity(helper.getLevel(), start.x, start.y, start.z, new Vec3(0.0, 0.0, 1.2), 0.0F, 0.0F);
		bomb.setLaunched(true);
		helper.getLevel().addFreshEntity(bomb);
		helper.succeedWhen(() -> {
			List<BombletEntity> bomblets = entities(helper, BombletEntity.class, 30);
			helper.assertTrue(!bomb.isAlive() && !bomblets.isEmpty(), "Launched bomb has not opened yet");
			for (BombletEntity bomblet : bomblets) {
				helper.assertTrue(bomblet.getZ() < pig.getZ() + 1.0, "Bomb opened only after passing the target");
			}
			bomblets.forEach(Entity::discard);
		});
	}

	private static BombletEntity spawnBomblet(GameTestHelper helper, double y, boolean dud) {
		ServerLevel level = helper.getLevel();
		Vec3 pos = helper.absoluteVec(new Vec3(3.5, y, 3.5));
		BombletEntity bomblet = new BombletEntity(level, pos.x, pos.y, pos.z, Vec3.ZERO);
		bomblet.setDud(dud);
		level.addFreshEntity(bomblet);
		return bomblet;
	}

	/** Entities of the type around the test area (larger margin only for tests with matching padding). */
	private static <T extends Entity> List<T> entities(GameTestHelper helper, Class<T> type, int margin) {
		AABB area = new AABB(helper.absoluteVec(new Vec3(-margin, -10, -margin)), helper.absoluteVec(new Vec3(8 + margin, 40, 8 + margin)));
		return helper.getLevel().getEntitiesOfClass(type, area);
	}

	private static void buildFloor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
				helper.setBlock(x, FLOOR_Y, z, Blocks.STONE);
			}
		}
	}

	private static int countFloor(GameTestHelper helper) {
		int count = 0;
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				if (helper.getBlockState(new BlockPos(x, FLOOR_Y, z)).is(Blocks.STONE)) {
					count++;
				}
			}
		}
		return count;
	}
}
