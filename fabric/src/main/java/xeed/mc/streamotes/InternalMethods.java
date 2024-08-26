package xeed.mc.streamotes;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.texture.NativeImage;
import xeed.mc.streamotes.emoticon.Emoticon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class InternalMethods {
	public static boolean loadImage(Emoticon emoticon, URI uri) {
		try (var in = uri.toURL().openStream()) {
			Streamotes.log("Loading from " + uri);
			return loadImage(emoticon, in);
		}
		catch (IOException e) {
			Streamotes.loge("Emote " + emoticon.getName() + " load failed", e);
			return false;
		}
	}

	private static int[] loadInts(File file) throws IOException {
		var result = new IntArrayList();
		try (var reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				result.add(Integer.parseInt(line));
			}
		}
		return result.toIntArray();
	}

	public static boolean loadImage(Emoticon emoticon, File file) {
		Streamotes.log("Loading from " + file.getAbsolutePath());
		try (var in = new FileInputStream(file)) {
			if (loadImage(emoticon, in)) {
				var meta = new File(file.getParentFile(), file.getName() + ".txt");
				if (meta.exists()) {
					int[] data = loadInts(meta);
					if (data.length < 3) return false;

					int width = data[0];
					int height = data[1];
					int[] frameTimes = Arrays.copyOfRange(data, 2, data.length - 1);
					emoticon.setFrameData(frameTimes, width, height);
				}
				return true;
			}
		}
		catch (IOException e) {
			Streamotes.loge("Emote " + emoticon.getName() + " cache load failed", e);
		}
		return false;
	}

	public static boolean loadImage(Emoticon emoticon, InputStream obj) {
		try {
			var frames = ImageHandler.readImages(obj);

			if (frames.isEmpty()) {
				return false;
			}
			else if (frames.size() == 1) {
				emoticon.setImage(awtToNative(frames.getFirst().getLeft()));
			}
			else {
				var images = new BufferedImage[frames.size()];
				int[] times = new int[images.length];

				for (int i = 0; i < images.length; ++i) {
					images[i] = frames.get(i).getLeft();
					times[i] = frames.get(i).getRight();
				}

				emoticon.setImages(images, times);
			}

			return true;
		}
		catch (IOException | IllegalArgumentException e) {
			Streamotes.loge("Emote " + emoticon.getName() + " data load failed", e);
			return false;
		}
	}

	public static NativeImage awtToNative(BufferedImage img) throws IOException {
		try (var baos = new ByteArrayOutputStream()) {
			ImageIO.write(img, "png", baos);
			try (var bais = new ByteArrayInputStream(baos.toByteArray())) {
				return NativeImage.read(bais);
			}
		}
	}
}
