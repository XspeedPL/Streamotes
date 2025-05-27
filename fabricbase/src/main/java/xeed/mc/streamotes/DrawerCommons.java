package xeed.mc.streamotes;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Style;
import org.joml.Matrix4f;
import xeed.mc.streamotes.emoticon.Emoticon;

import java.util.concurrent.ConcurrentHashMap;

public class DrawerCommons {
	public static class State {
		public int length;
		public Style style;
	}

	private static final ConcurrentHashMap<Emoticon, RenderLayer> LAYER_CACHE = new ConcurrentHashMap<>();

	public static RenderLayer getLayer(Emoticon emote) {
		return LAYER_CACHE.computeIfAbsent(emote, Compat::layerFunc);
	}

	public static void clearLayerCache() {
		LAYER_CACHE.clear();
	}

	public static void beforeAccept(State state, int codePoint) {
		if (Compat.getEmote(state.style) != null) {
			if (Character.isBmpCodePoint(codePoint)) {
				++state.length;
			}
			else if (Character.isValidCodePoint(codePoint)) {
				state.length += 2;
			}
		}
	}

	public static void afterAccept(State state) {
		var emote = Compat.getEmote(state.style);
		if (emote == null || state.length >= emote.code.length()) state.length = 0;
	}

	public static boolean atDrawGlyph(State state, boolean shadow, float x, float y, Matrix4f matrix, int color) {
		if (state.length == 0) return false;

		if (!shadow) {
			var icon = Compat.getEmote(state.style);
			if (icon == null) return true;

			if (icon.getTexture().isLoaded()) {
				Streamotes.RENDER_QUEUE.get().addLast(new EmoteRenderInfo(icon, x, y, matrix.m33(), color));
			}
			else {
				icon.requestTexture();
			}
		}
		return true;
	}

	public static Float atGetAdvance(State state) {
		if (state.length == 0) return null;

		var icon = Compat.getEmote(state.style);
		if (icon == null) return null;

		return state.length >= icon.code.length() ? icon.getChatRenderWidth() : 0f;
	}
}
