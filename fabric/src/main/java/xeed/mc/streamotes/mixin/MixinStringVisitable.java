package xeed.mc.streamotes.mixin;

import net.minecraft.client.util.TextCollector;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

@SuppressWarnings("unused")
@Mixin(StringVisitable.class)
public interface MixinStringVisitable {
	@Inject(method = "styled", at = @At("HEAD"), cancellable = true)
	private static void maybeStyled(String string, Style style, CallbackInfoReturnable<StringVisitable> cir) {
		int startIx = string.indexOf(Streamotes.CHAT_TRIGGER), endIx;
		if (startIx == -1) return;

		var textCollector = new TextCollector();
		textCollector.add(actuallyStyled(string.substring(0, startIx), style));

		while (true) {
			endIx = string.indexOf(Streamotes.CHAT_SEPARATOR, startIx + Streamotes.CHAT_TRIGGER.length());

			var code = string.substring(startIx + Streamotes.CHAT_TRIGGER.length(), endIx);
			var icon = EmoticonRegistry.fromName(code);
			var custom = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, icon.getName()))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, icon.getTooltip()));

			textCollector.add(actuallyStyled(string.substring(startIx, endIx + Streamotes.CHAT_SEPARATOR.length()), style.withParent(custom)));

			startIx = string.indexOf(Streamotes.CHAT_TRIGGER, endIx + Streamotes.CHAT_SEPARATOR.length());

			if (startIx != -1) textCollector.add(actuallyStyled(string.substring(endIx + Streamotes.CHAT_SEPARATOR.length(), startIx), style));
			else break;
		}

		textCollector.add(actuallyStyled(string.substring(endIx + Streamotes.CHAT_SEPARATOR.length()), style));

		cir.setReturnValue(textCollector.getCombined());
	}

	private static StringVisitable actuallyStyled(String string, Style style) {
		return new StringVisitable() {
			public <T> Optional<T> visit(Visitor<T> visitor) {
				return visitor.accept(string);
			}

			public <T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style stylex) {

				return styledVisitor.accept(style.withParent(stylex), string);
			}
		};
	}
}
