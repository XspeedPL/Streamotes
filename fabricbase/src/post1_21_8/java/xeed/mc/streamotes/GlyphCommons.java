package xeed.mc.streamotes;

import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.EmptyGlyph;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;
import xeed.mc.streamotes.emoticon.Emoticon;

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

	public static void drawEmote(Emoticon icon, Matrix4f matrix, VertexConsumer consumer, float x, float y, float w, float h, int color, int light) {
		if (icon.isAnimated()) icon.updateAnimation();

		drawQuad(consumer, matrix, x, y, 0f, w, h,
			icon.getCurrentFrameTexCoordX(), icon.getCurrentFrameTexCoordY(),
			icon.getWidth(), icon.getHeight(), icon.getSheetWidth(), icon.getSheetHeight(),
			color, light);
	}

	private static void drawQuad(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float z, float w, float h, float u, float v, float regionW, float regionH, int texW, int texH, int color, int light) {
		final float x1 = x0 + w;
		final float y1 = y0 + h;

		final float u0 = u / texW;
		final float u1 = (u + regionW) / texW;

		final float v0 = v / texH;
		final float v1 = (v + regionH) / texH;

		consumer.vertex(matrix, x0, y1, z).color(color).texture(u0, v1).light(light);
		consumer.vertex(matrix, x1, y1, z).color(color).texture(u1, v1).light(light);
		consumer.vertex(matrix, x1, y0, z).color(color).texture(u1, v0).light(light);
		consumer.vertex(matrix, x0, y0, z).color(color).texture(u0, v0).light(light);
	}
}
