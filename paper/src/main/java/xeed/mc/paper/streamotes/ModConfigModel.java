package xeed.mc.paper.streamotes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("unused")
public class ModConfigModel {
	public static void verifyChannels(ModConfigModel model) {
		var list = new ArrayList<>(model.emoteChannels);
		int size = list.size();
		var set = new HashSet<String>(size);

		for (int i = list.size() - 1; i >= 0; --i) {
			var item = list.get(i);
			if (item == null || item.length() < 4 || item.length() > 25 || !Streamotes.VALID_CHANNEL_PATTERN.matcher(item).find() || !set.add(item)) {
				list.remove(i);
			}
		}

		if (size != list.size()) {
			model.emoteChannels = list;
		}
	}

	public List<String> emoteChannels = List.of("Spookie_Rose", "fifigoesree", "Mifuyu");

	public boolean twitchGlobalEmotes = true;
	public boolean twitchSubscriberEmotes = true;

	public boolean bttvEmotes = true;
	public boolean bttvChannelEmotes = true;

	public boolean ffzEmotes = true;
	public boolean ffzChannelEmotes = true;

	public boolean x7tvEmotes = true;
	public boolean x7tvChannelEmotes = true;

	public boolean colorEmotes = true;

	public ActivationOption activationMode = ActivationOption.Optional;

	public String versionName = "<=1.2.11";
	public int versionCode = 0;
	public boolean forceClearCache = false;
}
