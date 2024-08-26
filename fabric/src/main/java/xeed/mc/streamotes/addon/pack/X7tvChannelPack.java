package xeed.mc.streamotes.addon.pack;

import xeed.mc.streamotes.addon.TwitchEmotesAPI;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

public class X7tvChannelPack {
	private static final String URL_TEMPLATE = "https://cdn.7tv.app/emote/{{id}}/2x.webp";
	private static final int PRIO = 6;

	public static void loadMetadata(String channelName) {
		try {
			String channelId = TwitchEmotesAPI.getChannelId(channelName);

			var apiURL = TwitchEmotesAPI.getURL("https://7tv.io/v3/users/twitch/" + channelId);

			var obj = TwitchEmotesAPI.getJsonObj(apiURL);
			if (obj == null) return;
			var elem = obj.get("emote_set");
			if (elem == null) return;
			obj = elem.getAsJsonObject();
			if (obj == null) return;
			elem = obj.get("emotes");
			if (elem == null) return;
			var emotes = elem.getAsJsonArray();
			for (int i = 0; i < emotes.size(); ++i) {
				var entry = emotes.get(i).getAsJsonObject();
				String code = TwitchEmotesAPI.getJsonString(entry, "name");

				var emoticon = EmoticonRegistry.registerEmoticon(channelName, code, PRIO, X7tvChannelPack::loadEmoticonImage);
				if (emoticon != null) {
					emoticon.setLoadData(TwitchEmotesAPI.getJsonString(entry, "id"));
					emoticon.setTooltip(channelName + " (7tv)");
				}
			}
		}
		catch (FileNotFoundException ignored) {
		}
		catch (Exception e) {
			throw new EmoteLoaderException("Unhandled exception", e);
		}
	}

	private static void loadEmoticonImage(Emoticon emoticon) {
		var data = (String)emoticon.getLoadData();
		try {
			TwitchEmotesAPI.loadEmoteImage(emoticon, new URI(URL_TEMPLATE.replace("{{id}}", data)), "7tv", data);
		}
		catch (URISyntaxException e) {
			throw new EmoteLoaderException(e);
		}
	}
}
