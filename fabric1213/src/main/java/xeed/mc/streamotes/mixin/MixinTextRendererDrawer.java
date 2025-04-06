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
import xeed.mc.streamotes.DrawerCommons;

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
	private Matrix4f matrix;

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
	@WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	private boolean atAddGlyph(List<?> instance, Object obj, Operation<Boolean> original) {
		if (obj instanceof BakedGlyph.DrawnGlyph glyph && !DrawerCommons.atDrawGlyph(currentString, shadow, glyph.x(), glyph.y(), matrix, glyph.color())) {
			return original.call(instance, obj);
		}
		return true;
	}

	@SuppressWarnings("unused")
	@WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/Glyph;getAdvance(Z)F"))
	private float atGetAdvance(Glyph glyph, boolean bold, Operation<Float> original) {
		var result = DrawerCommons.atGetAdvance(currentString);
		return result == null ? original.call(glyph, bold) : result;
	}
}
