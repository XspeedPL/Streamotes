package xeed.mc.streamotes;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ModConfigModel {
	private static final int SCHEMA_VERSION = 1;

	private static final ConfigClassHandler<ModConfigModel> HANDLER = ConfigClassHandler.createBuilder(ModConfigModel.class)
		.id(StreamotesCommon.IDENT)
		.serializer(config -> GsonConfigSerializerBuilder.create(config)
			.setPath(FabricLoader.getInstance().getConfigDir().resolve(StreamotesCommon.NAME + ".json5"))
			.setJson5(true)
			.build())
		.build();

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

	private static void setSelfVersion(ModConfigModel model) {
		var version = FabricLoader.getInstance().getModContainer(StreamotesCommon.NAME).orElseThrow().getMetadata().getVersion().getFriendlyString();
		int ix = version.indexOf('+');
		model.versionName = ix != -1 ? version.substring(0, ix) : version;
		model.versionCode = SCHEMA_VERSION;
	}

	public static ModConfigModel getInstance() {
		return HANDLER.instance();
	}

	public static ModConfigModel getDefaults() {
		return HANDLER.defaults();
	}

	public static void reload() {
		HANDLER.load();
		var inst = getInstance();
		setSelfVersion(inst);
		verifyChannels(inst);
	}

	public static void save() {
		verifyChannels(getInstance());
		HANDLER.save();
	}

	@SerialEntry
	public List<String> emoteChannels = List.of("Spookie_Rose", "fifigoesree", "Mifuyu");

	@SerialEntry
	public boolean twitchGlobalEmotes = true;
	@SerialEntry
	public boolean twitchSubscriberEmotes = true;

	@SerialEntry
	public boolean bttvEmotes = true;
	@SerialEntry
	public boolean bttvChannelEmotes = true;

	@SerialEntry
	public boolean ffzEmotes = true;
	@SerialEntry
	public boolean ffzChannelEmotes = true;

	@SerialEntry
	public boolean x7tvEmotes = true;
	@SerialEntry
	public boolean x7tvChannelEmotes = true;

	@SerialEntry
	public boolean colorEmotes = true;

	@SerialEntry
	public ActivationOption activationMode = ActivationOption.Optional;

	@SerialEntry
	public ReportOption errorReporting = ReportOption.Toast;

	public String versionName = "<=1.2.11";
	public int versionCode = 0;
	public boolean forceClearCache = false;
}
