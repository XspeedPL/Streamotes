package xeed.mc.streamotes;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum ReportOption implements NameableEnum {
	Toast,
	Chat,
	None;

	@Override
	public Component getDisplayName() {
		return Component.translatable("text.config.streamotes.value.report." + name().toLowerCase(Locale.ROOT));
	}
}
