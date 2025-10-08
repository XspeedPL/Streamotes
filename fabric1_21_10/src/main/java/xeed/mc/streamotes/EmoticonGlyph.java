package xeed.mc.streamotes;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.GlyphMetrics;
import net.minecraft.client.font.TextDrawable;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Style;
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
		var client = MinecraftClient.getInstance();
		float lineSpacing = (float)(client.options.getChatLineSpacing().getValue() * 4);
		float height = client.textRenderer.fontHeight + lineSpacing * 2;

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

	@Override
	public GlyphMetrics getMetrics() {
		return metrics;
	}

	@Override
	public @Nullable TextDrawable create(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
		return drawable;
	}

	private class EmoticonDrawable implements TextDrawable {
		@Override
		public void render(Matrix4f matrix, VertexConsumer consumer, int light, boolean noDepth) {
			if (icon.isAnimated()) icon.updateAnimation();

			drawQuad(consumer, matrix, x, y, 0f, w, h,
				icon.getCurrentFrameTexCoordX(), icon.getCurrentFrameTexCoordY(),
				icon.getWidth(), icon.getHeight(), icon.getSheetWidth(), icon.getSheetHeight(),
				color);
		}

		@Override
		public RenderLayer getRenderLayer(TextRenderer.TextLayerType type) {
			return DrawerCommons.getLayer(icon);
		}

		@Override
		public GpuTextureView textureView() {
			return icon.getTexture().getView();
		}

		@Override
		public RenderPipeline getPipeline() {
			return RenderPipelines.GUI_TEXTURED;
		}

		@Override
		public float getEffectiveMinX() {
			return x;
		}

		@Override
		public float getEffectiveMinY() {
			return y;
		}

		@Override
		public float getEffectiveMaxX() {
			return x + w;
		}

		@Override
		public float getEffectiveMaxY() {
			return y + h;
		}
	}

	private class EmoticonMetrics implements GlyphMetrics {
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
