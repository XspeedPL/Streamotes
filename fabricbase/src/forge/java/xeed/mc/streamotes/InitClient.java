package xeed.mc.streamotes;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(value = StreamotesCommon.NAME, dist = Dist.CLIENT)
public class InitClient {
	public InitClient(IEventBus bus) {
		Streamotes.INSTANCE = new Streamotes();

		bus.addListener(InitClient::onClientSetup);
		bus.addListener(Compat::onRegisterPayloads);

		NeoForge.EVENT_BUS.addListener(InitClient::onPlayerJoin);
		NeoForge.EVENT_BUS.addListener(InitClient::onPlayerDisconnect);
	}

	private static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			Streamotes.INSTANCE.onInitializeClient();

			ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
				() -> (client, parent) -> ConfigScreenBuilder.createConfigScreen(parent));

			Streamotes.INSTANCE.onClientStarted(Minecraft.getInstance());
		});
	}

	private static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		Streamotes.INSTANCE.onPlayerJoin();
	}

	private static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
		Streamotes.INSTANCE.onPlayerDisconnect();
	}
}
