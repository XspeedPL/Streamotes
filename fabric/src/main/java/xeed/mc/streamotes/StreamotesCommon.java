package xeed.mc.streamotes;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.packet.Packet;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

public class StreamotesCommon implements ModInitializer {
	public static final String NAME = "streamotes";

	public static final Identifier IDENT = new Identifier("xeed", NAME);

	public static final Pattern VALID_CHANNEL_PATTERN = Pattern.compile("[_a-zA-Z]\\w+");

	private static final Logger LOGGER = LogManager.getLogger(NAME);

	public static ModConfigModel getOwnConfig() {
		return ModConfigModel.getInstance();
	}

	public static ModConfigModel configFromJson(String json) {
		var gson = new Gson();
		try {
			return gson.fromJson(json, ModConfigModel.class);
		}
		catch (JsonSyntaxException ex) {
			return null;
		}
	}

	private static String configToJson(ModConfigModel cfg) {
		var gson = new Gson();
		return gson.toJson(cfg, ModConfigModel.class);
	}

	public static void logi(String text) {
		LOGGER.info(text);
	}

	public static void loge(String text, Throwable t) {
		LOGGER.error(text, t);
	}

	@Override
	public void onInitialize() {
		ModConfigModel.reload();
		CommandRegistrationCallback.EVENT.register(this::registerCommands);
		ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(this::onReload);
		PayloadTypeRegistry.playS2C().register(JsonPayload.PACKET_ID, JsonPayload.PACKET_CODEC);
	}

	private static Packet<?> createConfigPacket() {
		var buf = new JsonPayload(configToJson(getOwnConfig()));
		return ServerPlayNetworking.createS2CPacket(buf);
	}

	private void onReload(MinecraftServer server, LifecycledResourceManager resourceManager, boolean success) {
		ModConfigModel.reload();
		server.getPlayerManager().sendToAll(createConfigPacket());
	}

	private void onPlayerJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
		serverPlayNetworkHandler.sendPacket(createConfigPacket());
	}

	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess buildContext, CommandManager.RegistrationEnvironment environment) {
		// TODO: Add/remove channels live with commands?
	}
}
