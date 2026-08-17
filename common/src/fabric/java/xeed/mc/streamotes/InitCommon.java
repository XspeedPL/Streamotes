package xeed.mc.streamotes;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class InitCommon implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(StreamotesCommon::registerCommands);
		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> StreamotesCommon.onPlayerJoin(listener));

		StreamotesCommon.onInitialize();
		CompatServer.onInitializeServer();
	}

	public static Path getConfigDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	public static String getModVersion() {
		return FabricLoader.getInstance().getModContainer(StreamotesCommon.NAME).orElseThrow().getMetadata().getVersion().getFriendlyString();
	}
}
