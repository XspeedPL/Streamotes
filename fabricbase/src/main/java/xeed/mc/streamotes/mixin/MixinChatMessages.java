package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.ComponentCollector;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xeed.mc.streamotes.ActivationOption;
import xeed.mc.streamotes.Compat;
import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.util.Optional;

@SuppressWarnings("unused")
@Mixin(ComponentRenderUtils.class)
public class MixinChatMessages {
	@WrapOperation(method = "wrapComponents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ComponentCollector;getResultOrEmpty()Lnet/minecraft/network/chat/FormattedText;"))
	private static FormattedText atGetCombined(ComponentCollector instance, Operation<FormattedText> original) {
		var message = original.call(instance);

		var textCollector = new ComponentCollector();
		message.visit((style, part) -> {
			maybeStyled(textCollector, part, style);
			return Optional.empty();
		}, Style.EMPTY);

		return textCollector.getResultOrEmpty();
	}

	@Unique
	private static void maybeStyled(ComponentCollector textCollector, String string, Style style) {
		var mode = Streamotes.INSTANCE.getConfig().activationMode;
		var matcher = Streamotes.EMOTE_PATTERN.matcher(string);
		int lastEnd = 0;
		while (matcher.find()) {
			var name = matcher.group();
			var hasFix = name.length() > 1 && name.charAt(0) == ':' && name.charAt(name.length() - 1) == ':';

			var emoticon = mode != ActivationOption.Required || hasFix
				? EmoticonRegistry.fromName(name)
				: null;

			if (emoticon == null && mode != ActivationOption.Disabled && hasFix) {
				emoticon = EmoticonRegistry.fromName(name.substring(1, name.length() - 1));
			}

			if (emoticon != null) {
				Streamotes.log("Emote found: " + emoticon.getName());

				int start = matcher.start();
				if (start > lastEnd) {
					textCollector.append(FormattedText.of(string.substring(lastEnd, start), style));
				}

				textCollector.append(FormattedText.of(emoticon.getName(), Compat.makeEmoteStyle(emoticon).applyTo(style)));

				lastEnd = matcher.end();
			}
		}

		if (lastEnd < string.length()) {
			textCollector.append(FormattedText.of(string.substring(lastEnd), style));
		}
	}
}
