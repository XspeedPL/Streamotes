package xeed.mc.streamotes;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.regex.Pattern;

public class StreamotesCommon implements ModInitializer {
	public static final String NAME = "streamotes";
	private static final String RELOAD = "reload";
	private static final String FORCE_RELOAD = "force-reload";

	public static final Identifier IDENT = Objects.requireNonNull(Identifier.of("xeed", NAME));

	public static final Pattern VALID_CHANNEL_PATTERN = Pattern.compile("[_a-zA-Z]\\w+");

	private static final Logger LOGGER = LogManager.getLogger(NAME);

	public static ModConfigModel getOwnConfig() {
		return ModConfigModel.getInstance();
	}

	public static ModConfigModel configFromJson(String json) {
		var gson = new Gson();
		try {
			var result = gson.fromJson(json, ModConfigModel.class);
			result.errorReporting = getOwnConfig().errorReporting;
			return result;
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
		CompatServer.onInitializeServer();
	}

	private static Packet<?> createConfigPacket(boolean forceClear) {
		var cfg = getOwnConfig();
		cfg.forceClearCache = forceClear;
		return CompatServer.createConfigPacket(configToJson(cfg));
	}

	private void onPlayerJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
		serverPlayNetworkHandler.sendPacket(createConfigPacket(false));
	}

	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess buildContext, CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal(NAME)
			.requires(source -> source.hasPermissionLevel(3))
			.then(CommandManager.literal(RELOAD)
				.executes(context -> {
					ModConfigModel.reload();
					context.getSource().getServer().getPlayerManager().sendToAll(createConfigPacket(false));
					return Command.SINGLE_SUCCESS;
				})
			)
			.then(CommandManager.literal(FORCE_RELOAD)
				.executes(context -> {
					ModConfigModel.reload();
					context.getSource().getServer().getPlayerManager().sendToAll(createConfigPacket(true));
					return Command.SINGLE_SUCCESS;
				}))
			.executes(context -> {
				CompatServer.sendFeedback(context.getSource(), Text.literal("Usage: /" + NAME + " [" + RELOAD + "|" + FORCE_RELOAD + "]"), false);
				return Command.SINGLE_SUCCESS;
			})
		);
	}
}
