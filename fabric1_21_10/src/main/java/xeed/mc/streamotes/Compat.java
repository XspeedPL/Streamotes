package xeed.mc.streamotes;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

public class Compat {
	public static RenderLayer layerFunc(Emoticon icon) {
		return RenderLayer.of("emote-" + icon.getName(), 2048, false, true, RenderPipelines.RENDERTYPE_TEXT,
			RenderLayer.MultiPhaseParameters.builder().texture(new RenderPhase.TextureBase(icon.getTexture()::onApply, Runnables.doNothing()))
				.build(false));
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
		return Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(icon.getName()))
			.withHoverEvent(new HoverEvent.ShowText(icon.getTooltip()));
	}

	public static Emoticon getEmote(Style style) {
		return style.getClickEvent() instanceof ClickEvent.CopyToClipboard(String value)
			? EmoticonRegistry.fromName(value)
			: null;
	}

	public static class Texture implements AutoCloseable {
		private GpuTexture texture;
		private GpuTextureView view;

		public boolean isLoaded() {
			return view != null && !view.isClosed();
		}

		public GpuTextureView getView() {
			return view;
		}

		public void onApply() {
			if (texture != null) RenderSystem.setShaderTexture(0, view);
		}

		public void upload(String label, NativeImage buffer) {
			var dev = RenderSystem.getDevice();

			if (texture == null) {
				texture = dev.createTexture(label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, buffer.getWidth(), buffer.getHeight(), 1, 1);
				texture.setTextureFilter(FilterMode.LINEAR, FilterMode.NEAREST, true);
			}
			if (view == null) {
				view = dev.createTextureView(texture);
			}

			RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, buffer);
		}

		public void close() {
			if (view != null && !view.isClosed()) {
				view.close();
				view = null;
			}
			if (texture != null && !texture.isClosed()) {
				texture.close();
				texture = null;
			}
		}
	}
}
