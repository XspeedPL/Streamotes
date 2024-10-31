package xeed.mc.streamotes;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.text.Text;

import java.util.Locale;

public enum ActivationOption implements NameableEnum {
	Required,
	Optional,
	Disabled;

	@Override
	public Text getDisplayName() {
		return Text.translatable("text.config.streamotes.value.activation." + name().toLowerCase(Locale.ROOT));
	}
}
