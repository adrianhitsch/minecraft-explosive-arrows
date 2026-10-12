package explosivearrows.test;

import explosivearrows.ExplosiveArrowType;
import explosivearrows.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server GameTests for Explosive Arrows. Run automatically with {@code gradlew build}.
 * Test area: 8x8x8, stone floor at y=0 and y=1 (FLOOR_Y).
 */
public class ExplosiveArrowsGameTest {
	private static final String MOD_ID = "explosivearrows";
	private static final String[] IDS = {"small_explosive_arrow", "explosive_arrow", "god_arrow"};
	private static final int FLOOR_Y = 1;
	private static final int FLOOR_BLOCKS = 64;

	private static Item[] items() {
		return new Item[] {ModItems.SMALL_EXPLOSIVE_ARROW, ModItems.EXPLOSIVE_ARROW, ModItems.GOD_ARROW};
	}

	@GameTest
	public void itemsAreRegisteredAsArrows(GameTestHelper helper) {
		Item[] items = items();
		for (int i = 0; i < IDS.length; i++) {
			Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, IDS[i]);
			helper.assertTrue(BuiltInRegistries.ITEM.getValue(id) == items[i], "Item not registered as " + id);
			helper.assertTrue(new ItemStack(items[i]).is(ItemTags.ARROWS), id + " missing from tag #minecraft:arrows (otherwise the bow will not accept it)");
			helper.assertTrue(items[i] instanceof ProjectileItem, id + " must be a ProjectileItem (e.g. ArrowItem)");
		}
		helper.succeed();
	}

	@GameTest
	public void recipesExist(GameTestHelper helper) {
		for (String path : IDS) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(MOD_ID, path));
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(key).isPresent(), "Recipe missing: " + key.identifier());
		}
		helper.succeed();
	}

	@GameTest
	public void arrowTypeValues(GameTestHelper helper) {
		assertPower(helper, ExplosiveArrowType.SMALL, 4.0f);
		assertPower(helper, ExplosiveArrowType.NORMAL, 8.6f);
		assertPower(helper, ExplosiveArrowType.GOD, 35.0f);
		helper.assertTrue(!ExplosiveArrowType.SMALL.createsFire(), "SMALL must not create fire");
		helper.assertTrue(!ExplosiveArrowType.NORMAL.createsFire(), "NORMAL must not create fire");
		helper.assertTrue(ExplosiveArrowType.GOD.createsFire(), "GOD must create fire");
		helper.succeed();
	}

	@GameTest
	public void doNotUseArrowIsRegistered(GameTestHelper helper) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "do_not_use_arrow");
		Item item = ModItems.DO_NOT_USE_ARROW;
		helper.assertTrue(BuiltInRegistries.ITEM.getValue(id) == item, "Item not registered as " + id);
		helper.assertTrue(new ItemStack(item).is(ItemTags.ARROWS), id + " missing from tag #minecraft:arrows");
		helper.assertTrue(item instanceof ProjectileItem, id + " must be a ProjectileItem");
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
		helper.assertTrue(helper.getLevel().recipeAccess().byKey(key).isPresent(), "Recipe missing: " + id);
		assertPower(helper, ExplosiveArrowType.DOOM, 80.0f);
		helper.assertTrue(ExplosiveArrowType.DOOM.createsFire(), "DOOM must create fire");
		helper.succeed();
	}

	@GameTest(maxTicks = 100, padding = 16)
	public void smallArrowBlastsCraterIntoStone(GameTestHelper helper) {
		buildFloor(helper);
		shootDown(helper, ModItems.SMALL_EXPLOSIVE_ARROW);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(countFloor(helper) < FLOOR_BLOCKS, "Small explosive arrow did not destroy any block");
			assertCornersIntact(helper);
			assertNoArrowsLeft(helper);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 16, environment = "explosivearrows-test:no_mob_griefing")
	public void respectsMobGriefingRule(GameTestHelper helper) {
		buildFloor(helper);
		shootDown(helper, ModItems.SMALL_EXPLOSIVE_ARROW);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(countFloor(helper) == FLOOR_BLOCKS, "No blocks may be destroyed with mob_griefing=false");
			assertNoArrowsLeft(helper);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 16)
	public void explodesOnEntityHit(GameTestHelper helper) {
		buildFloor(helper);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, 3.5f, 2.0f, 3.5f);
		shootDown(helper, ModItems.SMALL_EXPLOSIVE_ARROW);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(!pig.isAlive(), "Pig survived: arrow does not explode when hitting a mob");
			assertNoArrowsLeft(helper);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 16)
	public void dispenserFiresExplosiveArrow(GameTestHelper helper) {
		buildFloor(helper);
		BlockPos dispenserPos = new BlockPos(3, 6, 3);
		helper.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.DOWN));
		DispenserBlockEntity dispenser = helper.getBlockEntity(dispenserPos, DispenserBlockEntity.class);
		dispenser.setItem(0, new ItemStack(ModItems.SMALL_EXPLOSIVE_ARROW));
		helper.pulseRedstone(new BlockPos(4, 6, 3), 2);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(countFloor(helper) < FLOOR_BLOCKS, "Dispenser does not fire an exploding arrow");
			helper.succeed();
		});
	}

	private static void assertPower(GameTestHelper helper, ExplosiveArrowType type, float expected) {
		helper.assertTrue(Math.abs(type.explosionPower() - expected) < 0.01f,
				type + ": explosion power " + type.explosionPower() + ", expected " + expected);
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

	private static void assertCornersIntact(GameTestHelper helper) {
		helper.assertBlockPresent(Blocks.STONE, 0, FLOOR_Y, 0);
		helper.assertBlockPresent(Blocks.STONE, 7, FLOOR_Y, 0);
		helper.assertBlockPresent(Blocks.STONE, 0, FLOOR_Y, 7);
		helper.assertBlockPresent(Blocks.STONE, 7, FLOOR_Y, 7);
	}

	/** Shoots an arrow of the item straight down onto the center of the floor (block 3/1/3). */
	private static void shootDown(GameTestHelper helper, Item item) {
		ServerLevel level = helper.getLevel();
		Vec3 pos = helper.absoluteVec(new Vec3(3.5, 6.0, 3.5));
		Projectile projectile = ((ProjectileItem) item).asProjectile(level, pos, new ItemStack(item), Direction.DOWN);
		projectile.setDeltaMovement(0.0, -1.5, 0.0);
		level.addFreshEntity(projectile);
	}

	/** No arrow may be left lying around after the explosion (it must disappear on impact). */
	private static void assertNoArrowsLeft(GameTestHelper helper) {
		AABB area = new AABB(helper.absoluteVec(new Vec3(-2, -2, -2)), helper.absoluteVec(new Vec3(10, 10, 10)));
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(AbstractArrow.class, area).isEmpty(),
				"Arrow still lying around after impact (no explosion?)");
	}
}
