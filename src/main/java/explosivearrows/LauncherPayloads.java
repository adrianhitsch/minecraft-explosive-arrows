package explosivearrows;

import java.util.function.BiConsumer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Client → server: left click fires the launcher, the reload key reloads it (vanilla has no input for either). */
public final class LauncherPayloads {
	public record Fire() implements CustomPacketPayload {
		public static final Fire INSTANCE = new Fire();
		public static final Type<Fire> TYPE = new Type<>(ExplosiveArrows.id("launcher_fire"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Fire> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<Fire> type() {
			return TYPE;
		}
	}

	public record Reload() implements CustomPacketPayload {
		public static final Reload INSTANCE = new Reload();
		public static final Type<Reload> TYPE = new Type<>(ExplosiveArrows.id("launcher_reload"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Reload> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<Reload> type() {
			return TYPE;
		}
	}

	private LauncherPayloads() {
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Fire.TYPE, Fire.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Reload.TYPE, Reload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Fire.TYPE, (payload, context) ->
				context.server().execute(() -> withLauncher(context.player(), ClusterBombLauncherItem::fire)));
		ServerPlayNetworking.registerGlobalReceiver(Reload.TYPE, (payload, context) ->
				context.server().execute(() -> withLauncher(context.player(), ClusterBombLauncherItem::startReload)));
	}

	private static void withLauncher(ServerPlayer player, BiConsumer<ServerPlayer, ItemStack> action) {
		ItemStack stack = player.getMainHandItem();
		if (stack.is(ModItems.CBU87_LAUNCHER) && player.isAlive() && !player.isSpectator()) {
			action.accept(player, stack);
		}
	}
}
