package xeed.mc.streamotes.addon.pack;

import net.minecraft.util.Pair;
import xeed.mc.streamotes.addon.TwitchEmotesAPI;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

public class BTTVPack {
	private static final String URL_TEMPLATE = "https://cdn.betterttv.net/emote/{{id}}/2x.webp";
	private static final int PRIO = 3;

	public static void loadMetadata() {
		try {
			var apiURL = TwitchEmotesAPI.getURL("https://api.betterttv.net/3/cached/emotes/global");

			var emotes = TwitchEmotesAPI.getJsonArr(apiURL.openStream());
			for (int i = 0; i < emotes.size(); i++) {
				var entry = emotes.get(i).getAsJsonObject();
				String code = TwitchEmotesAPI.getJsonString(entry, "code");
				var emoticon = EmoticonRegistry.registerEmoticon(".BTTV", code, PRIO, BTTVPack::loadEmoticonImage);
				if (emoticon != null) {
					emoticon.setLoadData(new Pair<>(TwitchEmotesAPI.getJsonString(entry, "id"), TwitchEmotesAPI.getJsonString(entry, "imageType")));
					emoticon.setTooltip("BTTV");
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
