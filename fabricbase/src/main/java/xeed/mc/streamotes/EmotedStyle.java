package xeed.mc.streamotes;

import net.minecraft.text.Style;
import org.jetbrains.annotations.Nullable;
import xeed.mc.streamotes.emoticon.Emoticon;

public interface EmotedStyle {
	@Nullable
	default Emoticon getEmote() {
		return null;
	}

	default void setEmote(@Nullable Emoticon emote) {
	}

	default Style withEmote(@Nullable Emoticon emote) {
		return Style.EMPTY;
	}
}
