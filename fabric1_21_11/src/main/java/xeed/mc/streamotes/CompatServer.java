package xeed.mc.streamotes;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;

import java.util.Objects;

public class CompatServer {
	public static final Identifier IDENT = Objects.requireNonNull(Identifier.tryBuild("xeed", StreamotesCommon.NAME));

	public static boolean permissionPredicate(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}

	public static void onInitializeServer() {
		PayloadTypeRegistry.playS2C().register(JsonPayload.PACKET_ID, JsonPayload.PACKET_CODEC);
	}

	public static Packet<?> createConfigPacket(String json) {
		return ServerPlayNetworking.createS2CPacket(new JsonPayload(json));
	}

	public static void sendFeedback(CommandSourceStack source, Component message, boolean broadcastToOps) {
		source.sendSuccess(() -> message, broadcastToOps);
	}
}
