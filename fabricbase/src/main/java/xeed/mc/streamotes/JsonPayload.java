package xeed.mc.streamotes;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.nio.charset.StandardCharsets;

record JsonPayload(String json) implements CustomPayload {
	public static final CustomPayload.Id<JsonPayload> PACKET_ID = new CustomPayload.Id<>(StreamotesCommon.IDENT);
	public static final PacketCodec<PacketByteBuf, JsonPayload> PACKET_CODEC = new PacketCodec<>() {
		public JsonPayload decode(PacketByteBuf byteBuf) {
			return new JsonPayload(byteBuf.readCharSequence(byteBuf.readableBytes(), StandardCharsets.UTF_8).toString());
		}

		public void encode(PacketByteBuf byteBuf, JsonPayload payload) {
			byteBuf.writeCharSequence(payload.json(), StandardCharsets.UTF_8);
		}
	};

	@Override
	public Id<? extends CustomPayload> getId() {
		return PACKET_ID;
	}
}
