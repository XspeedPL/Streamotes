package xeed.mc.streamotes;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
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

import java.util.function.Function;

public class Compat {
	public static final Function<Emoticon, RenderLayer> LAYER = Util.memoize(icon ->
		RenderLayer.of("emote-" + icon.code, 2048, false, false, RenderPipelines.GUI_TEXTURED,
			RenderLayer.MultiPhaseParameters.builder().texture(new RenderPhase.TextureBase(icon.getTexture()::onApply, Runnables.doNothing()))
				.build(false)));

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

	public static Style makeEmoteStyle(Emoticon icon) {
		return Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(icon.getName()))
			.withHoverEvent(new HoverEvent.ShowText(icon.getTooltip()));
	}

	public static void nextVertex(VertexConsumer consumer) {
	}

	public static class Texture implements AutoCloseable {
		private GpuTexture texture;

		public boolean isLoaded() {
			return texture != null && !texture.isClosed();
		}

		public void onApply() {
			RenderSystem.setShaderTexture(0, texture);
		}

		public void upload(String label, NativeImage buffer) {
			var dev = RenderSystem.getDevice();

			if (texture == null) {
				texture = dev.createTexture(label, TextureFormat.RGBA8, buffer.getWidth(), buffer.getHeight(), 1);
				texture.setTextureFilter(FilterMode.LINEAR, FilterMode.NEAREST, true);
			}

			RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, buffer);
		}

		public void close() {
			if (texture != null && !texture.isClosed()) {
				texture.close();
				texture = null;
			}
		}
	}
}
