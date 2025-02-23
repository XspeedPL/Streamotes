package xeed.mc.streamotes;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
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
	public static final Pattern EMOTE_PATTERN = Pattern.compile("[\\w-]{2,}|:?[\\w-]+:?");
	public static final String CHAT_TRIGGER = "\u2060";
	public static final String CHAT_SEPARATOR = "\u2061";
	public static final ThreadLocal<LinkedList<EmoteRenderInfo>> RENDER_QUEUE = ThreadLocal.withInitial(LinkedList::new);

	private static final AtomicInteger LOAD_COUNTER = new AtomicInteger(0);
	private static final SystemToast.Type STREAMOTES_TOAST = Compat.makeToastType();

	public static Streamotes INSTANCE;
	public static int MAX_TEXTURE_SIZE = 256;

	private ModConfigModel ovConfig = null;

	public static void log(String text) {
		StreamotesCommon.logi(text);
	}

	public static void loge(String text, Throwable t) {
		StreamotesCommon.loge(text, t);
		msg(text);
	}

	public static void msg(String text) {
		var mc = MinecraftClient.getInstance();

		var mode = StreamotesCommon.getOwnConfig().errorReporting;
		if (mode == ReportOption.Toast) {
			var title = Text.literal("Streamotes");
			var msg = Text.literal(text);

			mc.getToastManager().add(SystemToast.create(mc, STREAMOTES_TOAST, title, msg));
		}
		else if (mode == ReportOption.Chat) {
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

		Compat.onInitializeClient(this::onReceiveJsonPacket);
	}

	private void onReceiveJsonPacket(String json) {
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

	@FunctionalInterface
	public interface StringAction {
		void apply(String str);
	}
}
