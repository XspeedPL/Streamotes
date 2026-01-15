package xeed.mc.streamotes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import xeed.mc.streamotes.addon.TwitchEmotesAPI;
import xeed.mc.streamotes.addon.pack.*;
import xeed.mc.streamotes.api.EmoteLoaderException;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class Streamotes {
	public static final Pattern EMOTE_PATTERN = Pattern.compile("[^\\s:]{2,}|:?[^\\s:]+:?", Pattern.UNICODE_CHARACTER_CLASS);
	public static Streamotes INSTANCE;

	private final AtomicInteger LOAD_COUNTER = new AtomicInteger(0);
	private ModConfigModel ovConfig = null;

	public static void log(String text) {
		StreamotesCommon.logi(text);
	}

	public static void loge(String text, Throwable t) {
		StreamotesCommon.loge(text, t);
		msg(text);
	}

	public static void msg(String text) {
		var mc = Minecraft.getInstance();

		var mode = StreamotesCommon.getOwnConfig().errorReporting;
		if (mode == ReportOption.Toast) {
			var title = Component.literal("Streamotes");
			var msg = Component.literal(text);

			Compat.getToastManager().addToast(SystemToast.multiline(mc, Compat.TOAST_TYPE, title, msg));
		}
		else if (mode == ReportOption.Chat) {
			mc.gui.getChat().addMessage(Component.literal("Streamotes: " + text));
		}
	}

	public ModConfigModel getConfig() {
		return ovConfig != null ? ovConfig : StreamotesCommon.getOwnConfig();
	}

	public void onInitializeClient() {
		INSTANCE = this;
		ImageIO.scanForPlugins();
	}

	public void onClientStarted(Minecraft client) {
		TwitchEmotesAPI.initialize(client.gameDirectory);
	}

	public void onPlayerJoin() {
		reloadEmoticons();
	}

	public void onPlayerDisconnect() {
		ovConfig = null;
	}

	public void onReceiveJsonPacket(String json) {
		var cfg = StreamotesCommon.configFromJson(json);
		if (cfg == null) {
			log("Received invalid config JSON: " + json);
			msg("Received invalid emote config! Contact server admin.");
		}
		else {
			ovConfig = cfg;
			msg("Received emote config, starting loading");
			if (cfg.forceClearCache) {
				msg("Force cache clear requested, clearing cache");
				TwitchEmotesAPI.clearFileCache();
				TwitchEmotesAPI.clearJsonCache();
				cfg.forceClearCache = false;
			}
			if (StreamotesCommon.getOwnConfig().versionCode != cfg.versionCode) {
				loge("Server reported different mod version: " + cfg.versionName, null);
			}
			reloadEmoticons();
		}
	}

	private void startLoadingDaemon(String name, Runnable action) {
		EmoticonRegistry.startLoading();
		var thread = new Thread(() -> {
			try {
				action.run();
			}
			finally {
				if (EmoticonRegistry.endLoading()) {
					var emotes = EmoticonRegistry.getEmoteNames();
					msg("Finished loading, " + emotes.size() + " emotes from " + getConfig().emoteChannels.size() + " channels");
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
		int nr = 0;
		while (true) {
			try {
				func.run();
				return null;
			}
			catch (EmoteLoaderException t) {
				if (--maxTries <= 0) return t;
				sleepSweetPrince((++nr) * 50);
			}
		}
	}

	private void reloadEmoticons() {
		final int loadId = LOAD_COUNTER.incrementAndGet();

		startLoadingDaemon("Emote Load Manager", () -> {
			while (EmoticonRegistry.isLoading()) sleepSweetPrince(10);

			EmoticonRegistry.reloadEmoticons();
			Compat.clearLayerCache();
			Minecraft.getInstance().execute(EmoticonRegistry::runDisposal);

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

	public void processPacks(String sourceName, int loadId, ArrayList<String> channelList, Runnable globLoader, Consumer<String> subLoader) {
		startLoadingDaemon(sourceName + " Emote Loader", () -> {
			if (globLoader != null) {
				try {
					if (LOAD_COUNTER.get() != loadId) return;
					var ex = tryFewTimes(globLoader, 10);
					if (ex != null) throw ex;
				}
				catch (EmoteLoaderException e) {
					loge("Failed to load " + sourceName + " global emotes", e);
				}
			}

			if (subLoader != null) {
				for (String channel : channelList) {
					try {
						if (LOAD_COUNTER.get() != loadId) return;
						var ex = tryFewTimes(() -> subLoader.accept(channel), 10);
						if (ex != null) throw ex;
					}
					catch (EmoteLoaderException e) {
						loge("Failed to load " + sourceName + " " + channel + " emotes", e);
					}
				}
			}
		});
	}
}
