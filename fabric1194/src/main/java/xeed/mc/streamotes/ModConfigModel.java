package xeed.mc.streamotes;

import dev.isxander.yacl3.config.ConfigEntry;
import dev.isxander.yacl3.config.GsonConfigInstance;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ModConfigModel {
	@SuppressWarnings("deprecation")
	private static final GsonConfigInstance<ModConfigModel> HANDLER = new GsonConfigInstance<>(ModConfigModel.class, FabricLoader.getInstance().getConfigDir().resolve(StreamotesCommon.NAME + ".json5"));

	private static void verifyChannels(ModConfigModel model) {
		var list = new ArrayList<>(model.emoteChannels);
		int size = list.size();
		var set = new HashSet<String>(size);

		for (int i = list.size() - 1; i >= 0; --i) {
			var item = list.get(i);
			if (item == null || item.length() < 3 || item.length() > 25 || !StreamotesCommon.VALID_CHANNEL_PATTERN.matcher(item).find() || !set.add(item)) {
				list.remove(i);
			}
		}

		if (size != list.size()) {
			model.emoteChannels = list;
		}
	}

	public static ModConfigModel getInstance() {
		return HANDLER.getConfig();
	}

	public static ModConfigModel getDefaults() {
		return HANDLER.getDefaults();
	}

	public static void reload() {
		HANDLER.load();
		verifyChannels(getInstance());
	}

	public static void save() {
		verifyChannels(getInstance());
		HANDLER.save();
	}

	@ConfigEntry
	public List<String> emoteChannels = List.of("Spookie_Rose", "fifigoesree", "Mifuyu");

	@ConfigEntry
	public boolean twitchGlobalEmotes = true;
	@ConfigEntry
	public boolean twitchSubscriberEmotes = true;

	@ConfigEntry
	public boolean bttvEmotes = true;
	@ConfigEntry
	public boolean bttvChannelEmotes = true;

	@ConfigEntry
	public boolean ffzEmotes = true;
	@ConfigEntry
	public boolean ffzChannelEmotes = true;

	@ConfigEntry
	public boolean x7tvEmotes = true;
	@ConfigEntry
	public boolean x7tvChannelEmotes = true;
	
	public boolean forceClearCache = false;

	@ConfigEntry
	public ActivationOption activationMode = ActivationOption.Optional;

	@ConfigEntry
	public ReportOption errorReporting = ReportOption.Toast;
}
