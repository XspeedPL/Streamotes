package xeed.mc.streamotes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xeed.mc.streamotes.ActivationOption;
import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.CommandSuggestions;

@Mixin(CommandSuggestions.class)
public class MixinCommandSuggestor {
	@WrapOperation(method = "updateCommandInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/SharedSuggestionProvider;suggest(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
	private CompletableFuture<Suggestions> atRefreshSuggestMatching(Iterable<String> candidates, SuggestionsBuilder builder, Operation<CompletableFuture<Suggestions>> original) {
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

		return original.call(candidates, builder);
	}
}
