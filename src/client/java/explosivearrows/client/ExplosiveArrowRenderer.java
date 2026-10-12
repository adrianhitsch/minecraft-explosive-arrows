package explosivearrows.client;

import explosivearrows.ExplosiveArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

/** Renders the explosive arrows like regular arrows. */
public class ExplosiveArrowRenderer extends ArrowRenderer<ExplosiveArrowEntity, ArrowRenderState> {
	public ExplosiveArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected Identifier getTextureLocation(ArrowRenderState state) {
		return TippableArrowRenderer.NORMAL_ARROW_LOCATION;
	}

	@Override
	public ArrowRenderState createRenderState() {
		return new ArrowRenderState();
	}
}
