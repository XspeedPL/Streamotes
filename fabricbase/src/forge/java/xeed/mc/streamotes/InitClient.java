package xeed.mc.streamotes;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(StreamotesCommon.NAME)
public class InitClient {
	public InitClient(IEventBus bus) {
		Streamotes.INSTANCE = new Streamotes();

		bus.addListener(InitClient::onClientSetup);
		bus.addListener(InitClient::onClientStarted);
		bus.addListener(InitClient::onPlayerJoin);
		bus.addListener(InitClient::onPlayerDisconnect);
		bus.addListener(InitClient::onRegisterPayloads);
	}

	private static void onClientSetup(FMLClientSetupEvent event) {
		Streamotes.INSTANCE.onInitializeClient();

		ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
			() -> (client, parent) -> ConfigScreenBuilder.createConfigScreen(parent));
	}

	private static void onClientStarted(ClientStartedEvent event) {
		Streamotes.INSTANCE.onClientStarted(event.getClient());
	}

	private static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		Streamotes.INSTANCE.onPlayerJoin();
	}

	private static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
		Streamotes.INSTANCE.onPlayerDisconnect();
	}

	private static void onRegisterPayloads(RegisterClientPayloadHandlersEvent event) {
		Compat.onInitializeClient(event);
	}
}
