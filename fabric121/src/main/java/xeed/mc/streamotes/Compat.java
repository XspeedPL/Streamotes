package xeed.mc.streamotes;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class Compat {
	public static void onInitializeServer() {
		PayloadTypeRegistry.playS2C().register(JsonPayload.PACKET_ID, JsonPayload.PACKET_CODEC);
	}

	public static void onInitializeClient(Streamotes.StringAction handler) {
		ClientPlayNetworking.registerGlobalReceiver(JsonPayload.PACKET_ID, (packet, context) -> {
			handler.apply(packet.json());
		});
	}

	public static void sendFeedback(ServerCommandSource source, Text message, boolean broadcastToOps) {
		source.sendFeedback(() -> message, broadcastToOps);
	}

	public static Packet<?> createConfigPacket(String json) {
		return ServerPlayNetworking.createS2CPacket(new JsonPayload(json));
	}

	public static SystemToast.Type makeToastType() {
		return new SystemToast.Type(4000);
	}

	public static BufferBuilder makeBufferBuilder() {
		return Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
	}

	public static void nextVertex(VertexConsumer builder) {
	}

	public static void uploadImage(NativeImage loadBuffer) {
		loadBuffer.upload(0, 0, 0, 0, 0, loadBuffer.getWidth(), loadBuffer.getHeight(), false, false, true, true);
	}
}
