package xeed.mc.streamotes.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.command.CommandSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import xeed.mc.streamotes.emoticon.EmoticonRegistry;

@Mixin(ChatInputSuggestor.class)
public abstract class MixinCommandSuggestor {
	@SuppressWarnings("unused")
	@Redirect(method = "refresh", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;suggestMatching(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
	private CompletableFuture<Suggestions> atRefreshSuggestMatching(Iterable<String> iterable, SuggestionsBuilder builder) {
		var list = new ArrayList<String>();
		iterable.forEach(list::add);
		list.addAll(EmoticonRegistry.getEmoteNames());
		return CommandSource.suggestMatching(list, builder);
	}
}
