package xeed.mc.streamotes;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.joml.Matrix4f;
import xeed.mc.streamotes.emoticon.Emoticon;

public class EmoticonGlyph extends BakedGlyph {
	private final Emoticon icon;
	private final float y;
	private final float w;
	private final float h;
	private final int color;

	public static EmoticonGlyph of(Emoticon icon, int color) {
		var layer = Compat.getLayer(icon);
		var layerSet = new GlyphRenderTypes(layer, layer, layer);

		var client = Minecraft.getInstance();
		float lineSpacing = (float)(client.options.chatLineSpacing().get() * 4);
		float height = client.font.lineHeight + lineSpacing * 2;

		return new EmoticonGlyph(layerSet, icon, -lineSpacing - 1f, icon.getRenderWidth(height), height, color);
	}

	public EmoticonGlyph(GlyphRenderTypes layerSet, Emoticon icon, float y, float w, float h, int color) {
		super(layerSet, 0f, 1f, 0f, 1f, 0f, w, 0f, h);
		this.icon = icon;
		this.y = y;
		this.w = w;
		this.h = h;
		this.color = color;
	}

	@Override
	public void render(boolean italic, float x, float y, Matrix4f matrix, VertexConsumer consumer, float red, float green, float blue, float alpha, int light) {
		if (icon.isAnimated()) icon.updateAnimation();

		drawQuad(consumer, matrix, x, this.y + y, 0f, w, h,
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
		
		Compat.addVertex(consumer, matrix, x0, y1, z, color, u0, v1, light);
		Compat.addVertex(consumer, matrix, x1, y1, z, color, u1, v1, light);
		Compat.addVertex(consumer, matrix, x1, y0, z, color, u1, v0, light);
		Compat.addVertex(consumer, matrix, x0, y0, z, color, u0, v0, light);
	}
}
