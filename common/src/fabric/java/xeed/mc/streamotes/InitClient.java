package xeed.mc.streamotes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class InitClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		var mod = new Streamotes();
		Streamotes.INSTANCE = mod;

		ClientLifecycleEvents.CLIENT_STARTED.register(mod::onClientStarted);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> mod.onPlayerJoin());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> mod.onPlayerDisconnect());

		mod.onInitializeClient();
		Compat.onInitializeClient();
	}
}
