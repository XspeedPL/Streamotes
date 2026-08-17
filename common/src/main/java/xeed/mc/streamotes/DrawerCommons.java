package xeed.mc.streamotes;

import net.minecraft.network.chat.Style;

public class DrawerCommons {
	public static class State {
		public int length;
		public Style style;
		public int color;
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
