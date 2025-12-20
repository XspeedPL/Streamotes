package xeed.mc.streamotes;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import xeed.mc.streamotes.emoticon.Emoticon;

public class EmoticonGlyph implements BakedGlyph {
	private final Emoticon icon;
	private final float x;
	private final float y;
	private final float w;
	private final float h;
	private final int color;

	private final EmoticonMetrics metrics;
	private final EmoticonDrawable drawable;

	public static EmoticonGlyph of(Emoticon icon, float x, float y, int color) {
		var client = Minecraft.getInstance();
		float lineSpacing = (float)(client.options.chatLineSpacing().get() * 4);
		float height = client.font.lineHeight + lineSpacing * 2;

		return new EmoticonGlyph(icon, x, y - lineSpacing - 1f, icon.getRenderWidth(height), height, color);
	}

	public EmoticonGlyph(Emoticon icon, float x, float y, float w, float h, int color) {
		this.icon = icon;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.color = color;
		this.metrics = new EmoticonMetrics();
		this.drawable = new EmoticonDrawable();
	}

	@Override
	public GlyphInfo info() {
		return metrics;
	}

	@Override
	public @Nullable TextRenderable createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
		return drawable;
	}

	private class EmoticonDrawable implements TextRenderable {
		@Override
		public void render(Matrix4f matrix, VertexConsumer consumer, int light, boolean noDepth) {
			if (icon.isAnimated()) icon.updateAnimation();

			GlyphCommons.drawEmote(icon, matrix, consumer, x, y, w, h, color, light);
		}

		@Override
		public RenderType renderType(Font.DisplayMode type) {
			return DrawerCommons.getLayer(icon);
		}

		@Override
		public GpuTextureView textureView() {
			return icon.getTexture().getView();
		}

		@Override
		public RenderPipeline guiPipeline() {
			return RenderPipelines.GUI_TEXTURED;
		}

		@Override
		public float left() {
			return x;
		}

		@Override
		public float top() {
			return y;
		}

		@Override
		public float right() {
			return x + w;
		}

		@Override
		public float bottom() {
			return y + h;
		}
	}

	private class EmoticonMetrics implements GlyphInfo {
		@Override
		public float getAdvance() {
			return icon.getWidth();
		}

		@Override
		public float getAdvance(boolean bold) {
			return icon.getWidth();
		}

		@Override
		public float getBoldOffset() {
			return 0f;
		}

		@Override
		public float getShadowOffset() {
			return 0f;
		}
	}
}
