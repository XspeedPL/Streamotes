package xeed.mc.streamotes.emoticon;

import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.StreamotesCommon;

import java.util.ArrayDeque;
import java.util.Deque;

public class AsyncEmoticonLoader implements Runnable {
	private static final int WORKER_COUNT = 4;

	public static final AsyncEmoticonLoader instance = new AsyncEmoticonLoader();

	private final Deque<Emoticon> loadQueue;
	private final Object sync;

	public AsyncEmoticonLoader() {
		loadQueue = new ArrayDeque<>();
		sync = new Object();
		for (int i = 0; i < WORKER_COUNT; i++) {
			var thread = new Thread(this, "StreamotesLoader-" + i);
			thread.setDaemon(true);
			thread.start();
		}
	}

	public void loadAsync(Emoticon emoticon) {
		synchronized (sync) {
			loadQueue.push(emoticon);
			sync.notify();
		}
	}

	@SuppressWarnings("InfiniteLoopStatement")
	@Override
	public void run() {
		while (true) {
			Emoticon emoticon;
			synchronized (sync) {
				while (loadQueue.isEmpty()) {
					try {
						sync.wait();
					}
					catch (InterruptedException ignored) {
					}
				}
				emoticon = loadQueue.pop();
			}

			try {
				emoticon.getLoader().loadEmoticonImage(emoticon);
				Streamotes.log("Loaded emote " + emoticon.getName() + ": W" + emoticon.getWidth() + ", H" + emoticon.getHeight());
			}
			catch (Exception e) {
				StreamotesCommon.loge("Emote " + emoticon.getName() + " load failed", e);
			}
		}
	}
}
