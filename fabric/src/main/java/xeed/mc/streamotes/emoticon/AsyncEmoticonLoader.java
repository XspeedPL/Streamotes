package xeed.mc.streamotes.emoticon;

import java.util.LinkedList;

import xeed.mc.streamotes.Streamotes;

public class AsyncEmoticonLoader implements Runnable {
	public static final AsyncEmoticonLoader instance = new AsyncEmoticonLoader();

	private final LinkedList<Emoticon> loadQueue;
	private final Object sync;

	public AsyncEmoticonLoader() {
		loadQueue = new LinkedList<>();
		sync = new Object();
		var thread = new Thread(this, "StreamotesLoader");
		thread.setDaemon(true);
		thread.start();
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
			try {
				synchronized (sync) {
					while (!loadQueue.isEmpty()) {
						var emoticon = loadQueue.pop();
						try {
							emoticon.getLoader().loadEmoticonImage(emoticon);

							Streamotes.log("Loaded emote " + emoticon.getName() + ": W" + emoticon.getWidth() + ", H" + emoticon.getHeight());
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
					sync.wait();
				}
			} catch (InterruptedException ignored) {}
		}
	}
}
