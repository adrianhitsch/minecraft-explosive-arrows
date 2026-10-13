package explosivearrows.client;

import com.mojang.blaze3d.platform.InputConstants;
import explosivearrows.ClusterBombLauncherItem;
import explosivearrows.LauncherPayloads;
import explosivearrows.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/** Left click fires the launcher instead of attacking/mining, the reload key (default R) reloads it. */
final class LauncherControls {
	private static final KeyMapping RELOAD = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.explosivearrows.reload_launcher", InputConstants.KEY_R, KeyMapping.Category.GAMEPLAY));

	private LauncherControls() {
	}

	static void register() {
		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
			ItemStack stack = player.getMainHandItem();
			if (!stack.is(ModItems.CBU87_LAUNCHER)) {
				return false;
			}
			if (clickCount > 0) {
				fire(player, stack);
			}
			// Never attack or mine with the launcher in hand
			return true;
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (RELOAD.consumeClick()) {
				if (client.player != null && client.player.getMainHandItem().is(ModItems.CBU87_LAUNCHER)) {
					ClientPlayNetworking.send(LauncherPayloads.Reload.INSTANCE);
				}
			}
		});
	}

	private static void fire(LocalPlayer player, ItemStack stack) {
		boolean reloading = ClusterBombLauncherItem.reloadProgress(stack, player.level().getGameTime()) >= 0.0F;
		if (ClusterBombLauncherItem.isLoaded(stack) && !reloading) {
			// The client moves its own player, so the recoil is applied here
			player.push(player.getLookAngle().scale(-ClusterBombLauncherItem.RECOIL));
		}
		ClientPlayNetworking.send(LauncherPayloads.Fire.INSTANCE);
	}
}
