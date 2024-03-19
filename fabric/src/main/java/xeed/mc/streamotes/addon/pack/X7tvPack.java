package xeed.mc.streamotes.addon.pack;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import xeed.mc.streamotes.addon.TwitchEmotesAPI;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

public class X7tvPack {
	private static final String URL_TEMPLATE = "https://cdn.7tv.app/emote/{{id}}/2x.webp";
	private static final int PRIO = 7;

	public static void loadMetadata() {
		try {
			var apiURL = new URL("https://7tv.io/v3/emote-sets/global");
			try (var reader = new InputStreamReader(apiURL.openStream())) {
				var gson = new Gson();
				var emotes = gson.fromJson(reader, JsonObject.class).get("emotes").getAsJsonArray();
				for (int i = 0; i < emotes.size(); i++) {
					var entry = emotes.get(i).getAsJsonObject();
					String code = TwitchEmotesAPI.getJsonString(entry, "name");

					var emoticon = EmoticonRegistry.registerEmoticon(".7tv", code, PRIO, X7tvPack::loadEmoticonImage);
					if (emoticon != null) {
						emoticon.setLoadData(TwitchEmotesAPI.getJsonString(entry, "id"));
						emoticon.setTooltip("7tv");
					}
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
