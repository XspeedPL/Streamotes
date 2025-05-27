package xeed.mc.streamotes;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

public class Compat {
	public static RenderLayer layerFunc(Emoticon icon) {
		return RenderLayer.of("emote-" + icon.getName(), VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS, 2048, false, false,
			RenderLayer.MultiPhaseParameters.builder().texture(new RenderPhase.TextureBase(icon.getTexture()::onApply, Runnables.doNothing()))
				.program(RenderPhase.POSITION_TEXTURE_COLOR_PROGRAM).transparency(RenderPhase.Transparency.TRANSLUCENT_TRANSPARENCY).build(false));
	}

	public static void onInitializeClient(Streamotes.StringAction handler) {
		ClientPlayNetworking.registerGlobalReceiver(JsonPayload.PACKET_ID, (packet, context) -> {
			handler.apply(packet.json());
		});
	}

	public static SystemToast.Type makeToastType() {
		return new SystemToast.Type(4000);
	}

	public static Style makeEmoteStyle(Emoticon icon) {
		return Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, icon.getName()))
			.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, icon.getTooltip()));
	}

	public static Emoticon getEmote(Style style) {
		var ev = style.getClickEvent();
		return ev != null && ev.getAction() == ClickEvent.Action.COPY_TO_CLIPBOARD
			? EmoticonRegistry.fromName(ev.getValue())
			: null;
	}

	public static void nextVertex(VertexConsumer consumer) {
	}

	public static class Texture implements AutoCloseable {
		private int glId = -1;

		public boolean isLoaded() {
			return glId != -1;
		}

		public void onApply() {
			if (glId != -1) RenderSystem.setShaderTexture(0, glId);
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
