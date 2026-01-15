package xeed.mc.streamotes;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.nio.file.Path;

@Mod(StreamotesCommon.NAME)
public class InitCommon {
	public InitCommon(IEventBus bus) {
		bus.addListener(InitCommon::onCommonSetup);
		bus.addListener(InitCommon::onRegisterCommands);
		bus.addListener(InitCommon::onPlayerLogin);
		bus.addListener(InitCommon::onRegisterPayloads);
	}

	private static void onCommonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(StreamotesCommon::onInitialize);
	}

	private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		var player = event.getEntity();
		if (player instanceof ServerPlayer) {
			StreamotesCommon.onPlayerJoin(((ServerPlayer)player).connection);
		}
	}

	private static void onRegisterCommands(RegisterCommandsEvent event) {
		StreamotesCommon.registerCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
	}

	private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
		CompatServer.onInitializeServer(event);
	}

	public static Path getConfigDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	public static String getModVersion() {
		return ModList.get().getModFileById(StreamotesCommon.NAME).getMods().getFirst().getVersion().toString();
	}
}
