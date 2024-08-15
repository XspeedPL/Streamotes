package xeed.mc.streamotes.addon.pack;

import xeed.mc.streamotes.addon.TwitchEmotesAPI;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

public class X7tvPack {
	private static final String URL_TEMPLATE = "https://cdn.7tv.app/emote/{{id}}/2x.webp";
	private static final int PRIO = 7;

	public static void loadMetadata() {
		try {
			var apiURL = TwitchEmotesAPI.getURL("https://7tv.io/v3/emote-sets/global");

			var emotes = TwitchEmotesAPI.getJsonObj(apiURL.openStream()).get("emotes").getAsJsonArray();
			for (int i = 0; i < emotes.size(); i++) {
				var entry = emotes.get(i).getAsJsonObject();
				var code = TwitchEmotesAPI.getJsonString(entry, "name");

				var emoticon = EmoticonRegistry.registerEmoticon(".7tv", code, PRIO, X7tvPack::loadEmoticonImage);
				if (emoticon != null) {
					emoticon.setLoadData(TwitchEmotesAPI.getJsonString(entry, "id"));
					emoticon.setTooltip("7tv");
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
