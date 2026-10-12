package explosivearrows;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> EXPLOSIVE_ARROW_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, ExplosiveArrows.id("explosive_arrow"));

	// Same values as the vanilla spectral arrow
	public static final EntityType<ExplosiveArrowEntity> EXPLOSIVE_ARROW = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			EXPLOSIVE_ARROW_KEY,
			EntityType.Builder.<ExplosiveArrowEntity>of(ExplosiveArrowEntity::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.5F, 0.5F)
					.eyeHeight(0.13F)
					.clientTrackingRange(4)
					.updateInterval(20)
					.build(EXPLOSIVE_ARROW_KEY)
	);

	private ModEntities() {
	}

	static void init() {
	}
}
