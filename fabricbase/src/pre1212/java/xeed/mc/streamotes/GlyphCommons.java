package xeed.mc.streamotes;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;

public class GlyphCommons {
	public static BakedGlyph atDrawGlyph(DrawerCommons.State state, boolean shadow, BakedGlyph original) {
		if (state.length == 0) return original;

		if (!shadow) {
			var icon = Compat.getEmote(state.style);
			if (icon == null) return EmptyGlyph.INSTANCE;

			if (icon.getTexture().isLoaded()) {
				return EmoticonGlyph.of(icon, state.color);
			}
			else {
				icon.requestTexture();
			}
		}

		return EmptyGlyph.INSTANCE;
	}
}
