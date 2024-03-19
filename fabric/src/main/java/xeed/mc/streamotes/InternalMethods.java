package xeed.mc.streamotes;

import net.minecraft.client.texture.NativeImage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;

import xeed.mc.streamotes.emoticon.Emoticon;

public class InternalMethods {
	public static boolean loadImage(Emoticon emoticon, URI uri) {
		try (var in = uri.toURL().openStream()) {
			Streamotes.log("Loading from " + uri);
			return loadImage(emoticon, in);
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static int[] loadInts(Path path) throws IOException {
		try (var lines = Files.lines(path, StandardCharsets.US_ASCII)) {
			return lines.mapToInt(Integer::parseInt).toArray();
		}
	}

	public static boolean loadImage(Emoticon emoticon, File file) {
		Streamotes.log("Loading from " + file.getAbsolutePath());
		try (var in = new FileInputStream(file)) {
			if (loadImage(emoticon, in)) {
				var meta = new File(file.getParentFile(), file.getName() + ".txt");
				if (meta.exists()) {
					int[] data = loadInts(file.toPath());
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
			e.printStackTrace();
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
				emoticon.setImage(awtToNative(frames.get(0).getLeft()));
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
			e.printStackTrace();
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
