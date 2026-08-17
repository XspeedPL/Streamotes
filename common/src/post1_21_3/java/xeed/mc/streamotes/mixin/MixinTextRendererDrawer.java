package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.font.GlyphInfo;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
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

@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
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
	protected abstract int getTextColor(@Nullable TextColor override);

	@Inject(method = "accept", at = @At("HEAD"))
	private void beforeAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		state.style = style;
		DrawerCommons.beforeAccept(state, codePoint);
	}

	@Inject(method = "accept", at = @At("TAIL"))
	private void afterAccept(int index, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
		DrawerCommons.afterAccept(state);
	}

	@ModifyVariable(method = "accept", at = @At(value = "STORE"))
	private BakedGlyph atGetGlyph(BakedGlyph glyph) {
		state.color = Streamotes.INSTANCE.getConfig().colorEmotes
			? getTextColor(state.style.getColor())
			: (color | 0xffffff);
		return GlyphCommons.atDrawGlyph(state, false, x, y, glyph);
	}

	@WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/font/GlyphInfo;getAdvance(Z)F"))
	private float atGetAdvance(GlyphInfo glyph, boolean bold, Operation<Float> original) {
		var result = DrawerCommons.atGetAdvance(state);
		return result == null ? original.call(glyph, bold) : result;
	}
}
