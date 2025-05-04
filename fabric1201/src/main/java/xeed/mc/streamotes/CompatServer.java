package xeed.mc.streamotes;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Objects;

public class CompatServer {
	public static void onInitializeServer() {
	}

	public static Packet<?> createConfigPacket(String json) {
		var buf = PacketByteBufs.create();
		buf.writeString(json);
		return ServerPlayNetworking.createS2CPacket(Objects.requireNonNull(StreamotesCommon.IDENT), buf);
	}

	public static void sendFeedback(ServerCommandSource source, Text message, boolean broadcastToOps) {
		source.sendFeedback(() -> message, broadcastToOps);
	}
}
