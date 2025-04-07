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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xeed.mc.streamotes.ActivationOption;
import xeed.mc.streamotes.Compat;
import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.util.Optional;
import java.util.regex.Matcher;

@SuppressWarnings("unused")
@Mixin(ChatMessages.class)
public class MixinChatMessages {
	@Inject(method = "getRenderedChatMessage", at = @At("RETURN"), cancellable = true)
	private static void maybeGetRenderedChatMessage(String input, CallbackInfoReturnable<String> cir) {
		var mode = Streamotes.INSTANCE.getConfig().activationMode;
		cir.setReturnValue(Streamotes.EMOTE_PATTERN.matcher(cir.getReturnValue()).replaceAll(x -> {
			var name = x.group();
			var hasFix = name.length() > 1 && name.charAt(0) == ':' && name.charAt(name.length() - 1) == ':';

			var emoticon = mode != ActivationOption.Required || hasFix
				? EmoticonRegistry.fromName(name)
				: null;

			if (emoticon == null && mode != ActivationOption.Disabled && hasFix) {
				name = name.substring(1, name.length() - 1);
				emoticon = EmoticonRegistry.fromName(name);
			}
			if (emoticon != null) Streamotes.log("Emote found: " + emoticon.getName());
			return Matcher.quoteReplacement(emoticon != null ? Streamotes.CHAT_TRIGGER + emoticon.code + Streamotes.CHAT_SEPARATOR : name);
		}));
	}

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
		int startIx = string.indexOf(Streamotes.CHAT_TRIGGER), endIx;
		if (startIx == -1) {
			textCollector.add(StringVisitable.styled(string, style));
			return;
		}

		textCollector.add(StringVisitable.styled(string.substring(0, startIx), style));

		while (true) {
			endIx = string.indexOf(Streamotes.CHAT_SEPARATOR, startIx + Streamotes.CHAT_TRIGGER.length());

			var code = string.substring(startIx + Streamotes.CHAT_TRIGGER.length(), endIx);
			var icon = EmoticonRegistry.fromName(code);
			var custom = Compat.makeEmoteStyle(icon);

			textCollector.add(StringVisitable.styled(string.substring(startIx, endIx + Streamotes.CHAT_SEPARATOR.length()), style.withParent(custom)));

			startIx = string.indexOf(Streamotes.CHAT_TRIGGER, endIx + Streamotes.CHAT_SEPARATOR.length());

			if (startIx != -1)
				textCollector.add(StringVisitable.styled(string.substring(endIx + Streamotes.CHAT_SEPARATOR.length(), startIx), style));
			else break;
		}

		textCollector.add(StringVisitable.styled(string.substring(endIx + Streamotes.CHAT_SEPARATOR.length()), style));
	}
}
