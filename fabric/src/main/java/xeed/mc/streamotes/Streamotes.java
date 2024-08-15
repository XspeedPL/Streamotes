package xeed.mc.streamotes;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import xeed.mc.streamotes.addon.TwitchEmotesAPI;
import xeed.mc.streamotes.addon.pack.*;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class Streamotes implements ClientModInitializer {
	public static final Pattern EMOTE_PATTERN = Pattern.compile("\\w{2,}");
	public static final Pattern EMOTE_PATTERN_ALT = Pattern.compile("\\w{2,}|:\\w{2,}:");
	public static final String CHAT_TRIGGER = "\u2060";
	public static final String CHAT_SEPARATOR = "\u2061";
	public static final ThreadLocal<LinkedList<EmoteRenderInfo>> RENDER_QUEUE = ThreadLocal.withInitial(LinkedList::new);
	private static final AtomicInteger LOAD_COUNTER = new AtomicInteger(0);

	public static Streamotes INSTANCE;
	public static int MAX_TEXTURE_SIZE = 256;

	private ModConfigModel ovConfig = null;

	public static void log(String text) {
		StreamotesCommon.logi(text);
	}

	public static void loge(String text, Throwable t) {
		StreamotesCommon.loge(text, t);
	}

	public static void msg(String text) {
		var mc = MinecraftClient.getInstance();
		if (mc != null) {
			mc.inGameHud.getChatHud().addMessage(Text.literal("Streamotes: " + text));
		}
	}

	public ModConfigModel getConfig() {
		return ovConfig != null ? ovConfig : StreamotesCommon.getOwnConfig();
	}

	@Override
	public void onInitializeClient() {
		INSTANCE = this;

		ImageIO.scanForPlugins();

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			MAX_TEXTURE_SIZE = RenderSystem.maxSupportedTextureSize();

			TwitchEmotesAPI.initialize(client.runDirectory);
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reloadEmoticons());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ovConfig = null);

		ClientPlayNetworking.registerGlobalReceiver(JsonPayload.PACKET_ID, this::onReceivePacket);
	}

	private void onReceivePacket(JsonPayload packet, ClientPlayNetworking.Context context) {
		var json = packet.json();
		var cfg = StreamotesCommon.configFromJson(json);
		if (cfg == null) {
			log("Receinved invalid config JSON: " + json);
			msg("Received invalid emote config! Contact server admin.");
		}
		else {
			ovConfig = cfg;
			msg("Received emote config, starting loading");
			reloadEmoticons();
		}
	}

	private static void startLoadingDaemon(String name, Runnable action) {
		var thread = new Thread(() -> {
			EmoticonRegistry.startLoading();
			try {
				action.run();
			}
			finally {
				if (EmoticonRegistry.endLoading()) {
					var emotes = EmoticonRegistry.getEmoteNames();
					log("Loaded emote metadata: " + String.join(", ", emotes));
					msg("Finished loading, " + emotes.size() + " emotes");
				}
			}
		}, name);
		thread.setDaemon(true);
		thread.start();
	}

	public static void sleepSweetPrince(int millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ignored) {
		}
	}

	public static EmoteLoaderException tryFewTimes(Runnable func, int maxTries) {
		while (true) {
			try {
				func.run();
				return null;
			}
			catch (EmoteLoaderException t) {
				if (--maxTries <= 0) return t;
				sleepSweetPrince(50);
			}
		}
	}

	private void reloadEmoticons() {
		final int loadId = LOAD_COUNTER.incrementAndGet();

		startLoadingDaemon("Emote Load Manager", () -> {
			while (EmoticonRegistry.isLoading()) sleepSweetPrince(10);

			EmoticonRegistry.reloadEmoticons();
			RenderSystem.recordRenderCall(EmoticonRegistry::runDisposal);

			final var cfg = getConfig();
			final var channelList = new ArrayList<>(cfg.emoteChannels);

			if (cfg.x7tvEmotes || cfg.x7tvChannelEmotes) {
				processPacks("7TV", loadId, channelList,
					cfg.x7tvEmotes ? X7tvPack::loadMetadata : null,
					cfg.x7tvChannelEmotes ? X7tvChannelPack::loadMetadata : null);
			}

			if (cfg.ffzEmotes || cfg.ffzChannelEmotes) {
				processPacks("FFZ", loadId, channelList,
					cfg.ffzEmotes ? FFZPack::loadMetadata : null,
					cfg.ffzChannelEmotes ? FFZChannelPack::loadMetadata : null);
			}

			if (cfg.bttvEmotes || cfg.bttvChannelEmotes) {
				processPacks("BTTV", loadId, channelList,
					cfg.bttvEmotes ? BTTVPack::loadMetadata : null,
					cfg.bttvChannelEmotes ? BTTVChannelPack::loadMetadata : null);
			}

			if (cfg.twitchGlobalEmotes || cfg.twitchSubscriberEmotes) {
				processPacks("Twitch", loadId, channelList,
					cfg.twitchGlobalEmotes ? TwitchGlobalPack::loadMetadata : null,
					cfg.twitchSubscriberEmotes ? TwitchSubscriberPack::loadMetadata : null);
			}
		});
	}

	public static void processPacks(String sourceName, int loadId, ArrayList<String> channelList, Runnable globLoader, Consumer<String> subLoader) {
		startLoadingDaemon(sourceName + " Emote Loader", () -> {
			if (globLoader != null) {
				try {
					if (LOAD_COUNTER.get() != loadId) return;
					var ex = tryFewTimes(globLoader, 5);
					if (ex != null) throw ex;
				}
				catch (EmoteLoaderException e) {
					loge("Failed to load " + sourceName + " global emotes", e);
				}
			}

			if (subLoader != null) {
				try {
					for (String channel : channelList) {
						if (LOAD_COUNTER.get() != loadId) return;
						var ex = tryFewTimes(() -> subLoader.accept(channel), 5);
						if (ex != null) throw ex;
					}
				}
				catch (EmoteLoaderException e) {
					loge("Failed to load " + sourceName + " subscriber emotes", e);
				}
			}
		});
	}
}
