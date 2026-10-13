package explosivearrows;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExplosiveArrows implements ModInitializer {
	public static final String MOD_ID = "explosivearrows";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModComponents.init();
		ModEntities.init();
		ModItems.init();
		DelayedActions.register();
		LauncherPayloads.register();
		LOGGER.info("Explosive Arrows geladen");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
