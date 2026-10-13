package explosivearrows.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import explosivearrows.ClusterBombLauncherItem;
import explosivearrows.ModComponents;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * Item model of the CBU-87 launcher, same scale as the bomb item and also lying along the X axis (muzzle towards
 * -X). Positioned per context by {@code models/item/cbu87_launcher.json}.
 *
 * <p>Loaded, the folded bomb sits in the tube. Reload animation (driven by the reload start time on the stack): the
 * tube swings across the view so that its rear end is visible, the bomb slides in from behind, the latch snaps,
 * the tube swings back.
 */
public class LauncherSpecialRenderer implements SpecialModelRenderer<LauncherSpecialRenderer.State> {
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(CbuModels.LAUNCHER_TEXTURE);
	/** Bomb position along the tube when loaded (tube runs from -1.3 to 1.3). */
	private static final float LOADED_Z = -0.08F;
	/** Bomb position at the start of the reload: completely behind the tube. */
	private static final float START_Z = -1.3F - CbuModels.BOMB_LENGTH / 2.0F - 0.6F;
	private static final float SWING_DEGREES = 75.0F;

	public record State(boolean loaded, long reloadStart) {
	}

	@Override
	public @Nullable State extractArgument(ItemStack stack) {
		Long start = stack.get(ModComponents.LAUNCHER_RELOAD_START);
		return new State(ClusterBombLauncherItem.isLoaded(stack), start != null ? start : Long.MIN_VALUE);
	}

	@Override
	public void submit(@Nullable State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords,
			boolean hasFoil, int outlineColor) {
		float progress = state != null ? reloadProgress(state) : -1.0F;
		boolean reloading = progress >= 0.0F;
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F, 0.5F);
		float bombZ = LOADED_Z;
		float bombY = 0.0F;
		float latch = 0.0F;
		if (reloading) {
			// Swing out (0..0.2), hold, swing back (0.85..1)
			float swing = smooth(progress / 0.2F) * (1.0F - smooth((progress - 0.85F) / 0.15F));
			// Item space -X is "forward" in first person: push the tube away a bit so it is completely visible
			poseStack.translate(-0.6F * swing, 0.05F * swing, 0.0F);
			poseStack.rotateDegrees(Axis.YP, SWING_DEGREES * swing);
			// Bomb slides in (0.2..0.75), coming up slightly from below
			float slide = easeOutCubic((progress - 0.2F) / 0.55F);
			bombZ = Mth.lerp(slide, START_Z, LOADED_Z);
			bombY = -0.15F * (1.0F - smooth(slide * 2.0F));
			// Latch jolt (0.75..0.82)
			float jolt = Mth.clamp((progress - 0.75F) / 0.07F, 0.0F, 1.0F);
			latch = jolt > 0.0F && jolt < 1.0F ? Mth.sin(jolt * Mth.PI) * 0.05F : 0.0F;
		}
		poseStack.scale(ClusterBombSpecialRenderer.SCALE, ClusterBombSpecialRenderer.SCALE, ClusterBombSpecialRenderer.SCALE);
		poseStack.rotateDegrees(Axis.YP, -90.0F);
		poseStack.translate(0.0F, 0.0F, latch);
		submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE,
				(pose, buffer) -> CbuModels.LAUNCHER.render(pose, buffer, lightCoords, overlayCoords, -1));
		if (outlineColor != 0 && RENDER_TYPE.outline().isPresent()) {
			submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE.outline().get(),
					(pose, buffer) -> CbuModels.LAUNCHER.render(pose, buffer, lightCoords, overlayCoords, outlineColor));
		}
		if (reloading || state != null && state.loaded()) {
			poseStack.pushPose();
			poseStack.translate(0.0F, bombY, bombZ - latch);
			submitNodeCollector.submitCustomGeometry(poseStack, ClusterBombRenderer.RENDER_TYPE,
					(pose, buffer) -> CbuModels.BOMB_FOLDED.render(pose, buffer, lightCoords, overlayCoords, -1));
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	private static float reloadProgress(State state) {
		Minecraft minecraft = Minecraft.getInstance();
		if (state.reloadStart() == Long.MIN_VALUE || minecraft.level == null) {
			return -1.0F;
		}
		float gameTime = minecraft.level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		float progress = (gameTime - state.reloadStart()) / ClusterBombLauncherItem.RELOAD_TICKS;
		return progress < 0.0F || progress > 1.0F ? -1.0F : progress;
	}

	private static float smooth(float t) {
		t = Mth.clamp(t, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	private static float easeOutCubic(float t) {
		t = Mth.clamp(t, 0.0F, 1.0F);
		float inverse = 1.0F - t;
		return 1.0F - inverse * inverse * inverse;
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		float halfLength = CbuModels.LAUNCHER_LENGTH / 2.0F * ClusterBombSpecialRenderer.SCALE;
		float radius = 0.42F * ClusterBombSpecialRenderer.SCALE;
		for (int i = 0; i < 8; i++) {
			output.accept(new Vector3f(
					0.5F + ((i & 1) == 0 ? -halfLength : halfLength),
					0.5F + ((i & 2) == 0 ? -radius : radius),
					0.5F + ((i & 4) == 0 ? -radius : radius)));
		}
	}

	public record Unbaked() implements SpecialModelRenderer.Unbaked<State> {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public LauncherSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new LauncherSpecialRenderer();
		}
	}
}
