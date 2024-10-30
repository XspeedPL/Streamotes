package xeed.mc.streamotes.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class MixinCommandSuggestor {
	@SuppressWarnings("unused")
	@Redirect(method = "refresh", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;suggestMatching(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
	private CompletableFuture<Suggestions> atRefreshSuggestMatching(Iterable<String> iterable, SuggestionsBuilder builder) {
		String text = builder.getRemaining().toLowerCase(Locale.ROOT);

		for (var emote : EmoticonRegistry.getEmoteNames()) {
			if (emote.toLowerCase(Locale.ROOT).contains(text))
				builder.suggest(emote);
		}

		return CommandSource.suggestMatching(iterable, builder);
	}
}
