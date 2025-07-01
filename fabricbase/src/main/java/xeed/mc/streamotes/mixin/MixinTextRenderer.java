package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xeed.mc.streamotes.Compat;

@Mixin(TextRenderer.class)
public class MixinTextRenderer {
	@WrapOperation(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/client/font/TextHandler$WidthRetriever;)Lnet/minecraft/client/font/TextHandler;"))
	private TextHandler maybeTextHandler(TextHandler.WidthRetriever widthRetriever, Operation<TextHandler> original) {
		return original.call(new TextHandler.WidthRetriever() {
			private int currentLength = 0;

			@Override
			public float getWidth(int codePoint, Style style) {
				var icon = Compat.getEmote(style);
				if (icon == null) {
					currentLength = 0;
					return widthRetriever.getWidth(codePoint, style);
				}

				if (Character.isBmpCodePoint(codePoint)) ++currentLength;
				else if (Character.isValidCodePoint(codePoint)) currentLength += 2;

				if (currentLength >= icon.getName().length()) {
					currentLength = 0;
					return icon.getChatRenderWidth();
				}

				return 0f;
			}
		});
	}
}
