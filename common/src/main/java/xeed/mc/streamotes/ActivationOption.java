package xeed.mc.streamotes;

import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum ActivationOption implements NameableEnum {
	Required,
	Optional,
	Disabled;

	@Override
	public Component getDisplayName() {
		return Component.translatable("text.config.streamotes.value.activation." + name().toLowerCase(Locale.ROOT));
	}
}
