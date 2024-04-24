package xeed.mc.streamotes.addon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.impl.lib.gson.MalformedJsonException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import xeed.mc.streamotes.InternalMethods;
import xeed.mc.streamotes.StreamotesCommon;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;

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

public class TwitchEmotesAPI {
	private static final int CACHE_LIFETIME_JSON = 86400000;
	private static final int CACHE_LIFETIME_IMAGE = 604800000;
	private static final boolean CACHE_EMOTES = false;

	private static final HashMap<String, CacheEntry<String>> channelToIdMap = new HashMap<>();

	private static File cacheDir;
	private static File cachedEmotes;

	private record CacheEntry<T>(T item, long expTime) {
	}

	public static void initialize(File mcDataDir) {
		cacheDir = new File(mcDataDir, "emoticons/cache");
		cachedEmotes = new File(cacheDir, "images/");
	}

	public static synchronized String getChannelId(String name) throws IOException {
		var entry = channelToIdMap.get(name);
		if (entry != null && entry.expTime() <= System.currentTimeMillis()) return entry.item();

		var apiURL = new URL("https://twitchtracker.com/" + name);

		try (var reader = new BufferedReader(new InputStreamReader(apiURL.openStream()))) {
			String data = IOUtils.toString(reader);
			final String prefix = "window.channel = {";
			final String suffix = "id: ";

			int ixStart = data.indexOf(prefix);
			if (ixStart == -1) throw new MalformedJsonException("Prefix not found");

			int ixId = data.indexOf(suffix, ixStart + prefix.length());
			if (ixId == -1) throw new MalformedJsonException("Suffix not found");

			int ixEnd = data.indexOf(",", ixId + suffix.length());
			if (ixEnd == -1) throw new MalformedJsonException("Separator after suffix not found");

			String channelId = data.substring(ixId + suffix.length(), ixEnd);
			channelToIdMap.put(name, new CacheEntry<>(channelId, System.currentTimeMillis() + 1000 * 60));
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
				StreamotesCommon.loge("Cache writing failed for " + emote.getName(), e);
			}
		}
	}

	public static void clearCache() {
		try {
			FileUtils.deleteDirectory(cacheDir);
		}
		catch (IOException e) {
			StreamotesCommon.loge("Cache purge failed", e);
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
			if (result == null) {
				throw new EmoteLoaderException("'" + name + "' is null");
			}
			return result;
		}
		catch (ClassCastException e) {
			throw new EmoteLoaderException("name: " + name, e);
		}
	}
}
