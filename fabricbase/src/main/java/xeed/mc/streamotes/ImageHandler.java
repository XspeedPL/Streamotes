package xeed.mc.streamotes;

import com.madgag.gif.fmsware.GifDecoder;
import net.minecraft.util.Pair;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageHandler {
	public static final Color TRANSPARENT = new Color(255, 255, 255, 0);

	public static List<Pair<BufferedImage, Integer>> readImages(InputStream source) throws IOException {
		IOException lastEx = null;

		var output = new ByteArrayOutputStream() {
			public synchronized byte[] getBuffer() {
				return buf;
			}
		};
		source.transferTo(output);

		var input = new ByteArrayInputStream(output.getBuffer(), 0, output.size());

		try (var in = ImageIO.createImageInputStream(input)) {
			for (var reader : (Iterable<ImageReader>)(() -> ImageIO.getImageReaders(in))) {
				Streamotes.log("Trying " + reader.getClass().getName() + ", Format " + reader.getFormatName());

				if (reader.getFormatName().equals("gif")) {
					input.reset();
					return readGifFrames(input);
				}

				try {
					reader.setInput(in);
					var frames = readFrames(reader);
					var result = new ArrayList<Pair<BufferedImage, Integer>>(frames.size());

					for (var frame : frames) {
						result.add(new Pair<>(frame.image(), frame.delay()));
					}

					return result;
				}
				catch (IOException e) {
					lastEx = e;
				}
			}
		}

		if (lastEx != null) throw new IOException("All image readers failed", lastEx);
		return Collections.emptyList();
	}

	private static List<Pair<BufferedImage, Integer>> readGifFrames(InputStream input) {
		var decoder = new GifDecoder();
		decoder.read(input);

		int count = decoder.getFrameCount();
		var frames = new ArrayList<Pair<BufferedImage, Integer>>(count);

		for (int i = 0; i < count; ++i) {
			int delay = decoder.getDelay(i);
			frames.add(new Pair<>(decoder.getFrame(i), delay <= 10 ? 100 : delay));
		}

		return frames;
	}

	private static List<ImageFrame> readFrames(ImageReader reader) throws IOException {
		var frames = new ArrayList<ImageFrame>(1);

		try {
			BufferedImage master = null;
			Graphics2D baseImage = null;
			int width = -1;
			int height = -1;

			int numFrames = reader.getNumImages(true);
			frames.ensureCapacity(numFrames);

			final var size = extractLogicalScreenSize(reader);
			if (size != null) {
				width = size.width;
				height = size.height;
			}
			else {
				for (int frameIndex = 0; frameIndex < numFrames; ++frameIndex) {
					var meta = getFrameMetadata(reader, frameIndex);
					width = Math.max(width, reader.getWidth(frameIndex) + meta.x);
					height = Math.max(height, reader.getHeight(frameIndex) + meta.y);
				}
			}

			for (int frameIndex = 0; frameIndex < numFrames; ++frameIndex) {
				BufferedImage img;
				try {
					img = reader.read(frameIndex);
				}
				catch (IndexOutOfBoundsException e) {
					break;
				}

				var imageMetadata = getFrameMetadata(reader, frameIndex);
				int x = imageMetadata.x;
				int y = imageMetadata.y;

				if (master == null) {
					master = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
					baseImage = prepareGraphics(master);
				}

				baseImage.setComposite(imageMetadata.alphaSrcOver ? AlphaComposite.SrcOver : AlphaComposite.Src);
				baseImage.drawImage(img, x, y, TRANSPARENT, null);

				var copy = cloneImage(master);
				var imageFrame = new ImageFrame(copy, imageMetadata.delay, imageMetadata.disposal);
				frames.add(imageFrame);

				if (DisposalType.PREVIOUS == imageMetadata.disposal) {
					BufferedImage from = null;
					for (int i = frameIndex - 1; i >= 0; --i) {
						var frame = frames.get(i);
						if (DisposalType.PREVIOUS != frame.disposal || frameIndex == 0) {
							from = frame.image;
							break;
						}
					}
					if (from != null) {
						master = cloneImage(from);
						baseImage = prepareGraphics(master);
					}
				}
				else if (imageMetadata.disposal == DisposalType.BACKGROUND || imageMetadata.disposal == DisposalType.REPLACE) {
					baseImage.clearRect(x, y, img.getWidth(), img.getHeight());
				}

			}
			return frames;
		}
		finally {
			reader.dispose();
		}
	}

	private static BufferedImage cloneImage(BufferedImage image) {
		var raster = image.copyData(image.getRaster().createCompatibleWritableRaster());
		return new BufferedImage(image.getColorModel(), raster, image.isAlphaPremultiplied(), null);
	}

	private static Graphics2D prepareGraphics(BufferedImage image) {
		var g = image.createGraphics();
		g.setBackground(new Color(0, 0, 0, 0));
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		return g;
	}

	private static Dimension extractLogicalScreenSize(ImageReader reader) {
		try {
			var metadata = reader.getStreamMetadata();
			if (metadata == null) return null;

			var globalRoot = (IIOMetadataNode)metadata.getAsTree(metadata.getNativeMetadataFormatName());
			var globalScreenDescriptor = globalRoot.getElementsByTagName("LogicalScreenDescriptor");
			if (globalScreenDescriptor.getLength() > 0) {
				var screenDescriptor = (IIOMetadataNode)globalScreenDescriptor.item(0);
				if (screenDescriptor != null) {
					return new Dimension(Integer.parseInt(screenDescriptor.getAttribute("logicalScreenWidth")),
						Integer.parseInt(screenDescriptor.getAttribute("logicalScreenHeight")));
				}
			}
		}
		catch (IOException e) {
			StreamotesCommon.loge("Display metadata extraction failed", e);
		}
		return null;
	}

	private static FrameMetadata getFrameMetadata(ImageReader reader, int frameIndex) throws IOException {
		FrameMetadata result = null;
		if (reader.getFormatName().equalsIgnoreCase("gif"))
			result = fromGifMetadata(reader, frameIndex);
		else if (reader.getClass().getName().equals("com.twelvemonkeys.imageio.plugins.webp.WebPImageReader"))
			result = fromWebPImageReader(reader, frameIndex);
		return result != null ? result : new FrameMetadata(0, 0, 100, DisposalType.BACKGROUND, false);
	}

	private static FrameMetadata fromGifMetadata(ImageReader reader, int frameIndex) {
		try {
			var metadata = reader.getImageMetadata(frameIndex);
			var root = (IIOMetadataNode)metadata.getAsTree("javax_imageio_gif_image_1.0");
			var gce = (IIOMetadataNode)root.getElementsByTagName("GraphicControlExtension").item(0);
			int delay = Integer.parseInt(gce.getAttribute("delayTime")) * 10;
			if (delay < 20) delay = 100;
			String disposal = gce.getAttribute("disposalMethod");
			int x = 0, y = 0;
			var children = root.getChildNodes();
			for (int nodeIndex = 0; nodeIndex < children.getLength(); ++nodeIndex) {
				var nodeItem = children.item(nodeIndex);
				if ("ImageDescriptor".equalsIgnoreCase(nodeItem.getNodeName())) {
					var map = nodeItem.getAttributes();
					x = Integer.parseInt(map.getNamedItem("imageLeftPosition").getNodeValue());
					y = Integer.parseInt(map.getNamedItem("imageTopPosition").getNodeValue());
				}
			}
			return new FrameMetadata(x, y, delay, DisposalType.fromKey(disposal), true);
		}
		catch (IOException e) {
			StreamotesCommon.loge("GIF metadata extraction failed", e);
			return null;
		}
	}

	private static Field getField(Class<?> cls, String name) throws NoSuchFieldException {
		var f = cls.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	@SuppressWarnings("unchecked")
	private static FrameMetadata fromWebPImageReader(ImageReader reader, int frameIndex) {
		try {
			var framesData = (List<Object>)getField(reader.getClass(), "frames").get(reader);
			if (frameIndex >= framesData.size()) return null;

			var frameData = framesData.get(frameIndex);
			int duration = getField(frameData.getClass(), "duration").getInt(frameData);
			boolean blend = getField(frameData.getClass(), "blend").getBoolean(frameData);
			boolean dispose = getField(frameData.getClass(), "dispose").getBoolean(frameData);
			Rectangle bounds = (Rectangle)getField(frameData.getClass(), "bounds").get(frameData);
			return new FrameMetadata(bounds.x, bounds.y, duration, dispose ? DisposalType.BACKGROUND : DisposalType.NONE, blend);
		}
		catch (ReflectiveOperationException e) {
			StreamotesCommon.loge("WebP metadata extraction failed", e);
			return null;
		}
	}

	private record ImageFrame(BufferedImage image, int delay, DisposalType disposal) {
	}

	private record FrameMetadata(int x, int y, int delay, DisposalType disposal, boolean alphaSrcOver) {
	}

	private enum DisposalType {
		REPLACE("none"),
		NONE("doNotDispose"),
		BACKGROUND("restoreToBackgroundColor"),
		PREVIOUS("restoreToPrevious");

		public final String key;

		DisposalType(String key) {
			this.key = key;
		}

		public static DisposalType fromKey(String key) {
			for (var type : DisposalType.values()) {
				if (type.key.equals(key)) return type;
			}
			return REPLACE;
		}
	}
}
