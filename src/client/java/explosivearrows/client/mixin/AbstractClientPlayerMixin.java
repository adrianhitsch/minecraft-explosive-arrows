package explosivearrows.client.mixin;

import explosivearrows.client.LauncherScope;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The launcher scope zooms less than the spyglass (which would return 0.1). */
@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerMixin {
	@Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
	private void explosivearrows$launcherZoom(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
		if (firstPerson && LauncherScope.isAiming((AbstractClientPlayer) (Object) this)) {
			cir.setReturnValue(LauncherScope.FOV_MODIFIER);
		}
	}
}
