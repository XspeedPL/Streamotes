package xeed.mc.streamotes;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Objects;

public class Compat {
	public static void onInitializeServer() {
	}

	public static void onInitializeClient(Streamotes.StringAction handler) {
		ClientPlayNetworking.registerGlobalReceiver(StreamotesCommon.IDENT, (c, h, buf, rs) -> {
			handler.apply(buf.readString());
		});
	}

	public static void sendFeedback(ServerCommandSource source, Text message, boolean broadcastToOps) {
		source.sendFeedback(message, broadcastToOps);
	}

	public static Packet<?> createConfigPacket(String json) {
		var buf = PacketByteBufs.create();
		buf.writeString(json);
		return ServerPlayNetworking.createS2CPacket(Objects.requireNonNull(StreamotesCommon.IDENT), buf);
	}

	public static SystemToast.Type makeToastType() {
		return SystemToast.Type.PERIODIC_NOTIFICATION;
	}

	public static BufferBuilder makeBufferBuilder() {
		var buf = Tessellator.getInstance().getBuffer();
		buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
		return buf;
	}

	public static void nextVertex(VertexConsumer builder) {
		builder.next();
	}

	public static class Texture implements AutoCloseable {
		private int glId;

		public boolean isLoaded() {
			return glId != -1;
		}

		public void setShaderTexture(int index) {
			RenderSystem.setShaderTexture(index, glId);
		}

		public void upload(String label, NativeImage buffer) {
			if (glId == -1) glId = TextureUtil.generateTextureId();
			TextureUtil.prepareImage(glId, 0, buffer.getWidth(), buffer.getHeight());

			buffer.upload(0, 0, 0, 0, 0, buffer.getWidth(), buffer.getHeight(), false, false, true, true);
		}

		public void close() {
			if (glId != -1) {
				TextureUtil.releaseTextureId(glId);
				glId = -1;
			}
		}
	}
}
