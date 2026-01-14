package xeed.mc.streamotes;

public class ColorHelper {
	public static int getAlpha(int argb) {
		return argb >>> 24;
	}

	public static int getRed(int argb) {
		return argb >> 16 & 255;
	}

	public static int getGreen(int argb) {
		return argb >> 8 & 255;
	}

	public static int getBlue(int argb) {
		return argb & 255;
	}

	public static int getArgb(int alpha, int red, int green, int blue) {
		return alpha << 24 | red << 16 | green << 8 | blue;
	}

	public static int scaleRgb(int argb, float scale) {
		return getArgb(getAlpha(argb), clamp(((int)(getRed(argb) * scale))), clamp(((int)(getGreen(argb) * scale))), clamp(((int)(getBlue(argb) * scale))));
	}

	public static int withAlpha(int alpha, int rgb) {
		return alpha << 24 | rgb & 16777215;
	}

	private static int clamp(int value) {
		return Math.min(255, Math.max(value, 0));
	}
}
