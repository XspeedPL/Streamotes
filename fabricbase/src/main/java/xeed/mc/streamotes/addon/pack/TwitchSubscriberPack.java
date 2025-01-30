package xeed.mc.streamotes.addon.pack;

import xeed.mc.streamotes.addon.TwitchEmotesAPI;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

public class TwitchSubscriberPack {
	private static final String EMOTE_URL_TEMPLATE = "https://static-cdn.jtvnw.net/emoticons/v2/{{id}}/default/dark/2.0";
	private static final int PRIO = 0;

	public static void loadMetadata(String channel) {
		try {
			String channelId = TwitchEmotesAPI.getChannelId(channel);

			var apiURL = TwitchEmotesAPI.getURL("https://twitchemotes.com/channels/" + channelId);
			try (var reader = new BufferedReader(new InputStreamReader(TwitchEmotesAPI.openStream(apiURL)))) {
				String line;
				while ((line = reader.readLine()) != null) {
					int ixStart = line.indexOf(" class=\"emote expandable-emote\" ");
					if (ixStart == -1) continue;

					String prefix = "data-image-id=\"";
					ixStart = line.indexOf(prefix);
					if (ixStart == -1) continue;

					int ixEnd = line.indexOf("\"", ixStart + prefix.length());
					if (ixEnd == -1) continue;

					String id = line.substring(ixStart + prefix.length(), ixEnd);

					prefix = "data-regex=\"";
					ixStart = line.indexOf(prefix);
					if (ixStart == -1) continue;

					ixEnd = line.indexOf("\"", ixStart + prefix.length());
					if (ixEnd == -1) continue;

					String code = line.substring(ixStart + prefix.length(), ixEnd);

					var emoticon = EmoticonRegistry.registerEmoticon(channel, code, PRIO, TwitchSubscriberPack::loadEmoticonImage);
					if (emoticon != null) {
						emoticon.setLoadData(id);
						emoticon.setTooltip(channel);
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
		String data = (String)emoticon.getLoadData();
		try {
			TwitchEmotesAPI.loadEmoteImage(emoticon, new URI(EMOTE_URL_TEMPLATE.replace("{{id}}", data)), "twitch", data);
		}
		catch (URISyntaxException e) {
			throw new EmoteLoaderException(e);
		}
	}
}
