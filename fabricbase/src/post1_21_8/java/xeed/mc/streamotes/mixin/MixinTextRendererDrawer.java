package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.GlyphMetrics;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import org.jetbrains.annotations.Nullable;
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
public abstract class MixinTextRendererDrawer {
	@Unique
	private final DrawerCommons.State state = new DrawerCommons.State();

	@Final
	@Shadow
	private int color;

	@Shadow
	float x;

	@Shadow
	float y;

	@Shadow
	protected abstract int getRenderColor(@Nullable TextColor override);

	@Inject(method = "accept(ILnet/minecraft/text/Style;I)Z", at = @At("HEAD"))
	private void beforeAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		state.style = style;
		DrawerCommons.beforeAccept(state, codePoint);
	}

	@Inject(method = "accept(ILnet/minecraft/text/Style;I)Z", at = @At("TAIL"))
	private void afterAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		DrawerCommons.afterAccept(state);
	}

	@ModifyVariable(method = "accept(ILnet/minecraft/text/Style;I)Z", at = @At(value = "STORE"))
	private BakedGlyph atGetGlyph(BakedGlyph glyph) {
		state.color = Streamotes.INSTANCE.getConfig().colorEmotes
			? getRenderColor(state.style.getColor())
			: (color | 0xffffff);
		return GlyphCommons.atDrawGlyph(state, false, x, y, glyph);
	}

	@WrapOperation(method = "accept(ILnet/minecraft/text/Style;Lnet/minecraft/client/font/BakedGlyph;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/GlyphMetrics;getAdvance(Z)F"))
	private float atGetAdvance(GlyphMetrics metrics, boolean bold, Operation<Float> original) {
		var result = DrawerCommons.atGetAdvance(state);
		return result == null ? original.call(metrics, bold) : result;
	}
}
