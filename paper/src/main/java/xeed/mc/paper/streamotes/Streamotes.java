package xeed.mc.paper.streamotes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.Unpooled;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class Streamotes extends JavaPlugin implements Listener {
	public static final Pattern VALID_CHANNEL_PATTERN = Pattern.compile("[_a-zA-Z]\\w+");

	private static final String CHANNEL = "xeed:streamotes";
	private static final String COMMAND = "streamotes";
	private static final String RELOAD = "reload";

	private ModConfigModel config = new ModConfigModel();

	private String getConfigPath() {
		return getServer().getPluginsFolder().toPath().resolve("../config/streamotes.json5").toString();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
		if (event.getChannel().equals(CHANNEL)) {
			getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
				event.getPlayer().sendPluginMessage(this, CHANNEL, createConfigPacket());
				getLogger().info("Sent config packet to " + event.getPlayer().getName());
			}, 5);
		}
	}

	private void onReload() {
		reloadModConfig();
		getServer().sendPluginMessage(this, CHANNEL, createConfigPacket());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onReload(ServerResourcesReloadedEvent event) {
		onReload();
	}

	private void reloadModConfig() {
		var gson = new Gson();
		try (var input = new InputStreamReader(new FileInputStream(getConfigPath()))) {
			config = gson.fromJson(input, ModConfigModel.class);
			ModConfigModel.verifyChannels(config);
			getLogger().info("Config reloaded");
		}
		catch (IOException e) {
			getLogger().log(Level.SEVERE, "Config reload failed", e);
		}
	}

	private void saveModConfig() {
		var gson = new GsonBuilder().setPrettyPrinting().create();
		try (var output = new OutputStreamWriter(new FileOutputStream(getConfigPath()))) {
			ModConfigModel.verifyChannels(config);
			output.write(gson.toJson(config));
		}
		catch (IOException e) {
			getLogger().log(Level.SEVERE, "Config save failed", e);
		}
	}

	@Override
	public void onEnable() {
		if (!new File(getConfigPath()).exists()) {
			saveModConfig();
		}

		var cmd = Objects.requireNonNull(getCommand(COMMAND));
		cmd.setUsage("Usage: /streamotes [reload]");
		cmd.setTabCompleter(this);
		cmd.setExecutor(this);

		getServer().getPluginManager().registerEvents(this, this);
		getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
		reloadModConfig();
	}

	@Override
	public void onDisable() {
		getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
	}

	private byte[] createConfigPacket() {
		var buf = Unpooled.buffer();
		var json = new Gson().toJson(config, ModConfigModel.class);
		return json.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		if (command.getName().equals(COMMAND)) {
			if (args.length > 0 && RELOAD.startsWith(args[0])) {
				return List.of(RELOAD);
			}
		}

		return super.onTabComplete(sender, command, alias, args);
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (command.getName().equals(COMMAND)) {
			if (args.length > 0 && args[0].equals(RELOAD)) {
				if (!sender.hasPermission("xeed.mc.streamotes") && !sender.isOp()) {
					sender.sendMessage("Insufficient permissions to execute command!");
					return true;
				}

				onReload();
				return true;
			}
		}

		return super.onCommand(sender, command, label, args);
	}
}
