package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xeed.mc.streamotes.DrawerCommons;

@Mixin(targets = "net.minecraft.client.font.TextRenderer$Drawer")
public abstract class MixinTextRendererDrawer implements CharacterVisitor {
	@Unique
	private final StringBuilder currentString = new StringBuilder(7);

	@Final
	@Shadow
	private boolean shadow;

	@SuppressWarnings("unused")
	@Inject(method = "accept", at = @At("HEAD"))
	private void beforeAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		currentString.append(Character.toChars(codePoint));
	}

	@SuppressWarnings("unused")
	@Inject(method = "accept", at = @At("TAIL"))
	private void afterAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		DrawerCommons.afterAccept(currentString);
	}

	@SuppressWarnings("unused")
	@WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;drawGlyph(Lnet/minecraft/client/font/GlyphRenderer;ZZFFFLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumer;FFFFI)V"))
	private void atDrawGlyph(TextRenderer renderer, GlyphRenderer glyphRenderer, boolean bold, boolean italic, float weight, float x, float y, Matrix4f matrix, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int light, Operation<Void> original) {
		int c = (int)(red * 255) + ((int)(green * 255) << 8) + ((int)(blue * 255) << 16) + ((int)(alpha * 255) << 24);
		if (!DrawerCommons.atDrawGlyph(currentString, shadow, x, y, matrix, c)) {
			original.call(renderer, glyphRenderer, bold, italic, weight, x, y, matrix, vertexConsumer, red, green, blue, alpha, light);
		}
	}

	@SuppressWarnings("unused")
	@WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/Glyph;getAdvance(Z)F"))
	private float atGetAdvance(Glyph glyph, boolean bold, Operation<Float> original) {
		var result = DrawerCommons.atGetAdvance(currentString);
		return result == null ? original.call(glyph, bold) : result;
	}
}
