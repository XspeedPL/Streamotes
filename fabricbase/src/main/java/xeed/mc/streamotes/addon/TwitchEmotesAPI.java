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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TwitchEmotesAPI {
	private static final int CACHE_LIFETIME_IMAGE = 604_800_000; // 7 days
	private static final int CACHE_LIFETIME_JSON = 60_000; // 1 minute
	private static final int CACHE_LIFETIME_CHANNEL_ID = 300_000; // 5 minutes
	private static final boolean CACHE_EMOTES = true;

	private static final ConcurrentHashMap<URI, CacheEntry<JsonElement>> jsonCache = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, CacheEntry<String>> channelIdCache = new ConcurrentHashMap<>();
	private static final Gson gson = new Gson();
	private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

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

	public static URI getURI(String url) {
		try {
			return new URI(url);
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private static InputStream getInputStream(HttpResponse<InputStream> resp) throws IOException {
		return resp.body();
	}

	public static InputStream openStream(URI uri) {
		try {
			var req = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(15))
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0")
				.GET()
				.build();

			var resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
			if (resp.statusCode() == 404) throw new FileNotFoundException();

			return getInputStream(resp);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static <T> T getJson(InputStreamReader reader, Class<T> cls) {
		return gson.fromJson(reader, cls);
	}

	public static <T extends JsonElement> JsonElement getJson(URI uri, Class<T> cls) {
		return jsonCache.compute(uri, (key, entry) -> entry == null || entry.expTime() < System.currentTimeMillis()
			? new CacheEntry<>(getJson(new InputStreamReader(openStream(key)), cls), System.currentTimeMillis() + CACHE_LIFETIME_JSON)
			: entry).item;
	}

	public static JsonObject getJsonObj(URI uri) {
		return getJson(uri, JsonObject.class).getAsJsonObject();
	}

	public static JsonArray getJsonArr(URI uri) {
		return getJson(uri, JsonArray.class).getAsJsonArray();
	}

	private static HttpResponse<InputStream> makeGQL(String name) {
		var apiURI = getURI("https://7tv.io/v3/gql");

		var query = "query FindUser($name: String!) { users(query: $name) { id username connections { id platform display_name } } }";
		var vars = "{ \"name\": \"" + name + "\" }";
		var body = "{\"query\": \"" + query + "\",\"operationName\": \"FindUser\",\"variables\": " + vars + "}";

		var req = HttpRequest.newBuilder(apiURI)
			.timeout(Duration.ofSeconds(15))
			.header("Content-Type", "application/json; charset=utf-8")
			.header("User-Agent", "insomnia/9.3.3")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();

		try {
			return client.send(req, HttpResponse.BodyHandlers.ofInputStream());
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static JsonObject getChannelDataObject(String name) {
		try {
			var conn = makeGQL(name);
			int code = conn.statusCode();
			if (code / 100 != 2) {
				String info = IOUtils.toString(conn.body(), StandardCharsets.UTF_8);
				throw new RuntimeException("Channel ID request for name " + name + " returned " + code + ": " + info);
			}

			var reader = new InputStreamReader(getInputStream(conn));
			return getJson(reader, JsonObject.class);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static String getChannelId(String name) {
		return Objects.requireNonNull(channelIdCache.compute(name, (key, entry) -> {
			if (entry != null && entry.expTime() > System.currentTimeMillis()) return entry;

			var data = getChannelDataObject(name);
			try {
				var users = data.getAsJsonObject("data").getAsJsonArray("users").asList();
				boolean nameFound = false;

				for (var uelem : users) {
					var user = uelem.getAsJsonObject();
					if (!user.get("username").getAsString().equalsIgnoreCase(name)) continue;

					nameFound = true;
					var conns = user.getAsJsonArray("connections").asList();
					for (var celem : conns) {
						var cdata = celem.getAsJsonObject();
						if (!cdata.get("platform").getAsString().equals("TWITCH")) continue;

						var channelId = cdata.get("id").getAsString();
						return new CacheEntry<>(channelId, System.currentTimeMillis() + CACHE_LIFETIME_CHANNEL_ID);
					}
				}

				if (nameFound) throw new RuntimeException("7tv profile " + name + " has no associated Twitch channel");
				else throw new RuntimeException("Channel " + name + " has no valid 7tv profile");
			}
			catch (NullPointerException | IndexOutOfBoundsException e) {
				throw new RuntimeException("Invalid json trying to get channel ID of " + name + ": " + data.toString(), e);
			}
		})).item;
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
		jsonCache.clear();
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
