package xeed.mc.streamotes;

import xeed.mc.streamotes.emoticon.Emoticon;

public record EmoteRenderInfo(Emoticon icon, float x, float y, float z, float r, float g, float b, float alpha,
							  int light) {
}
