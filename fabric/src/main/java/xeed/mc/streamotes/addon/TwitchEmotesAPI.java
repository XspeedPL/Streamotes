package xeed.mc.streamotes.addon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import xeed.mc.streamotes.InternalMethods;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;

public class TwitchEmotesAPI {
	private static final int CACHE_LIFETIME_JSON = 86400000;
	private static final int CACHE_LIFETIME_IMAGE = 604800000;
	private static final boolean CACHE_EMOTES = false;

	private static final HashMap<String, String> channelToIdMap = new HashMap<>();

	private static File cacheDir;
	private static File cachedEmotes;

	public static void initialize(File mcDataDir) {
		cacheDir = new File(mcDataDir, "emoticons/cache");
		cachedEmotes = new File(cacheDir, "images/");
	}

	public static String getChannelId(String name) throws IOException {
		String channelId = channelToIdMap.get(name);
		var apiURL = new URL("https://twitchtracker.com/" + name);

		try (var reader = new BufferedReader(new InputStreamReader(apiURL.openStream()))) {
			String data = IOUtils.toString(reader);
			final String prefix = "window.channel = {\n\t\t\tid: ";

			int ixStart = data.indexOf(prefix);
			if (ixStart == -1) return channelId;

			int ixEnd = data.indexOf(",", ixStart + prefix.length());
			if (ixEnd == -1) return channelId;

			channelId = data.substring(ixStart + prefix.length(), ixEnd);
			channelToIdMap.put(name, channelId);
			return channelId;
		}
	}

	private static boolean shouldUseCacheFileImage(File file) {
		return file.exists() && (System.currentTimeMillis() - file.lastModified()) <= CACHE_LIFETIME_IMAGE;
	}

	public static void loadEmoteImage(Emoticon emote, URI source, String cacheId, String imageId) {
		var cachedImageFile = new File(cachedEmotes, cacheId + "-" + imageId + ".png");
		if (CACHE_EMOTES && shouldUseCacheFileImage(cachedImageFile)) {
			if (InternalMethods.loadImage(emote, cachedImageFile)) return;
			else clearCache();
		}

		if (InternalMethods.loadImage(emote, source)) {
			try {
				var cacheDir = cachedImageFile.getParentFile();
				if (CACHE_EMOTES && (cacheDir.exists() || cacheDir.mkdirs())) {
					emote.writeImage(cachedImageFile);
					if (emote.isAnimated()) {
						Files.write(new File(cachedImageFile.getParentFile(), cachedImageFile.getName() + ".txt").toPath(),
								IntStream.concat(IntStream.of(emote.getWidth(), emote.getHeight()), IntStream.of(emote.getFrameTimes()))
										.mapToObj(Integer::toString).collect(Collectors.toList()),
								StandardCharsets.US_ASCII);
					}
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void clearCache() {
		try {
			FileUtils.deleteDirectory(cacheDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getJsonString(JsonObject object, String name) {
		JsonElement element = object.get(name);
		if (element == null) {
			throw new EmoteLoaderException("'" + name + "' is null");
		}
		try {
			return element.getAsString();
		}
		catch (ClassCastException e) {
			throw new EmoteLoaderException("name: " + name, e);
		}
	}

	public static JsonArray getJsonArray(JsonObject object, String name) {
		try {
			JsonArray result = object.getAsJsonArray(name);
			if(result == null) {
				throw new EmoteLoaderException("'" + name + "' is null");
			}
			return result;
		} catch (ClassCastException e) {
			throw new EmoteLoaderException("name: " + name, e);
		}
	}
}
