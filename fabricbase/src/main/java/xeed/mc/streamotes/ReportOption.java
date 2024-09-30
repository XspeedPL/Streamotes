package xeed.mc.streamotes;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.text.Text;

import java.util.Locale;

public enum ReportOption implements NameableEnum {
	Toast,
	Chat,
	None;

	@Override
	public Text getDisplayName() {
		return Text.translatable("text.config.streamotes.value.report." + name().toLowerCase(Locale.ROOT));
	}
}
