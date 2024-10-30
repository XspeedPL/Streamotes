package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.Glyph;
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
import xeed.mc.streamotes.EmoteRenderInfo;
import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.util.List;

@Mixin(targets = "net.minecraft.client.font.TextRenderer$Drawer")
public abstract class MixinTextRendererDrawer implements CharacterVisitor {
	@Unique
	private final StringBuilder currentString = new StringBuilder(7);

	@Final
	@Shadow
	private boolean shadow;

	@Final
	@Shadow
	private int light;

	@Final
	@Shadow
	private Matrix4f matrix;

	@SuppressWarnings("unused")
	@Inject(method = "accept", at = @At("HEAD"))
	private void beforeAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		currentString.append(Character.toChars(codePoint));
	}

	@SuppressWarnings("unused")
	@Inject(method = "accept", at = @At("TAIL"))
	private void afterAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		var sb = currentString;

		int startIx = sb.indexOf(Streamotes.CHAT_TRIGGER);
		int endIx = startIx == -1 ? -1 : sb.indexOf(Streamotes.CHAT_SEPARATOR, startIx + Streamotes.CHAT_TRIGGER.length());

		if (endIx != -1 || (startIx == -1 && sb.length() > Streamotes.CHAT_TRIGGER.length())) sb.setLength(0);
	}

	@SuppressWarnings("unused")
	@WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	private boolean atAddGlyph(List<?> instance, Object obj, Operation<Boolean> original) { //GlyphRenderer glyphRenderer, boolean bold, boolean italic, float weight, float x, float y, Matrix4f matrix, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int light, Operation<Void> original) {
		var sb = currentString;

		int startIx = sb.indexOf(Streamotes.CHAT_TRIGGER);
		if (startIx == -1) {
			return original.call(instance, obj);
		}
		else if (!shadow) {
			int endIx = sb.indexOf(Streamotes.CHAT_SEPARATOR, startIx + Streamotes.CHAT_TRIGGER.length());
			if (endIx == -1) return true;

			var code = sb.substring(startIx + Streamotes.CHAT_TRIGGER.length(), endIx);
			var icon = EmoticonRegistry.fromName(code);
			if (icon == null) return true;

			if (icon.getTextureId() == -1) {
				icon.requestTexture();
			}
			else if (obj instanceof BakedGlyph.DrawnGlyph glyph) {
				int c = glyph.color();
				int alpha = (c >> 24) & 255;
				int red = (c >> 16) & 255;
				int green = (c >> 8) & 255;
				int blue = c & 255;
				Streamotes.RENDER_QUEUE.get().addLast(new EmoteRenderInfo(icon, glyph.x(), glyph.y(), matrix.m33(), red, green, blue, alpha, light));
			}
		}
		return true;
	}

	@SuppressWarnings("unused")
	@WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/Glyph;getAdvance(Z)F"))
	private float atGetAdvance(Glyph glyph, boolean bold, Operation<Float> original) {
		var sb = currentString;

		int startIx = sb.indexOf(Streamotes.CHAT_TRIGGER);
		if (startIx == -1) {
			return original.call(glyph, bold);
		}
		else {
			int endIx = sb.indexOf(Streamotes.CHAT_SEPARATOR, startIx + Streamotes.CHAT_TRIGGER.length());
			if (endIx == -1) return 0;

			var code = sb.substring(startIx + Streamotes.CHAT_TRIGGER.length(), endIx);
			var icon = EmoticonRegistry.fromName(code);

			return icon == null ? 8 : icon.getChatRenderWidth();
		}
	}
}
