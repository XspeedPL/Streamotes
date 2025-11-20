package xeed.mc.streamotes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.client.gl.RenderPipelines;
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
		var layerSet = new TextRenderLayerSet(layer, layer, layer, RenderPipelines.RENDERTYPE_TEXT);

		var client = MinecraftClient.getInstance();
		float lineSpacing = (float)(client.options.getChatLineSpacing().getValue() * 4);
		float height = client.textRenderer.fontHeight + lineSpacing * 2;

		return new EmoticonGlyph(layerSet, icon, x, y - lineSpacing - 1f, icon.getRenderWidth(height), height, color);
	}

	public EmoticonGlyph(TextRenderLayerSet layerSet, Emoticon icon, float x, float y, float w, float h, int color) {
		super(layerSet, icon.getTexture().getView(), 0f, 1f, 0f, 1f, x, x + w, y, y + h);
		this.icon = icon;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.color = color;
	}

	@Override
	public void draw(BakedGlyph.DrawnGlyph glyph, Matrix4f matrix, VertexConsumer consumer, int light, boolean fixedZ) {
		GlyphCommons.drawEmote(icon, matrix, consumer, x, y, w, h, color, light);
	}
}
