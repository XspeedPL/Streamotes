package xeed.mc.streamotes.mixin;

import net.minecraft.client.util.ChatMessages;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

@SuppressWarnings("unused")
@Mixin(ChatMessages.class)
public class MixinChatMessages {
	@Inject(method = "getRenderedChatMessage", at = @At("RETURN"), cancellable = true)
	private static void maybeGetRenderedChatMessage(String input, CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(Streamotes.VALID_EMOTE_PATTERN.matcher(cir.getReturnValue()).replaceAll(x -> {
			String name = x.group();
			var emoticon = EmoticonRegistry.fromName(name);
			if (emoticon != null) Streamotes.log("Emote found: " + emoticon.getName());
			return emoticon != null ? Streamotes.CHAT_TRIGGER + emoticon.code + Streamotes.CHAT_SEPARATOR : name;
		}));
	}
}
