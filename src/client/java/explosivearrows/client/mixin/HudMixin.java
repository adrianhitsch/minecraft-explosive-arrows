package explosivearrows.client.mixin;

import explosivearrows.client.LauncherScope;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shows the launcher's reticle instead of the spyglass overlay. */
@Mixin(Hud.class)
abstract class HudMixin {
	@Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
	private void explosivearrows$launcherReticle(GuiGraphicsExtractor graphics, float scale, CallbackInfo ci) {
		if (LauncherScope.isAiming()) {
			LauncherScope.extractOverlay(graphics, scale);
			ci.cancel();
		}
	}
}
