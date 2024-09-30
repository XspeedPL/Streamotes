package xeed.mc.streamotes.addon.pack;

import xeed.mc.streamotes.addon.TwitchEmotesAPI;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

public class TwitchGlobalPack {
	private static final String EMOTE_URL_TEMPLATE = "https://static-cdn.jtvnw.net/emoticons/v2/{{id}}/default/dark/2.0";
	private static final int PRIO = 1;

	private static void loadSource1() throws IOException {
		var apiURL = TwitchEmotesAPI.getURL("https://twitchemotes.com/");

		try (var reader = new BufferedReader(new InputStreamReader(apiURL.openStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String prefix = "data-regex=\"";
				int ixStart = line.indexOf(prefix);
				if (ixStart == -1) continue;

				int ixEnd = line.indexOf("\"", ixStart + prefix.length());
				if (ixEnd == -1) continue;

				String code = line.substring(ixStart + prefix.length(), ixEnd);

				prefix = "data-image-id=\"";
				ixStart = line.indexOf(prefix);
				if (ixStart == -1) continue;

				ixEnd = line.indexOf("\"", ixStart + prefix.length());
				if (ixEnd == -1) continue;

				String id = line.substring(ixStart + prefix.length(), ixEnd);

				var emoticon = EmoticonRegistry.registerEmoticon(".Twitch", code, PRIO, TwitchGlobalPack::loadEmoticonImage);
				if (emoticon != null) {
					emoticon.setLoadData(id);
					emoticon.setTooltip("Twitch");
				}
			}
		}
	}

	private static void loadSource2() throws IOException {
		var apiURL = TwitchEmotesAPI.getURL("https://www.twitchmetrics.net/emotes/");

		try (var reader = new BufferedReader(new InputStreamReader(apiURL.openStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String prefix = "<a href=\"/e/";
				int ixStart = line.indexOf(prefix);
				if (ixStart == -1) continue;

				int ixEnd = line.indexOf("\"", ixStart + prefix.length());
				if (ixEnd == -1) continue;

				String idCode = line.substring(ixStart + prefix.length(), ixEnd);

				ixStart = idCode.indexOf('-');
				if (ixStart == -1) continue;

				String id = idCode.substring(0, ixStart);
				String code = idCode.substring(ixStart + 1);

				var emoticon = EmoticonRegistry.registerEmoticon(".Twitch", code, PRIO, TwitchGlobalPack::loadEmoticonImage);
				if (emoticon != null) {
					emoticon.setLoadData(id);
					emoticon.setTooltip("Twitch");
				}
			}
		}
	}

	public static void loadMetadata() {
		try {
			loadSource1();
		}
		catch (IOException ignored) {
			try {
				loadSource2();
			}
			catch (Exception e) {
				throw new EmoteLoaderException("Unhandled exception", e);
			}
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
