package explosivearrows.client;

import explosivearrows.ExplosiveArrows;
import explosivearrows.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Scope of the CBU-87 launcher (hold right click). Zoom and overlay hook into the vanilla spyglass code via mixins.
 * The reticle ({@code scripts/generate-textures.py}) has hold-over marks for 80, 100 and 120 blocks at the default
 * field of view of 70.
 */
public final class LauncherScope {
	/** 4x zoom (the spyglass has 10x). */
	public static final float FOV_MODIFIER = 0.25F;
	private static final Identifier TEXTURE = ExplosiveArrows.id("textures/misc/cbu87_scope.png");

	private LauncherScope() {
	}

	public static boolean isAiming(Player player) {
		return player.isUsingItem() && player.getUseItem().is(ModItems.CBU87_LAUNCHER);
	}

	public static boolean isAiming() {
		Player player = Minecraft.getInstance().player;
		return player != null && isAiming(player);
	}

	/** Same layout as the vanilla spyglass overlay: square texture in the middle, black around it. */
	public static void extractOverlay(GuiGraphicsExtractor graphics, float scale) {
		float size = Math.min(graphics.guiWidth(), graphics.guiHeight());
		float ratio = Math.min(graphics.guiWidth() / size, graphics.guiHeight() / size) * scale;
		int width = Mth.floor(size * ratio);
		int height = Mth.floor(size * ratio);
		int left = (graphics.guiWidth() - width) / 2;
		int top = (graphics.guiHeight() - height) / 2;
		int right = left + width;
		int bottom = top + height;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0.0F, 0.0F, width, height, width, height);
		graphics.fill(RenderPipelines.GUI, 0, bottom, graphics.guiWidth(), graphics.guiHeight(), 0xFF000000);
		graphics.fill(RenderPipelines.GUI, 0, 0, graphics.guiWidth(), top, 0xFF000000);
		graphics.fill(RenderPipelines.GUI, 0, top, left, bottom, 0xFF000000);
		graphics.fill(RenderPipelines.GUI, right, top, graphics.guiWidth(), bottom, 0xFF000000);
	}
}
