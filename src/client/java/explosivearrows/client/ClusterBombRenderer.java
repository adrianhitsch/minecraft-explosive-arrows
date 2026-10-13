package explosivearrows.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import explosivearrows.ClusterBombEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** Renders the falling CBU-87 (nose along the flight path, spinning) and the placed one (lying on its fins). */
public class ClusterBombRenderer<T extends Entity> extends EntityRenderer<T, ClusterBombRenderer.State> {
	static final RenderType RENDER_TYPE = RenderTypes.entityCutout(CbuModels.BOMB_TEXTURE);
	/** Axis height of a bomb lying on the ground: the lower fins (X configuration) touch the ground. */
	private static final float LYING_AXIS_HEIGHT = 0.29F;
	/** The fins spin the bomb up to this rate (degrees per tick). */
	private static final float MAX_SPIN = 40.0F;
	private static final float SPIN_ACCELERATION = 0.5F;

	public static class State extends EntityRenderState {
		float yRot;
		float xRot;
		float spin;
		boolean lying;
	}

	public ClusterBombRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.4F;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.xRot = entity.getXRot(partialTicks);
		state.lying = !(entity instanceof ClusterBombEntity);
		state.spin = state.lying ? 0.0F : spinAngle(state.ageInTicks);
	}

	/** Angle after {@code t} ticks with constant spin-up until the maximum rate. */
	private static float spinAngle(float t) {
		float spinUpTicks = MAX_SPIN / SPIN_ACCELERATION;
		if (t < spinUpTicks) {
			return 0.5F * SPIN_ACCELERATION * t * t;
		}
		return 0.5F * MAX_SPIN * spinUpTicks + MAX_SPIN * (t - spinUpTicks);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, state.lying ? LYING_AXIS_HEIGHT : state.boundingBoxHeight / 2.0F, 0.0F);
		poseStack.rotateDegrees(Axis.YP, state.yRot);
		poseStack.rotateDegrees(Axis.XP, -state.xRot);
		poseStack.rotateDegrees(Axis.ZP, state.spin);
		submitBomb(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, camera);
	}

	/** Draws the bomb mesh at the origin of the pose (also used for the item). */
	static void submitBomb(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, int overlay, int outlineColor) {
		submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> CbuModels.BOMB.render(pose, buffer, light, overlay, -1));
		if (outlineColor != 0 && RENDER_TYPE.outline().isPresent()) {
			submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE.outline().get(),
					(pose, buffer) -> CbuModels.BOMB.render(pose, buffer, light, overlay, outlineColor));
		}
	}

	@Override
	protected AABB getBoundingBoxForCulling(T entity, float partialTicks) {
		// The bomb is much longer than its hitbox
		return super.getBoundingBoxForCulling(entity, partialTicks).inflate(CbuModels.BOMB_LENGTH / 2.0F);
	}
}
