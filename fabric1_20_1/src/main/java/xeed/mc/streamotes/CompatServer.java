package xeed.mc.streamotes;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class CompatServer {
	public static final ResourceLocation IDENT = Objects.requireNonNull(ResourceLocation.tryBuild("xeed", StreamotesCommon.NAME));

	public static boolean permissionPredicate(CommandSourceStack source) {
		return source.hasPermission(3);
	}

	public static void onInitializeServer() {
	}

	public static Packet<?> createConfigPacket(String json) {
		var buf = PacketByteBufs.create();
		buf.writeUtf(json);
		return ServerPlayNetworking.createS2CPacket(Objects.requireNonNull(IDENT), buf);
	}

	public static void sendFeedback(CommandSourceStack source, Component message, boolean broadcastToOps) {
		source.sendSuccess(() -> message, broadcastToOps);
	}
}
