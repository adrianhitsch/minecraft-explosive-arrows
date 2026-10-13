package explosivearrows.mixin;

import explosivearrows.ModItems;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Aiming with the CBU-87 launcher counts as scoping like the spyglass: slower mouse, hidden hand item, scope overlay
 * (replaced by our reticle) and zoom (adjusted) in first person.
 */
@Mixin(Player.class)
abstract class PlayerMixin {
	@Inject(method = "isScoping", at = @At("HEAD"), cancellable = true)
	private void explosivearrows$launcherScope(CallbackInfoReturnable<Boolean> cir) {
		Player self = (Player) (Object) this;
		if (self.isUsingItem() && self.getUseItem().is(ModItems.CBU87_LAUNCHER)) {
			cir.setReturnValue(true);
		}
	}
}
