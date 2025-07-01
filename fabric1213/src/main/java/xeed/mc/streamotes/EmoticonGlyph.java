package xeed.mc.streamotes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;
import xeed.mc.streamotes.emoticon.Emoticon;

public class EmoticonGlyph extends BakedGlyph {
	private final Emoticon icon;
	private final float x;
	private final float y;
	private final float w;
	private final float h;
	private final int color;

	public static EmoticonGlyph of(Emoticon icon, float x, float y, int color) {
		var layer = DrawerCommons.getLayer(icon);
		var layerSet = new TextRenderLayerSet(layer, layer, layer);

		var client = MinecraftClient.getInstance();
		float lineSpacing = (float)(client.options.getChatLineSpacing().getValue() * 4);
		float height = client.textRenderer.fontHeight + lineSpacing * 2;

		return new EmoticonGlyph(layerSet, icon, x, y - lineSpacing - 1f, icon.getRenderWidth(height), height, color);
	}

	public EmoticonGlyph(TextRenderLayerSet layerSet, Emoticon icon, float x, float y, float w, float h, int color) {
		super(layerSet, 0f, 1f, 0f, 1f, x, x + w, y, y + h);
		this.icon = icon;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.color = color;
	}

	@Override
	public void draw(DrawnGlyph glyph, Matrix4f matrix, VertexConsumer consumer, int light) {
		if (icon.isAnimated()) icon.updateAnimation();

		drawQuad(consumer, matrix, x, y, 0f, w, h,
			icon.getCurrentFrameTexCoordX(), icon.getCurrentFrameTexCoordY(),
			icon.getWidth(), icon.getHeight(), icon.getSheetWidth(), icon.getSheetHeight(),
			color);
	}

	private static void drawQuad(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float z, float w, float h, float u, float v, float regionW, float regionH, int texW, int texH, int color) {
		final float x1 = x0 + w;
		final float y1 = y0 + h;

		final float u0 = u / texW;
		final float u1 = (u + regionW) / texW;

		final float v0 = v / texH;
		final float v1 = (v + regionH) / texH;

		consumer.vertex(matrix, x0, y1, z).texture(u0, v1).color(color);
		consumer.vertex(matrix, x1, y1, z).texture(u1, v1).color(color);
		consumer.vertex(matrix, x1, y0, z).texture(u1, v0).color(color);
		consumer.vertex(matrix, x0, y0, z).texture(u0, v0).color(color);
	}
}
