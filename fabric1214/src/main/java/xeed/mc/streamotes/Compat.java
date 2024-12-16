package xeed.mc.streamotes;

import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.network.packet.Packet;
import org.lwjgl.opengl.GL11;

public class Compat {
	public static void onInitializeServer() {
		PayloadTypeRegistry.playS2C().register(JsonPayload.PACKET_ID, JsonPayload.PACKET_CODEC);
	}

	public static void onInitializeClient(Streamotes.StringAction handler) {
		ClientPlayNetworking.registerGlobalReceiver(JsonPayload.PACKET_ID, (packet, context) -> {
			handler.apply(packet.json());
		});
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
		// enable mipmap
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
		// disable blur
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		loadBuffer.upload(0, 0, 0, 0, 0, loadBuffer.getWidth(), loadBuffer.getHeight(), true);
	}
}
