package xeed.mc.streamotes;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerPlayerConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

public class StreamotesCommon {
	public static final String NAME = "streamotes";
	private static final String RELOAD = "reload";
	private static final String FORCE_RELOAD = "force-reload";

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

	public static void onInitialize() {
		ModConfigModel.reload();
	}

	private static Packet<?> createConfigPacket(boolean forceClear) {
		var cfg = getOwnConfig();
		cfg.forceClearCache = forceClear;
		return CompatServer.createConfigPacket(configToJson(cfg));
	}

	public static void onPlayerJoin(ServerPlayerConnection connection) {
		connection.send(createConfigPacket(false));
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection environment) {
		dispatcher.register(Commands.literal(NAME)
			.requires(CompatServer::permissionPredicate)
			.then(Commands.literal(RELOAD)
				.executes(context -> {
					ModConfigModel.reload();
					context.getSource().getServer().getPlayerList().broadcastAll(createConfigPacket(false));
					return Command.SINGLE_SUCCESS;
				})
			)
			.then(Commands.literal(FORCE_RELOAD)
				.executes(context -> {
					ModConfigModel.reload();
					context.getSource().getServer().getPlayerList().broadcastAll(createConfigPacket(true));
					return Command.SINGLE_SUCCESS;
				}))
			.executes(context -> {
				CompatServer.sendFeedback(context.getSource(), Component.literal("Usage: /" + NAME + " [" + RELOAD + "|" + FORCE_RELOAD + "]"), false);
				return Command.SINGLE_SUCCESS;
			})
		);
	}
}
