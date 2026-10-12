package explosivearrows.client;

import explosivearrows.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ExplosiveArrowsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.EXPLOSIVE_ARROW, ExplosiveArrowRenderer::new);
	}
}
