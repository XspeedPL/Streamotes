package xeed.mc.streamotes.addon.pack;

import net.minecraft.util.Pair;
import xeed.mc.streamotes.addon.TwitchEmotesAPI;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

public class FFZChannelPack {
	private static final int PRIO = 4;

	public static void loadMetadata(String channelName) {
		try {
			var apiURL = TwitchEmotesAPI.getURL("https://api.frankerfacez.com/v1/room/" + channelName.toLowerCase());

			var root = TwitchEmotesAPI.getJsonObj(apiURL.openStream());
			if (root == null) {
				throw new EmoteLoaderException("Failed to grab FrankerFaceZ channel emotes for " + channelName);
			}

			int setId = root.getAsJsonObject("room").get("set").getAsInt();
			var set = root.getAsJsonObject("sets").getAsJsonObject(String.valueOf(setId));
			var emoticons = set.getAsJsonArray("emoticons");
			for (int j = 0; j < emoticons.size(); j++) {
				var emoticonObject = emoticons.get(j).getAsJsonObject();

				String code = emoticonObject.get("name").getAsString();
				var urls = emoticonObject.getAsJsonObject("urls");
				String url = (urls.has("2") ? urls.get("2") : urls.get("1")).getAsString();
				String id = emoticonObject.get("id").getAsString();

				var emoticon = EmoticonRegistry.registerEmoticon(channelName, code, PRIO, FFZChannelPack::loadEmoticonImage);
				if (emoticon != null) {
					emoticon.setLoadData(new Pair<>(id, url));
					emoticon.setTooltip(channelName + " (FFZ)");
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
			TwitchEmotesAPI.loadEmoteImage(emoticon, new URI(data.getRight()), "ffz", data.getLeft());
		}
		catch (URISyntaxException e) {
			throw new EmoteLoaderException(e);
		}
	}
}
