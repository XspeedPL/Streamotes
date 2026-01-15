package xeed.mc.streamotes;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.util.concurrent.ConcurrentHashMap;

public class Compat {
	private static final ConcurrentHashMap<Emoticon, RenderType> LAYER_CACHE = new ConcurrentHashMap<>();
	public static final SystemToast.SystemToastId TOAST_TYPE = new SystemToast.SystemToastId(4000);

	public static ToastManager getToastManager() {
		return Minecraft.getInstance().getToastManager();
	}

	public static RenderType getLayer(Emoticon emote) {
		return LAYER_CACHE.computeIfAbsent(emote, Compat::layerFunc);
	}

	public static void clearLayerCache() {
		LAYER_CACHE.clear();
	}

	public static RenderType layerFunc(Emoticon icon) {
		return RenderType.create("emote-" + icon.getName(), 2048, false, true, RenderPipelines.TEXT,
			RenderType.CompositeState.builder().setTextureState(new RenderStateShard.EmptyTextureStateShard(icon.getTexture()::onApply, Runnables.doNothing()))
				.createCompositeState(false));
	}

	public static void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(JsonPayload.PACKET_ID, (packet, context) -> Streamotes.INSTANCE.onReceiveJsonPacket(packet.json()));
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

		public boolean isLoaded() {
			return texture != null && !texture.isClosed();
		}

		public void onApply() {
			if (texture != null) RenderSystem.setShaderTexture(0, texture);
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
