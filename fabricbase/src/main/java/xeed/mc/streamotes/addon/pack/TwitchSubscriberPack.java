package xeed.mc.streamotes.addon.pack;

import xeed.mc.streamotes.addon.TwitchEmotesAPI;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class TwitchSubscriberPack {
	private static final String EMOTE_URL_TEMPLATE = "https://static-cdn.jtvnw.net/emoticons/v2/{{id}}/default/dark/2.0";
	private static final int PRIO = 0;

	private static void loadSource1(String channel, String channelId) throws IOException {
		var apiURL = TwitchEmotesAPI.getURL("https://twitchemotes.com/channels/" + channelId);
		try (var reader = new BufferedReader(new InputStreamReader(TwitchEmotesAPI.openStream(apiURL)))) {
			TwitchEmotesAPI.concentrateLines(reader, line -> {
				int beginAt = 0;
				while (beginAt < line.length()) {
					int ixStart = line.indexOf(" class=\"emote expandable-emote\" ", beginAt);
					if (ixStart == -1) return;

					String prefix = "data-image-id=\"";
					ixStart = line.indexOf(prefix);
					if (ixStart == -1) return;

					int ixEnd = line.indexOf("\"", ixStart + prefix.length());
					if (ixEnd == -1) return;

					String id = line.substring(ixStart + prefix.length(), ixEnd);

					prefix = "data-regex=\"";
					ixStart = line.indexOf(prefix);
					if (ixStart == -1) return;

					ixEnd = line.indexOf("\"", ixStart + prefix.length());
					if (ixEnd == -1) return;

					String code = line.substring(ixStart + prefix.length(), ixEnd);

					var emoticon = EmoticonRegistry.registerEmoticon(channel, code, PRIO, TwitchSubscriberPack::loadEmoticonImage);
					if (emoticon != null) {
						emoticon.setLoadData(id);
						emoticon.setTooltip(channel);
					}

					beginAt = ixEnd + 1;
				}
			});
		}
	}

	private static void loadSource2(String channel, String channelId) throws IOException {
		var apiURL = TwitchEmotesAPI.getURL("https://www.twitchmetrics.net/c/" + channelId + "-" + channel.toLowerCase(Locale.ROOT) + "/emotes/");
		try (var reader = new BufferedReader(new InputStreamReader(TwitchEmotesAPI.openStream(apiURL)))) {
			TwitchEmotesAPI.concentrateLines(reader, line -> {
				int beginAt = 0;
				while (beginAt < line.length()) {
					final String prefix = "<a href=\"/e/";
					int ixStart = line.indexOf(prefix, beginAt);
					if (ixStart == -1) return;

					int ixEnd = line.indexOf("\"", ixStart + prefix.length());
					if (ixEnd == -1) return;

					String idCode = line.substring(ixStart + prefix.length(), ixEnd);

					ixStart = idCode.indexOf('-');
					if (ixStart == -1) return;

					String id = idCode.substring(0, ixStart);
					String code = idCode.substring(ixStart + 1);

					var emoticon = EmoticonRegistry.registerEmoticon(channel, code, PRIO, TwitchSubscriberPack::loadEmoticonImage);
					if (emoticon != null) {
						emoticon.setLoadData(id);
						emoticon.setTooltip(channel);
					}

					beginAt = ixEnd + 1;
				}
			});
		}
	}

	public static void loadMetadata(String channel) {
		try {
			String channelId = TwitchEmotesAPI.getChannelId(channel);

			try {
				loadSource1(channel, channelId);
			}
			catch (IOException ignored) {
				loadSource2(channel, channelId);
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
