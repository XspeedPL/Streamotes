package xeed.mc.streamotes.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xeed.mc.streamotes.ActivationOption;
import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class MixinCommandSuggestor {
	@SuppressWarnings("unused")
	@Redirect(method = "refresh", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;suggestMatching(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
	private CompletableFuture<Suggestions> atRefreshSuggestMatching(Iterable<String> iterable, SuggestionsBuilder builder) {
		var text = builder.getRemaining().toLowerCase(Locale.ROOT);
		var mode = Streamotes.INSTANCE.getConfig().activationMode;

		for (var emote : EmoticonRegistry.getEmotes()) {
			var nameLower = emote.getNameLower();
			var hasFix = nameLower.length() > 1 && nameLower.charAt(0) == ':' && nameLower.charAt(nameLower.length() - 1) == ':';
			if (mode != ActivationOption.Disabled && !hasFix) nameLower = ":" + nameLower + ":";
			if (nameLower.contains(text)) {
				var name = emote.getName();
				if (mode != ActivationOption.Disabled && !hasFix) name = ":" + name + ":";
				builder.suggest(name, emote.getPreview());
			}
		}

		return CommandSource.suggestMatching(iterable, builder);
	}
}
