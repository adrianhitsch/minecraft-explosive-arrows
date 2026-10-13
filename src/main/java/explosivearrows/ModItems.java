package explosivearrows;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.DispenserBlock;

public final class ModItems {
	public static final Item SMALL_EXPLOSIVE_ARROW = register("small_explosive_arrow", ExplosiveArrowType.SMALL, new Item.Properties());
	public static final Item EXPLOSIVE_ARROW = register("explosive_arrow", ExplosiveArrowType.NORMAL, new Item.Properties());
	public static final Item GOD_ARROW = register("god_arrow", ExplosiveArrowType.GOD, new Item.Properties());
	public static final Item DO_NOT_USE_ARROW = register("do_not_use_arrow", ExplosiveArrowType.DOOM,
			new Item.Properties().rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

	public static final Item CBU87 = Registry.register(BuiltInRegistries.ITEM, key("cbu87"),
			new ClusterBombItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).setId(key("cbu87"))));

	public static final Item CBU87_LAUNCHER = Registry.register(BuiltInRegistries.ITEM, key("cbu87_launcher"),
			new ClusterBombLauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).setId(key("cbu87_launcher"))));

	private ModItems() {
	}

	private static ResourceKey<Item> key(String path) {
		return ResourceKey.create(Registries.ITEM, ExplosiveArrows.id(path));
	}

	private static Item register(String path, ExplosiveArrowType type, Item.Properties properties) {
		ResourceKey<Item> key = key(path);
		Item item = new ExplosiveArrowItem(type, properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static Item forType(ExplosiveArrowType type) {
		return switch (type) {
			case SMALL -> SMALL_EXPLOSIVE_ARROW;
			case NORMAL -> EXPLOSIVE_ARROW;
			case GOD -> GOD_ARROW;
			case DOOM -> DO_NOT_USE_ARROW;
		};
	}

	static void init() {
		for (ExplosiveArrowType type : ExplosiveArrowType.values()) {
			DispenserBlock.registerProjectileBehavior(forType(type));
		}
		DispenserBlock.registerBehavior(CBU87, new ClusterBombItem.DispenseBehavior());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output ->
				output.insertAfter(Items.SPECTRAL_ARROW, SMALL_EXPLOSIVE_ARROW, EXPLOSIVE_ARROW, GOD_ARROW, DO_NOT_USE_ARROW, CBU87, CBU87_LAUNCHER));
	}
}
