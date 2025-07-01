package xeed.mc.streamotes;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Style;
import xeed.mc.streamotes.emoticon.Emoticon;

import java.util.concurrent.ConcurrentHashMap;

public class DrawerCommons {
	public static class State {
		public int length;
		public Style style;
		public int color;
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
		if (emote == null || state.length >= emote.getName().length()) state.length = 0;
	}

	public static Float atGetAdvance(State state) {
		if (state.length == 0) return null;

		var icon = Compat.getEmote(state.style);
		if (icon == null) return null;

		return state.length >= icon.getName().length() ? icon.getChatRenderWidth() : 0f;
	}
}
