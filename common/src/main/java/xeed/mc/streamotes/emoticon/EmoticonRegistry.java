package xeed.mc.streamotes.emoticon;

import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.api.IEmoticonLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EmoticonRegistry {
	private static final HashMap<String, Emoticon> namedMap = new HashMap<>();
	private static final ArrayList<Emoticon> disposalList = new ArrayList<>();
	private static final ArrayList<Emoticon> tempList = new ArrayList<>();
	private static final Object loadingLock = new Object();
	private static final AtomicInteger loading = new AtomicInteger(0);

	public static void startLoading() {
		loading.incrementAndGet();
	}

	public static boolean endLoading() {
		return loading.decrementAndGet() == 0;
	}

	public static boolean isLoading() {
		return loading.get() > 1;
	}

	public static List<String> getEmoteNames() {
		return namedMap.keySet().stream().sorted().collect(Collectors.toList());
	}

	public static Collection<Emoticon> getEmotes() {
		tempList.clear();
		synchronized (loadingLock) {
			tempList.addAll(namedMap.values());
		}
		return tempList;
	}

	public static Emoticon registerEmoticon(String source, String name, int priority, IEmoticonLoader loader) {
		return registerEmoticon(source, name, false, priority, loader);
	}

	public static Emoticon registerEmoticon(String source, String name, boolean zeroWidth, int priority, IEmoticonLoader loader) {
		if (!Streamotes.EMOTE_PATTERN.matcher(name).matches()) return null;

		synchronized (loadingLock) {
			var emoticon = namedMap.get(name);
			if (emoticon != null && emoticon.priority < priority) return null;

			emoticon = new Emoticon(source, name, zeroWidth, priority, loader);
			namedMap.put(name, emoticon);
			return emoticon;
		}
	}

	public static Emoticon fromName(String name) {
		synchronized (loadingLock) {
			return namedMap.get(name);
		}
	}

	public static void reloadEmoticons() {
		synchronized (loadingLock) {
			synchronized (disposalList) {
				disposalList.addAll(namedMap.values());
			}
			namedMap.clear();
		}
	}

	public static void runDisposal() {
		synchronized (disposalList) {
			if (!disposalList.isEmpty()) {
				for (Emoticon emoticon : disposalList) {
					emoticon.close();
				}
				disposalList.clear();
			}
		}
	}
}
