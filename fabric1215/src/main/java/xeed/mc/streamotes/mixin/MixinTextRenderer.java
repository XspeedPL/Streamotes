package xeed.mc.streamotes.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xeed.mc.streamotes.Compat;
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

			while (!queue.isEmpty()) {
				var info = queue.removeFirst();
				var icon = info.icon();

				if (icon.isAnimated()) icon.updateAnimation();

				var consumer = vertexConsumers.getBuffer(Compat.LAYER.apply(icon));

				drawTexture(consumer, matrix, info.x(), info.y() - lineSpacing - 1F, info.z() + 1F,
					icon.getRenderWidth(height), height,
					icon.getCurrentFrameTexCoordX(), icon.getCurrentFrameTexCoordY(),
					icon.getWidth(), icon.getHeight(), icon.getSheetWidth(), icon.getSheetHeight(),
					info.alpha(), info.light());
			}
		}
	}

	@Unique
	private static void drawTexture(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float z, float w, float h, float u, float v, float regionW, float regionH, int texW, int texH, float a, int l) {
		final float x1 = x0 + w;
		final float y1 = y0 + h;

		final float u0 = u / texW;
		final float u1 = (u + regionW) / texW;

		final float v0 = v / texH;
		final float v1 = (v + regionH) / texH;

		consumer.vertex(matrix, x0, y1, z).texture(u0, v1).color(1f, 1f, 1f, a).light(l);
		consumer.vertex(matrix, x1, y1, z).texture(u1, v1).color(1f, 1f, 1f, a).light(l);
		consumer.vertex(matrix, x1, y0, z).texture(u1, v0).color(1f, 1f, 1f, a).light(l);
		consumer.vertex(matrix, x0, y0, z).texture(u0, v0).color(1f, 1f, 1f, a).light(l);
	}
}
