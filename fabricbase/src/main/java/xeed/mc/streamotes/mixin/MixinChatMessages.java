package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.client.util.TextCollector;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xeed.mc.streamotes.ActivationOption;
import xeed.mc.streamotes.Compat;
import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.util.Optional;

@SuppressWarnings("unused")
@Mixin(ChatMessages.class)
public class MixinChatMessages {
	@WrapOperation(method = "breakRenderedChatMessageLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/TextCollector;getCombined()Lnet/minecraft/text/StringVisitable;"))
	private static StringVisitable atGetCombined(TextCollector instance, Operation<StringVisitable> original) {
		var message = original.call(instance);

		var textCollector = new TextCollector();
		message.visit((style, part) -> {
			maybeStyled(textCollector, part, style);
			return Optional.empty();
		}, Style.EMPTY);

		return textCollector.getCombined();
	}

	@Unique
	private static void maybeStyled(TextCollector textCollector, String string, Style style) {
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
					textCollector.add(StringVisitable.styled(string.substring(lastEnd, start), style));
				}

				var custom = Compat.makeEmoteStyle(emoticon);
				custom.setEmote(emoticon);
				textCollector.add(StringVisitable.styled(emoticon.code, style.withParent(custom)));

				lastEnd = matcher.end();
			}
		}

		if (lastEnd < string.length()) {
			textCollector.add(StringVisitable.styled(string.substring(lastEnd), style));
		}
	}
}
