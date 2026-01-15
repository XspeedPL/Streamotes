package xeed.mc.streamotes;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.lwjgl.opengl.GL11;
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
		return RenderType.create("emote-" + icon.getName(), DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 2048, false, true,
			RenderType.CompositeState.builder().setTextureState(new RenderStateShard.EmptyTextureStateShard(icon.getTexture()::onApply, Runnables.doNothing()))
				.setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER).setTransparencyState(RenderStateShard.TransparencyStateShard.TRANSLUCENT_TRANSPARENCY)
				.setLightmapState(RenderStateShard.LIGHTMAP).createCompositeState(false));
	}

	public static void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(JsonPayload.PACKET_ID, (packet, context) -> Streamotes.INSTANCE.onReceiveJsonPacket(packet.json()));
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

			// enable mipmap
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
			// disable blur
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

			buffer.upload(0, 0, 0, 0, 0, buffer.getWidth(), buffer.getHeight(), true);
		}

		public void close() {
			if (glId != -1) {
				TextureUtil.releaseTextureId(glId);
				glId = -1;
			}
		}
	}
}
