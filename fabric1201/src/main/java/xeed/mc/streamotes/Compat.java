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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import xeed.mc.streamotes.emoticon.Emoticon;

import java.util.Objects;
import java.util.function.Function;

public class Compat {
	private static final RenderPhase.ShaderProgram PROGRAM = new RenderPhase.ShaderProgram(GameRenderer::getPositionTexColorProgram);

	public static final Function<Emoticon, RenderLayer> LAYER = Util.memoize(icon ->
		RenderLayer.of("emote-" + icon.code, VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS, 2048, false, false,
			RenderLayer.MultiPhaseParameters.builder().texture(new RenderPhase.TextureBase(icon.getTexture()::preApply, icon.getTexture()::postApply))
				.program(PROGRAM).depthTest(RenderPhase.ALWAYS_DEPTH_TEST).build(false)));

	public static void onInitializeServer() {
	}

	public static void onInitializeClient(Streamotes.StringAction handler) {
		ClientPlayNetworking.registerGlobalReceiver(StreamotesCommon.IDENT, (c, h, buf, rs) -> {
			handler.apply(buf.readString());
		});
	}

	public static void sendFeedback(ServerCommandSource source, Text message, boolean broadcastToOps) {
		source.sendFeedback(() -> message, broadcastToOps);
	}

	public static Packet<?> createConfigPacket(String json) {
		var buf = PacketByteBufs.create();
		buf.writeString(json);
		return ServerPlayNetworking.createS2CPacket(Objects.requireNonNull(StreamotesCommon.IDENT), buf);
	}

	public static SystemToast.Type makeToastType() {
		return SystemToast.Type.PERIODIC_NOTIFICATION;
	}

	public static Style makeEmoteStyle(Emoticon icon) {
		return Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, icon.getName()))
			.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, icon.getTooltip()));
	}

	public static void nextVertex(VertexConsumer builder) {
		builder.next();
	}

	public static class Texture implements AutoCloseable {
		private int glId = -1;

		public boolean isLoaded() {
			return glId != -1;
		}

		public void preApply() {
			RenderSystem.disableBlend();
			RenderSystem.setShaderTexture(0, glId);
		}

		public void postApply() {
			RenderSystem.enableBlend();
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
