package xeed.mc.streamotes;

import net.minecraft.client.font.EmptyGlyphRenderer;
import net.minecraft.client.font.GlyphRenderer;

public class GlyphCommons {
	public static GlyphRenderer atDrawGlyph(DrawerCommons.State state, boolean shadow, float x, float y, GlyphRenderer original) {
		if (state.length == 0) return original;

		if (!shadow) {
			var icon = Compat.getEmote(state.style);
			if (icon == null) return EmptyGlyphRenderer.INSTANCE;

			if (icon.getTexture().isLoaded()) {
				return EmoticonGlyph.of(icon, x, y, state.color);
			}
			else {
				icon.requestTexture();
			}
		}

		return EmptyGlyphRenderer.INSTANCE;
	}
}
