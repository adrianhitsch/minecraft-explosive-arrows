package explosivearrows.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Item model of the CBU-87: the same round mesh as the entity, scaled down and lying along the X axis
 * (nose towards -X). The display transforms of {@code models/item/cbu87.json} position it per context.
 */
public class ClusterBombSpecialRenderer implements NoDataSpecialModelRenderer {
	static final float SCALE = 0.45F;

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
		poseStack.pushPose();
		transform(poseStack);
		ClusterBombRenderer.submitBomb(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
		poseStack.popPose();
	}

	private static void transform(PoseStack poseStack) {
		poseStack.translate(0.5F, 0.5F, 0.5F);
		poseStack.scale(SCALE, SCALE, SCALE);
		poseStack.rotateDegrees(Axis.YP, -90.0F);
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		float halfLength = (CbuModels.BOMB_LENGTH / 2.0F + 0.1F) * SCALE;
		float radius = 0.4F * SCALE;
		for (int i = 0; i < 8; i++) {
			output.accept(new Vector3f(
					0.5F + ((i & 1) == 0 ? -halfLength : halfLength),
					0.5F + ((i & 2) == 0 ? -radius : radius),
					0.5F + ((i & 4) == 0 ? -radius : radius)));
		}
	}

	public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public ClusterBombSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new ClusterBombSpecialRenderer();
		}
	}
}
