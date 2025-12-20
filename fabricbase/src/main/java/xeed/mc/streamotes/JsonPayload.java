package xeed.mc.streamotes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.nio.charset.StandardCharsets;

record JsonPayload(String json) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<JsonPayload> PACKET_ID = new CustomPacketPayload.Type<>(StreamotesCommon.IDENT);
	public static final StreamCodec<FriendlyByteBuf, JsonPayload> PACKET_CODEC = new StreamCodec<>() {
		public JsonPayload decode(FriendlyByteBuf byteBuf) {
			return new JsonPayload(byteBuf.readCharSequence(byteBuf.readableBytes(), StandardCharsets.UTF_8).toString());
		}

		public void encode(FriendlyByteBuf byteBuf, JsonPayload payload) {
			byteBuf.writeCharSequence(payload.json(), StandardCharsets.UTF_8);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PACKET_ID;
	}
}
