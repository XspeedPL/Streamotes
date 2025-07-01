package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.Glyph;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xeed.mc.streamotes.DrawerCommons;
import xeed.mc.streamotes.GlyphCommons;
import xeed.mc.streamotes.Streamotes;

@Mixin(targets = "net.minecraft.client.font.TextRenderer$Drawer")
public class MixinTextRendererDrawer {
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
	private int color;

	@Final
	@Shadow
	private float brightnessMultiplier;

	@Shadow
	float x;

	@Shadow
	float y;

	@Unique
	protected int getRenderColor(TextColor override) {
		return override != null ? ColorHelper.withAlpha(ColorHelper.getAlpha(color), ColorHelper.scaleRgb(override.getRgb(), brightnessMultiplier)) : color;
	}

	@Inject(method = "accept", at = @At("HEAD"))
	private void beforeAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		state.style = style;
		DrawerCommons.beforeAccept(state, codePoint);
	}

	@Inject(method = "accept", at = @At("TAIL"))
	private void afterAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		DrawerCommons.afterAccept(state);
	}

	@ModifyVariable(method = "accept", at = @At("STORE"))
	private BakedGlyph atGetGlyph(BakedGlyph glyph) {
		state.color = Streamotes.INSTANCE.getConfig().colorEmotes
			? getRenderColor(state.style.getColor())
			: (color | 0xffffff);
		return GlyphCommons.atDrawGlyph(state, shadow, x, y, glyph);
	}

	@WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/Glyph;getAdvance(Z)F"))
	private float atGetAdvance(Glyph glyph, boolean bold, Operation<Float> original) {
		var result = DrawerCommons.atGetAdvance(state);
		return result == null ? original.call(glyph, bold) : result;
	}
}
