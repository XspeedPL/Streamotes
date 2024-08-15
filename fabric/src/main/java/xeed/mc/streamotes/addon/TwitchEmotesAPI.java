package xeed.mc.streamotes.addon;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import xeed.mc.streamotes.InternalMethods;
import xeed.mc.streamotes.StreamotesCommon;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.Emoticon;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TwitchEmotesAPI {
	private static final int CACHE_LIFETIME_IMAGE = 604800000;
	private static final boolean CACHE_EMOTES = false;

	private static final HashMap<String, CacheEntry<String>> channelToIdMap = new HashMap<>();
	private static final Gson gson = new Gson();

	private static File cacheDir;
	private static File cachedEmotes;

	private record CacheEntry<T>(T item, long expTime) {
	}

	public static void initialize(File mcDataDir) {
		cacheDir = new File(mcDataDir, "emoticons/cache");
		cachedEmotes = new File(cacheDir, "images/");
	}

	public static URL getURL(String url) {
		try {
			return new URI(url).toURL();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static JsonObject getJsonObj(InputStream stream) throws IOException {
		try (var reader = new InputStreamReader(stream)) {
			return gson.fromJson(reader, JsonObject.class);
		}
	}

	public static JsonArray getJsonArr(InputStream stream) throws IOException {
		try (var reader = new InputStreamReader(stream)) {
			return gson.fromJson(reader, JsonArray.class);
		}
	}

	public static synchronized String getChannelId(String name) throws IOException {
		var entry = channelToIdMap.get(name);
		if (entry != null && entry.expTime() <= System.currentTimeMillis()) return entry.item();

		var apiURL = getURL("https://7tv.io/v3/gql");

		var conn = (HttpURLConnection)apiURL.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("User-Agent", "insomnia/9.3.3");
		conn.setDoOutput(true);

		var query = "query FindUser($name: String!) { users(query: $name) { id username connections { id platform display_name } } }";
		var vars = "{ \"name\": \"" + name + "\" }";
		var body = "{\"query\": \"" + query + "\",\"operationName\": \"FindUser\",\"variables\": " + vars + "}";

		try (var writer = new DataOutputStream(conn.getOutputStream())) {
			writer.writeBytes(body);
			writer.flush();
		}

		int code = conn.getResponseCode();
		if (code / 100 != 2) {
			String info = IOUtils.toString(conn.getErrorStream(), StandardCharsets.UTF_8);
			throw new IOException("Channel ID request for name " + name + " returned " + code + ": " + info);
		}

		var data = getJsonObj(conn.getInputStream());
		data = data.getAsJsonObject("data").getAsJsonArray("users").get(0).getAsJsonObject();

		if (data.get("username").getAsString().equals("*deleted_user"))
			throw new IOException("Channel " + name + " has no valid 7tv profile");

		var struct = data.getAsJsonArray("connections").asList().stream().map(JsonElement::getAsJsonObject).filter(x -> x.get("platform").getAsString().equals("TWITCH")).findFirst();
		if (struct.isEmpty())
			throw new IOException("7tv profile " + name + " has no associated Twitch channel");

		var channelId = struct.get().get("id").getAsString();
		channelToIdMap.put(name, new CacheEntry<>(channelId, System.currentTimeMillis() + (1000 * 60 * 5)));
		return channelId;
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
