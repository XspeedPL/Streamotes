package xeed.mc.streamotes;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NonNull;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
		return RenderType.create("emote-" + icon.getName(), icon.getTexture().makeSetup());
	}

	public static void onInitializeClient(Streamotes.StringAction handler) {
		ClientPlayNetworking.registerGlobalReceiver(JsonPayload.PACKET_ID, (packet, context) -> {
			handler.apply(packet.json());
		});
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

		public void upload(String label, NativeImage buffer) {
			var dev = RenderSystem.getDevice();

			if (texture == null) {
				texture = dev.createTexture(label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, buffer.getWidth(), buffer.getHeight(), 1, 1);
				//texture.setTextureFilter(FilterMode.LINEAR, FilterMode.NEAREST, true);
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

		public RenderSetup makeSetup() {
			return new CustomSetup();
		}

		public class CustomSetup extends RenderSetup {
			public CustomSetup() {
				super(RenderPipelines.TEXT, Collections.emptyMap(), true, false, LayeringTransform.NO_LAYERING,
					OutputTarget.MAIN_TARGET, TextureTransform.DEFAULT_TEXTURING, OutlineProperty.NONE, false, false, 2048);
			}

			@Override
			@NonNull
			public Map<String, TextureAndSampler> getTextures() {
				var map = new HashMap<String, TextureAndSampler>();
				map.put("Sampler0", new TextureAndSampler(view, RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false)));
				return map;
			}
		}
	}
}
