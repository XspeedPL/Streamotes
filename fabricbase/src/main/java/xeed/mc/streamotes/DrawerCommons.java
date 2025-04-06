package xeed.mc.streamotes;

import org.joml.Matrix4f;
import xeed.mc.streamotes.emoticon.EmoticonRegistry;

public class DrawerCommons {
	public static void afterAccept(StringBuilder sb) {
		int startIx = sb.indexOf(Streamotes.CHAT_TRIGGER);
		int endIx = startIx == -1 ? -1 : sb.indexOf(Streamotes.CHAT_SEPARATOR, startIx + Streamotes.CHAT_TRIGGER.length());

		if (endIx != -1 || (startIx == -1 && sb.length() > Streamotes.CHAT_TRIGGER.length())) sb.setLength(0);
	}

	public static boolean atDrawGlyph(StringBuilder sb, boolean shadow, float x, float y, Matrix4f matrix, int color) {
		int startIx = sb.indexOf(Streamotes.CHAT_TRIGGER);
		if (startIx == -1) {
			return false;
		}
		else if (!shadow) {
			int endIx = sb.indexOf(Streamotes.CHAT_SEPARATOR, startIx + Streamotes.CHAT_TRIGGER.length());
			if (endIx == -1) return true;

			var code = sb.substring(startIx + Streamotes.CHAT_TRIGGER.length(), endIx);
			var icon = EmoticonRegistry.fromName(code);
			if (icon == null) return true;

			if (icon.getTexture().isLoaded()) {
				Streamotes.RENDER_QUEUE.get().addLast(new EmoteRenderInfo(icon, x, y, matrix.m33(), color));
			}
			else {
				icon.requestTexture();
			}
		}
		return true;
	}

	public static Float atGetAdvance(StringBuilder sb) {
		int startIx = sb.indexOf(Streamotes.CHAT_TRIGGER);
		if (startIx == -1) {
			return null;
		}
		else {
			int endIx = sb.indexOf(Streamotes.CHAT_SEPARATOR, startIx + Streamotes.CHAT_TRIGGER.length());
			if (endIx == -1) return 0f;

			var code = sb.substring(startIx + Streamotes.CHAT_TRIGGER.length(), endIx);
			var icon = EmoticonRegistry.fromName(code);

			return icon == null ? 8f : icon.getChatRenderWidth();
		}
	}
}
