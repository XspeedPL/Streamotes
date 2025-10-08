package xeed.mc.streamotes;

import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.EmptyGlyph;

public class GlyphCommons {
	private static final BakedGlyph EMPTY = new EmptyGlyph(0f).bake(null);

	public static BakedGlyph atDrawGlyph(DrawerCommons.State state, boolean shadow, float x, float y, BakedGlyph original) {
		if (state.length == 0) return original;

		if (!shadow) {
			var icon = Compat.getEmote(state.style);
			if (icon == null) return EMPTY;

			if (icon.getTexture().isLoaded()) {
				return EmoticonGlyph.of(icon, x, y, state.color);
			}
			else {
				icon.requestTexture();
			}
		}

		return EMPTY;
	}
}
