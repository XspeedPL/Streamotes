package xeed.mc.streamotes;

import net.minecraft.client.font.TextHandler;
import net.minecraft.text.Style;

import xeed.mc.streamotes.emoticon.EmoticonRegistry;

public class WrapTextHandler extends TextHandler {
	public WrapTextHandler(WidthRetriever widthRetriever) {
		super(new WidthRetriever() {
			private final StringBuilder currentString = new StringBuilder(7);

			@Override
			public float getWidth(int codePoint, Style style) {
				currentString.append(Character.toChars(codePoint));
				if (currentString.length() >= Streamotes.CHAT_TRIGGER.length()) {
					int startIx = currentString.indexOf(Streamotes.CHAT_TRIGGER);
					if (startIx != -1) {
						int endIx = currentString.indexOf(Streamotes.CHAT_SEPARATOR, startIx + Streamotes.CHAT_TRIGGER.length());
						if (endIx != -1) {
							var code = currentString.substring(startIx + Streamotes.CHAT_TRIGGER.length(), endIx);
							currentString.setLength(0);

							var icon = EmoticonRegistry.fromName(code);
							return icon == null ? 8 : icon.getChatRenderWidth();
						}
						else {
							return 0;
						}
					}
					else {
						currentString.setLength(0);
					}
				}
				return widthRetriever.getWidth(codePoint, style);
			}
		});
	}
}
