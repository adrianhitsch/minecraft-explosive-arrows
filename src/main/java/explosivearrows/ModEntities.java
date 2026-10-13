package explosivearrows;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
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

	public static final EntityType<ClusterBombEntity> CLUSTER_BOMB = register("cbu87",
			EntityType.Builder.<ClusterBombEntity>of(ClusterBombEntity::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.5F, 0.5F)
					.clientTrackingRange(10)
					.updateInterval(5));

	public static final EntityType<PlacedClusterBombEntity> PLACED_CLUSTER_BOMB = register("placed_cbu87",
			EntityType.Builder.<PlacedClusterBombEntity>of(PlacedClusterBombEntity::new, MobCategory.MISC)
					.noLootTable()
					.sized(1.0F, 0.6F)
					.clientTrackingRange(10)
					.updateInterval(20));

	public static final EntityType<BombletEntity> BOMBLET = register("blu97",
			EntityType.Builder.<BombletEntity>of(BombletEntity::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.25F, 0.25F)
					.clientTrackingRange(8)
					.updateInterval(5));

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ExplosiveArrows.id(path));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	static void init() {
	}
}
