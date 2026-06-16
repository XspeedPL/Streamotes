package xeed.mc.streamotes;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Objects;

public class CompatServer {
	public static final Identifier IDENT = Objects.requireNonNull(Identifier.tryBuild("xeed", StreamotesCommon.NAME));

	public static boolean permissionPredicate(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}

	public static void onInitializeServer(RegisterPayloadHandlersEvent event) {
		event.registrar(JsonPayload.PACKET_ID.id().getNamespace()).optional().playToClient(JsonPayload.PACKET_ID, JsonPayload.PACKET_CODEC);
	}

	public static Packet<?> createConfigPacket(String json) {
		return new ClientboundCustomPayloadPacket(new JsonPayload(json));
	}

	public static void sendFeedback(CommandSourceStack source, Component message, boolean broadcastToOps) {
		source.sendSuccess(() -> message, broadcastToOps);
	}
}
