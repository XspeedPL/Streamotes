package xeed.mc.streamotes;

import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.EmptyBakedGlyph;

public class GlyphCommons {
	public static BakedGlyph atDrawGlyph(DrawerCommons.State state, boolean shadow, float x, float y, BakedGlyph original) {
		if (state.length == 0) return original;

		if (!shadow) {
			var icon = Compat.getEmote(state.style);
			if (icon == null) return EmptyBakedGlyph.INSTANCE;

			if (icon.getTexture().isLoaded()) {
				return EmoticonGlyph.of(icon, x, y, state.color);
			}
			else {
				icon.requestTexture();
			}
		}

		return EmptyBakedGlyph.INSTANCE;
	}
}
