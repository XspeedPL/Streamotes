package xeed.mc.streamotes.addon.pack;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.minecraft.util.Pair;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import xeed.mc.streamotes.addon.TwitchEmotesAPI;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

public class BTTVChannelPack {
	private static final String URL_TEMPLATE = "https://cdn.betterttv.net/emote/{{id}}/2x.webp";
	private static final int PRIO = 2;

	public static void loadMetadata(String channelName) {
		try {
			var apiURL = new URL("https://api.betterttv.net/3/cached/users/twitch/" + TwitchEmotesAPI.getChannelId(channelName));
			try (var reader = new InputStreamReader(apiURL.openStream())) {
				var gson = new Gson();
				var root = gson.fromJson(reader, JsonObject.class);
				if (root == null || !root.has("channelEmotes")) {
					throw new EmoteLoaderException("Failed to grab BTTV channel emotes (unexpected status)");
				}
				var emotes = TwitchEmotesAPI.getJsonArray(root, "channelEmotes");
				for (int i = 0; i < emotes.size(); ++i) {
					var entry = emotes.get(i).getAsJsonObject();
					String code = TwitchEmotesAPI.getJsonString(entry, "code");
					var emoticon = EmoticonRegistry.registerEmoticon(channelName, code, PRIO, BTTVChannelPack::loadEmoticonImage);
					if (emoticon != null) {
						emoticon.setLoadData(new Pair<>(TwitchEmotesAPI.getJsonString(entry, "id"), TwitchEmotesAPI.getJsonString(entry, "imageType")));
						emoticon.setTooltip(channelName + " (BTTV)");
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
		@SuppressWarnings("unchecked")
		var data = (Pair<String, String>)emoticon.getLoadData();
		try {
			TwitchEmotesAPI.loadEmoteImage(emoticon, new URI(URL_TEMPLATE.replace("{{id}}", data.getLeft())), "bttv", data.getLeft());
		}
		catch (URISyntaxException e) {
			throw new EmoteLoaderException(e);
		}
	}

}
