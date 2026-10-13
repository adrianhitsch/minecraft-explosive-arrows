package explosivearrows;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

/** Item components of the CBU-87 launcher (saved and synced to the client, which renders them). */
public final class ModComponents {
	/** A bomb is in the tube. */
	public static final DataComponentType<Boolean> LAUNCHER_LOADED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ExplosiveArrows.id("launcher_loaded"),
			DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
	/** Game time at which the current reload started (drives the reload animation on the client). */
	public static final DataComponentType<Long> LAUNCHER_RELOAD_START = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ExplosiveArrows.id("launcher_reload_start"),
			DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

	private ModComponents() {
	}

	static void init() {
	}
}
