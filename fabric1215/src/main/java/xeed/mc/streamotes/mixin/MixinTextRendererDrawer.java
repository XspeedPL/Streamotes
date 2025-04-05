package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.EmptyBakedGlyph;
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

@Mixin(targets = "net.minecraft.client.font.TextRenderer$Drawer")
public abstract class MixinTextRendererDrawer implements CharacterVisitor {
	@Unique
	private final StringBuilder currentString = new StringBuilder(7);

	@Final
	@Shadow
	private int light;

	@Final
	@Shadow
	private Matrix4f matrix;

	@Shadow
	float x;

	@Shadow
	float y;

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
	@Inject(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/Glyph;getShadowOffset()F", shift = At.Shift.AFTER))
	private void atAccept(int i, Style style, int j, CallbackInfoReturnable<Boolean> cir, @Local LocalRef<BakedGlyph> glyphRef, @Local(ordinal = 2) int color) {
		var sb = currentString;

		int startIx = sb.indexOf(Streamotes.CHAT_TRIGGER);
		if (startIx != -1) {
			glyphRef.set(EmptyBakedGlyph.INSTANCE);

			int endIx = sb.indexOf(Streamotes.CHAT_SEPARATOR, startIx + Streamotes.CHAT_TRIGGER.length());
			if (endIx == -1) return;

			var code = sb.substring(startIx + Streamotes.CHAT_TRIGGER.length(), endIx);
			var icon = EmoticonRegistry.fromName(code);
			if (icon == null) return;

			if (icon.getTexture().isLoaded()) {
				int alpha = (color >> 24) & 255;
				int red = (color >> 16) & 255;
				int green = (color >> 8) & 255;
				int blue = color & 255;
				Streamotes.RENDER_QUEUE.get().addLast(new EmoteRenderInfo(icon, x, y, matrix.m33(), red, green, blue, alpha, light));
			}
			else {
				icon.requestTexture();
			}
		}
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
