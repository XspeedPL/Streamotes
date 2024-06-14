package xeed.mc.streamotes.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.text.OrderedText;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.WrapTextHandler;

@Mixin(TextRenderer.class)
public abstract class MixinTextRenderer {
	@SuppressWarnings("unused")
	@Redirect(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/client/font/TextHandler$WidthRetriever;)Lnet/minecraft/client/font/TextHandler;"))
	private TextHandler maybeTextHandler(TextHandler.WidthRetriever widthRetriever) {
		return new WrapTextHandler(widthRetriever);
	}

	@SuppressWarnings("unused")
	@Inject(method = "draw(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I", at = @At("TAIL"))
	private void afterDraw(OrderedText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumers, TextRenderer.TextLayerType layerType, int backgroundColor, int light, CallbackInfoReturnable<Integer> cir) {
		var queue = Streamotes.RENDER_QUEUE.get();

		if (!queue.isEmpty()) {
			var client = MinecraftClient.getInstance();
			float lineSpacing = (float)(client.options.getChatLineSpacing().getValue() * 4);
			float height = client.textRenderer.fontHeight + lineSpacing * 2;

			RenderSystem.setShader(GameRenderer::getPositionTexProgram);

			while (!queue.isEmpty()) {
				var info = queue.removeFirst();
				var icon = info.icon();

				if (icon.isAnimated()) icon.updateAnimation();

				RenderSystem.setShaderTexture(0, icon.getTextureId());

				drawTexture(matrix, info.x(), info.y() - lineSpacing - 1,
					icon.getRenderWidth(height), height,
					icon.getCurrentFrameTexCoordX(), icon.getCurrentFrameTexCoordY(),
					icon.getWidth(), icon.getHeight(), icon.getSheetWidth(), icon.getSheetHeight());
			}
		}
	}

	@Unique
	private static void drawTexture(Matrix4f matrix, float x, float y, float width, float height, float u, float v, float regionWidth, float regionHeight, int textureWidth, int textureHeight) {
		drawTexture2(matrix, x, x + width, y, y + height, regionWidth, regionHeight, u, v, textureWidth, textureHeight);
	}

	@Unique
	private static void drawTexture2(Matrix4f matrix, float x0, float x1, float y0, float y1, float regionWidth, float regionHeight, float u, float v, int textureWidth, int textureHeight) {
		drawTexturedQuad(matrix, x0, x1, y0, y1, (u + 0.0F) / (float)textureWidth, (u + regionWidth) / textureWidth, (v + 0.0F) / textureHeight, (v + regionHeight) / textureHeight);
	}

	@Unique
	private static void drawTexturedQuad(Matrix4f matrix, float x0, float x1, float y0, float y1, float u0, float u1, float v0, float v1) {
		var bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
		bufferBuilder.vertex(matrix, x0, y1, 666).texture(u0, v1);
		bufferBuilder.vertex(matrix, x1, y1, 666).texture(u1, v1);
		bufferBuilder.vertex(matrix, x1, y0, 666).texture(u1, v0);
		bufferBuilder.vertex(matrix, x0, y0, 666).texture(u0, v0);
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
	}
}
