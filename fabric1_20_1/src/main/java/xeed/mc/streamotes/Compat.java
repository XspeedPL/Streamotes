package xeed.mc.streamotes;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4f;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.util.concurrent.ConcurrentHashMap;

public class Compat {
	private static final ConcurrentHashMap<Emoticon, RenderType> LAYER_CACHE = new ConcurrentHashMap<>();
	public static final SystemToast.SystemToastIds TOAST_TYPE = SystemToast.SystemToastIds.PERIODIC_NOTIFICATION;

	public static ToastComponent getToastManager() {
		return Minecraft.getInstance().getToasts();
	}

	public static RenderType getLayer(Emoticon emote) {
		return LAYER_CACHE.computeIfAbsent(emote, Compat::layerFunc);
	}

	public static void clearLayerCache() {
		LAYER_CACHE.clear();
	}

	public static RenderType layerFunc(Emoticon icon) {
		return RenderType.create("emote-" + icon.getName(), DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 2048, false, true,
			RenderType.CompositeState.builder().setTextureState(new RenderStateShard.EmptyTextureStateShard(icon.getTexture()::onApply, Runnables.doNothing()))
				.setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
				.setLightmapState(RenderStateShard.LIGHTMAP).createCompositeState(false));
	}

	public static void onInitializeClient(Streamotes.StringAction handler) {
		ClientPlayNetworking.registerGlobalReceiver(CompatServer.IDENT, (c, h, buf, rs) -> {
			handler.apply(buf.readUtf());
		});
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

	public static void addVertex(VertexConsumer builder, Matrix4f matrix, float x, float y, float z, int c, float u, float v, int l) {
		builder.vertex(matrix, x, y, z).color(c).uv(u, v).uv2(l).endVertex();
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
