package xeed.mc.streamotes.emoticon;

import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.StreamotesCommon;

import java.util.ArrayDeque;

public class AsyncEmoticonLoader implements Runnable {
	public static final AsyncEmoticonLoader instance = new AsyncEmoticonLoader();

	private final ArrayDeque<Emoticon> loadQueue;
	private final Object sync;
	private Thread thread;

	public AsyncEmoticonLoader() {
		loadQueue = new ArrayDeque<>();
		sync = new Object();
		setupThread();
	}

	private synchronized void setupThread() {
		if (thread != null && thread.isAlive()) return;

		thread = new Thread(this, "StreamotesLoader");
		thread.setDaemon(true);
		thread.start();
	}

	public void loadAsync(Emoticon emoticon) {
		setupThread();

		synchronized (sync) {
			loadQueue.addLast(emoticon);
			sync.notify();
		}
	}

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
						return;
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
