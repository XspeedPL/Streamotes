package xeed.mc.streamotes.emoticon;

import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import xeed.mc.streamotes.ImageHandler;
import xeed.mc.streamotes.InternalMethods;
import xeed.mc.streamotes.Streamotes;
import xeed.mc.streamotes.api.IEmoticonLoader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;

public class Emoticon {
	private static class DrawImageCallback implements ImageObserver {
		private boolean isReady;

		public void prepare() {
			isReady = false;
		}

		@Override
		public boolean imageUpdate(Image img, int infoFlags, int x, int y, int width, int height) {
			if ((infoFlags & ALLBITS) == ALLBITS) {
				isReady = true;
			}
			else if ((infoFlags & ABORT) == ABORT) {
				isReady = true;
			}
			return false;
		}

		public boolean isReady() {
			return isReady;
		}
	}

	public final IEmoticonLoader loader;
	public final int priority;
	public final String code, source;

	private Object identifier;
	private Text tooltip;

	private boolean loadRequested;
	private int textureId = -1;
	private int width;
	private int height;
	private NativeImage loadBuffer;
	private BufferedImage tempBuffer;

	private int[] frameTimes;
	private int spriteSheetWidth;
	private int spriteSheetHeight;

	private long animationTime;
	private int currentFrameTime;
	private int currentFrame;
	private int currentFrameTexCoordX;
	private int currentFrameTexCoordY;
	private long lastRenderTime;

	public Emoticon(String source, String code, int priority, IEmoticonLoader loader) {
		this.priority = priority;
		this.code = code;
		this.source = source;
		this.loader = loader;

		tooltip = Text.literal(code);
	}

	public String getSource() {
		return source;
	}

	public String getName() {
		return code;
	}

	public Object getLoadData() {
		return identifier;
	}

	public void setLoadData(Object loadData) {
		this.identifier = loadData;
	}

	public void setTooltip(String extraInfo) {
		var text = Text.literal(code + "\n");
		text.append(Text.literal(extraInfo).setStyle(Style.EMPTY.withItalic(true)));
		tooltip = text;
	}

	public IEmoticonLoader getLoader() {
		return loader;
	}

	public void writeImage(File destination) throws IOException {
		ImageIO.write(tempBuffer, "png", destination);
	}

	public void setImage(BufferedImage image) throws IOException {
		currentFrameTexCoordX = 0;
		currentFrameTexCoordY = 0;
		this.frameTimes = null;
		spriteSheetWidth = image.getWidth();
		spriteSheetHeight = image.getHeight();
		width = spriteSheetWidth;
		height = spriteSheetHeight;
		tempBuffer = image;
		loadBuffer = InternalMethods.awtToNative(image);
	}

	public void setFrameData(int[] frameTimes, int spriteWidth, int spriteHeight) {
		currentFrameTexCoordX = 0;
		currentFrameTexCoordY = 0;
		this.frameTimes = frameTimes;
		width = spriteWidth;
		height = spriteHeight;
	}

	public void setImages(BufferedImage[] images, int[] frameTimes) throws IOException {
		currentFrameTexCoordX = 0;
		currentFrameTexCoordY = 0;
		this.frameTimes = frameTimes;
		width = images[0].getWidth();
		height = images[0].getHeight();
		int framesPerX = MathHelper.ceil(MathHelper.sqrt(images.length));
		int framesPerY = MathHelper.ceil(images.length / (float)framesPerX);
		spriteSheetWidth = width * framesPerX;
		spriteSheetHeight = height * framesPerY;
		tempBuffer = new BufferedImage(spriteSheetWidth, spriteSheetHeight, BufferedImage.TYPE_INT_ARGB);
		var callback = new DrawImageCallback();
		var g = tempBuffer.createGraphics();
		for (int y = 0; y < framesPerY; y++) {
			for (int x = 0; x < framesPerX; x++) {
				int frameIdx = x + y * framesPerX;
				if (frameIdx >= images.length) break;

				callback.prepare();
				if (!g.drawImage(images[frameIdx], x * width, y * height, ImageHandler.TRANSPARENT, callback)) {
					while (!callback.isReady()) {
						Streamotes.sleepSweetPrince(5);
					}
				}
			}
		}
		loadBuffer = InternalMethods.awtToNative(tempBuffer);
	}

	public int getTextureId() {
		if (loadBuffer != null) {
			textureId = TextureUtil.generateTextureId();
			TextureUtil.prepareImage(textureId, 0, loadBuffer.getWidth(), loadBuffer.getHeight());
			loadBuffer.upload(0, 0, 0, 0, 0, loadBuffer.getWidth(), loadBuffer.getHeight(), false, false, false, true);
			loadBuffer = null;
		}
		return textureId;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int[] getFrameTimes() {
		return frameTimes;
	}

	public float getChatRenderWidth() {
		var client = MinecraftClient.getInstance();
		float height = (float)(client.textRenderer.fontHeight + client.options.getChatLineSpacing().getValue() * 8);
		return getRenderWidth(height);
	}

	public float getRenderWidth(float renderHeight) {
		return height == 0 ? 0 : renderHeight * width / height;
	}

	public void requestTexture() {
		if (!loadRequested) {
			loadRequested = true;
			Streamotes.log("Requesting load of " + getName());
			AsyncEmoticonLoader.instance.loadAsync(this);
		}
	}

	public void disposeTexture() {
		if (textureId != -1) {
			TextureUtil.releaseTextureId(textureId);
		}
	}

	public Text getTooltip() {
		return tooltip;
	}

	public boolean isAnimated() {
		return frameTimes != null;
	}

	public void discardBitmap() {
		tempBuffer = null;
	}

	public void updateAnimation() {
		long now = System.currentTimeMillis();
		if (lastRenderTime == 0) {
			lastRenderTime = now;
			currentFrameTime = frameTimes[0];
		}
		animationTime += now - lastRenderTime;
		int lastFrame = currentFrame;
		while (animationTime > currentFrameTime) {
			animationTime -= currentFrameTime;
			if (++currentFrame >= frameTimes.length) {
				currentFrame = 0;
			}
			currentFrameTime = frameTimes[currentFrame];
		}
		if (currentFrame != lastFrame) {
			currentFrameTexCoordX = currentFrame * width % spriteSheetWidth;
			currentFrameTexCoordY = (currentFrame * width / spriteSheetWidth) * height;
		}
		lastRenderTime = now;
	}

	public int getCurrentFrameTexCoordX() {
		return currentFrameTexCoordX;
	}

	public int getCurrentFrameTexCoordY() {
		return currentFrameTexCoordY;
	}

	public int getSheetWidth() {
		return spriteSheetWidth;
	}

	public int getSheetHeight() {
		return spriteSheetHeight;
	}
}
