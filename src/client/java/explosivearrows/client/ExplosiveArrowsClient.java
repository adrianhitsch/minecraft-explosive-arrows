package explosivearrows.client;

import explosivearrows.ExplosiveArrows;
import explosivearrows.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.special.SpecialModelRenderers;

public class ExplosiveArrowsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.EXPLOSIVE_ARROW, ExplosiveArrowRenderer::new);
		EntityRendererRegistry.register(ModEntities.CLUSTER_BOMB, ClusterBombRenderer::new);
		EntityRendererRegistry.register(ModEntities.PLACED_CLUSTER_BOMB, ClusterBombRenderer::new);
		EntityRendererRegistry.register(ModEntities.BOMBLET, BombletRenderer::new);
		// Item model type "explosivearrows:cbu87" (Fabric API has no registry for special item models)
		SpecialModelRenderers.ID_MAPPER.put(ExplosiveArrows.id("cbu87"), ClusterBombSpecialRenderer.Unbaked.MAP_CODEC);
		SpecialModelRenderers.ID_MAPPER.put(ExplosiveArrows.id("cbu87_launcher"), LauncherSpecialRenderer.Unbaked.MAP_CODEC);
		LauncherControls.register();
	}
}
