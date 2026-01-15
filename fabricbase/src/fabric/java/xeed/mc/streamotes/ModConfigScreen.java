package xeed.mc.streamotes;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

@SuppressWarnings("unused")
public class ModConfigScreen implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ConfigScreenBuilder::createConfigScreen;
	}
}
