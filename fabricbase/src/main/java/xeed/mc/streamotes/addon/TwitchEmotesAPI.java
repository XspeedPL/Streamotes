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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TwitchEmotesAPI {
	private static final int CACHE_LIFETIME_IMAGE = 604800000; // 7 days
	private static final boolean CACHE_EMOTES = true;

	private static final HashMap<String, CacheEntry<JsonElement>> jsonCache = new HashMap<>();
	private static final HashMap<String, CacheEntry<String>> channelIdCache = new HashMap<>();
	private static final Gson gson = new Gson();

	private static File cacheDir;
	private static File cachedEmotes;

	private record CacheEntry<T>(T item, long expTime) {
	}

	public static void initialize(File mcDataDir) {
		cacheDir = new File(mcDataDir, "emoticons/cache");
		cachedEmotes = new File(cacheDir, "images/");
	}

	public static void concentrateLines(BufferedReader reader, Consumer<String> action) throws IOException {
		var buffer = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.isBlank() && !buffer.isEmpty()) {
				line = buffer.toString();
				buffer.setLength(0);
				action.accept(line);
			}
			else if (!line.isBlank()) {
				buffer.append(line);
			}
		}
	}

	public static URL getURL(String url) {
		try {
			return new URI(url).toURL();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static InputStream openStream(URL url) throws IOException {
		var conn = url.openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0");
		return conn.getInputStream();
	}

	private static <T> T getJson(InputStream stream, Class<T> cls) throws IOException {
		try (var reader = new InputStreamReader(stream)) {
			return gson.fromJson(reader, cls);
		}
	}

	public static <T extends JsonElement> JsonElement getJson(URL url, Class<T> cls) throws IOException {
		synchronized (jsonCache) {
			var entry = jsonCache.get(url.toString());
			if (entry != null && entry.expTime() <= System.currentTimeMillis()) return entry.item();

			var json = getJson(openStream(url), cls);
			jsonCache.put(url.toString(), new CacheEntry<>(json, System.currentTimeMillis() + (1000 * 60)));
			return json;
		}
	}

	public static JsonObject getJsonObj(URL url) throws IOException {
		return getJson(url, JsonObject.class).getAsJsonObject();
	}

	public static JsonArray getJsonArr(URL url) throws IOException {
		return getJson(url, JsonArray.class).getAsJsonArray();
	}

	private static HttpURLConnection makeGQL(String name) throws IOException {
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

		return conn;
	}

	public static String getChannelId(String name) throws IOException {
		synchronized (channelIdCache) {
			var entry = channelIdCache.get(name);
			if (entry != null && entry.expTime() <= System.currentTimeMillis()) return entry.item();

			var conn = makeGQL(name);
			int code = conn.getResponseCode();
			if (code / 100 != 2) {
				String info = IOUtils.toString(conn.getErrorStream(), StandardCharsets.UTF_8);
				throw new IOException("Channel ID request for name " + name + " returned " + code + ": " + info);
			}

			var data = getJson(conn.getInputStream(), JsonObject.class);
			try {
				var user = data.getAsJsonObject("data").getAsJsonArray("users").get(0).getAsJsonObject();

				if (user.get("username").getAsString().equals("*deleted_user"))
					throw new IOException("Channel " + name + " has no valid 7tv profile");

				var struct = user.getAsJsonArray("connections").asList().stream().map(JsonElement::getAsJsonObject).filter(x -> x.get("platform").getAsString().equals("TWITCH")).findFirst();
				if (struct.isEmpty())
					throw new IOException("7tv profile " + name + " has no associated Twitch channel");

				var channelId = struct.get().get("id").getAsString();
				channelIdCache.put(name, new CacheEntry<>(channelId, System.currentTimeMillis() + (1000 * 60 * 5)));
				return channelId;
			}
			catch (NullPointerException | IndexOutOfBoundsException e) {
				throw new IOException("Invalid json trying to get channel ID of " + name + ": " + data.toString(), e);
			}
		}
	}

	private static boolean shouldUseCacheFileImage(File file) {
		return file.exists() && (System.currentTimeMillis() - file.lastModified()) <= CACHE_LIFETIME_IMAGE;
	}

	public static void loadEmoteImage(Emoticon emote, URI source, String cacheId, String imageId) {
		var cachedImageFile = new File(cachedEmotes, cacheId + "-" + imageId + ".png");
		if (CACHE_EMOTES && shouldUseCacheFileImage(cachedImageFile)) {
			if (InternalMethods.loadImage(emote, cachedImageFile)) return;
			else clearFileCache();
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
							StandardCharsets.UTF_8);
					}
				}
			}
			catch (IOException e) {
				StreamotesCommon.loge("Cache writing failed for " + emote.getName(), e);
			}
			finally {
				emote.discardBitmap();
			}
		}
	}

	public static void clearFileCache() {
		try {
			FileUtils.deleteDirectory(cacheDir);
		}
		catch (IOException e) {
			StreamotesCommon.loge("Cache purge failed", e);
		}
	}

	public static void clearJsonCache() {
		synchronized (jsonCache) {
			jsonCache.clear();
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
