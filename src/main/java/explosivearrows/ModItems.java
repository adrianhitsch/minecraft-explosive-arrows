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

	private ModItems() {
	}

	private static Item register(String path, ExplosiveArrowType type, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ExplosiveArrows.id(path));
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
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output ->
				output.insertAfter(Items.SPECTRAL_ARROW, SMALL_EXPLOSIVE_ARROW, EXPLOSIVE_ARROW, GOD_ARROW, DO_NOT_USE_ARROW));
	}
}
