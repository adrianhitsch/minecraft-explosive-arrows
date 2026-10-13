package explosivearrows.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import explosivearrows.BombletEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Falling bomblet: nose down under its ballute, swaying slightly. Dud: lying on its side. */
public class BombletRenderer extends EntityRenderer<BombletEntity, BombletRenderer.State> {
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(CbuModels.BOMBLET_TEXTURE);

	public static class State extends EntityRenderState {
		boolean landed;
		/** Fixed per bomblet, the same on server and client. */
		float lyingYaw;
	}

	public BombletRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.12F;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BombletEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.landed = entity.isLanded();
		state.lyingYaw = (entity.getUUID().hashCode() & 0xFFFF) / 65536.0F * 360.0F;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		if (state.landed) {
			poseStack.translate(0.0F, CbuModels.BOMBLET_RADIUS, 0.0F);
			poseStack.rotateDegrees(Axis.YP, state.lyingYaw);
			// Slightly tilted, as if it had dug in with its nose
			poseStack.rotateDegrees(Axis.XP, 8.0F);
		} else {
			poseStack.translate(0.0F, CbuModels.BOMBLET_LENGTH / 2.0F, 0.0F);
			poseStack.rotateDegrees(Axis.ZP, Mth.sin(state.ageInTicks * 0.4F) * 8.0F);
			poseStack.rotateDegrees(Axis.YP, state.ageInTicks * 25.0F);
			// Nose (+Z) points down
			poseStack.rotateDegrees(Axis.XP, 90.0F);
		}
		boolean ballute = !state.landed;
		int light = state.lightCoords;
		submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
			CbuModels.BOMBLET.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, -1);
			if (ballute) {
				CbuModels.BALLUTE.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, -1);
			}
		});
		if (state.outlineColor != 0 && RENDER_TYPE.outline().isPresent()) {
			int outline = state.outlineColor;
			submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE.outline().get(),
					(pose, buffer) -> CbuModels.BOMBLET.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, outline));
		}
		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, camera);
	}
}
