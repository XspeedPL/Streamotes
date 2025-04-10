package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.EmptyGlyphRenderer;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xeed.mc.streamotes.ColorHelper;
import xeed.mc.streamotes.DrawerCommons;
import xeed.mc.streamotes.Streamotes;

@Mixin(targets = "net.minecraft.client.font.TextRenderer$Drawer")
public abstract class MixinTextRendererDrawer {
	@Unique
	private final DrawerCommons.State state = new DrawerCommons.State();

	@Final
	@Shadow
	private boolean shadow;

	@Final
	@Shadow
	private Matrix4f matrix;

	@Final
	@Shadow
	private float red;

	@Final
	@Shadow
	private float green;

	@Final
	@Shadow
	private float blue;

	@Final
	@Shadow
	private float alpha;

	@Final
	@Shadow
	private float brightnessMultiplier;

	@Shadow
	float x;

	@Shadow
	float y;

	@Unique
	protected int getRenderColor(TextColor override) {
		int alpha = (int)(this.alpha * 255);
		return override != null
			? ColorHelper.withAlpha(alpha, ColorHelper.scaleRgb(override.getRgb(), this.brightnessMultiplier))
			: ColorHelper.getArgb(alpha, (int)(red * 255), (int)(green * 255), (int)(blue * 255));
	}

	@SuppressWarnings("unused")
	@Inject(method = "accept", at = @At("HEAD"))
	private void beforeAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		state.style = style;
		DrawerCommons.beforeAccept(state, codePoint);
	}

	@SuppressWarnings("unused")
	@Inject(method = "accept", at = @At("TAIL"))
	private void afterAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		DrawerCommons.afterAccept(state);
	}

	@ModifyVariable(method = "accept", at = @At("STORE"))
	private GlyphRenderer atGetGlyph(GlyphRenderer glyph) {
		int c = Streamotes.INSTANCE.getConfig().colorEmotes
			? getRenderColor(state.style.getColor())
			: (((int)(alpha * 255) << 24) | 0xffffff);
		return DrawerCommons.atDrawGlyph(state, shadow, x, y, matrix, c) ? EmptyGlyphRenderer.INSTANCE : glyph;
	}

	@SuppressWarnings("unused")
	@WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/Glyph;getAdvance(Z)F"))
	private float atGetAdvance(Glyph glyph, boolean bold, Operation<Float> original) {
		var result = DrawerCommons.atGetAdvance(state);
		return result == null ? original.call(glyph, bold) : result;
	}
}
