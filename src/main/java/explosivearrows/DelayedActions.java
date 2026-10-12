package explosivearrows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;

/** Runs actions in a level after a number of ticks (aftershocks, countdown, etc.). */
public final class DelayedActions {
	private record Action(long gameTime, Consumer<ServerLevel> action) {
	}

	// Only accessed from the server thread
	private static final Map<ServerLevel, List<Action>> PENDING = new WeakHashMap<>();

	private DelayedActions() {
	}

	static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(DelayedActions::tick);
	}

	static void schedule(ServerLevel level, int delayTicks, Consumer<ServerLevel> action) {
		PENDING.computeIfAbsent(level, l -> new ArrayList<>()).add(new Action(level.getGameTime() + delayTicks, action));
	}

	private static void tick(ServerLevel level) {
		List<Action> list = PENDING.get(level);
		if (list == null || list.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		List<Action> due = new ArrayList<>();
		for (Iterator<Action> it = list.iterator(); it.hasNext(); ) {
			Action action = it.next();
			if (action.gameTime() <= now) {
				due.add(action);
				it.remove();
			}
		}
		// Actions may schedule further actions
		for (Action action : due) {
			action.action().accept(level);
		}
	}
}
