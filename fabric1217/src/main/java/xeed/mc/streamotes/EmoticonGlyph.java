package xeed.mc.streamotes;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.RenderPipelines;
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
		var layerSet = new GlyphRenderTypes(layer, layer, layer, RenderPipelines.TEXT);

		var client = Minecraft.getInstance();
		float lineSpacing = (float)(client.options.chatLineSpacing().get() * 4);
		float height = client.font.lineHeight + lineSpacing * 2;

		return new EmoticonGlyph(layerSet, icon, x, y - lineSpacing - 1f, icon.getRenderWidth(height), height, color);
	}

	public EmoticonGlyph(GlyphRenderTypes layerSet, Emoticon icon, float x, float y, float w, float h, int color) {
		super(layerSet, icon.getTexture().getView(), 0f, 1f, 0f, 1f, x, x + w, y, y + h);
		this.icon = icon;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.color = color;
	}

	@Override
	public void renderChar(BakedGlyph.GlyphInstance glyph, Matrix4f matrix, VertexConsumer consumer, int light, boolean fixedZ) {
		GlyphCommons.drawEmote(icon, matrix, consumer, x, y, w, h, color, light);
	}
}
