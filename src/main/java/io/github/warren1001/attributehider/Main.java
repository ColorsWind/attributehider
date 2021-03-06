package io.github.warren1001.attributehider;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.concurrency.ConcurrentPlayerMap;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

public class Main extends JavaPlugin implements Listener {

	private Remover remover;
	private List<Material> materials = new ArrayList<>();
	private ConcurrentMap<Player, Object> ignore = ConcurrentPlayerMap.usingName();
	private boolean whitelist = false;
	private static final Object PRESENT = new Object();
	
	@Override
	public void onEnable() {
		if (!setup())
			return;
		saveDefaultConfig();
		setMaterials();
		ProtocolLibrary.getProtocolManager().addPacketListener(
				new PacketAdapter(this, PacketType.Play.Server.WINDOW_ITEMS,
						PacketType.Play.Server.CUSTOM_PAYLOAD,
						PacketType.Play.Server.SET_SLOT) {

					@Override
					public void onPacketSending(PacketEvent event) {
						if (ignore.containsKey(event.getPlayer())) return;
						PacketContainer packet = event.getPacket();
						PacketType type = packet.getType();

						if (type == PacketType.Play.Server.WINDOW_ITEMS) {
							remover.remove(packet.getItemArrayModifier()
									.read(0));
						} else if (type == PacketType.Play.Server.SET_SLOT) {
							remover.remove(packet.getItemModifier().read(0));
						} else if (packet.getStrings().read(0)
								.equals("MC|TrList")) {
							remover.remove(event.getPlayer());
						}
					}

				});
		if (Bukkit.getPluginManager().getPlugin("Shopkeepers") != null) {
			getServer().getPluginManager().registerEvents(this, this);
		}
	}
	
	@EventHandler
	public void handle(InventoryOpenEvent e) {
		if (e.getInventory().getType() == InventoryType.MERCHANT) {
			ignore.put((Player) e.getPlayer(), PRESENT);
		}
	}
	
	@EventHandler
	public void handle(InventoryCloseEvent e) {
		ignore.remove(e.getPlayer());
	}
	
	@EventHandler
	public void handle(PlayerQuitEvent e) {
		ignore.remove(e.getPlayer());
	}


	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (cmd.getName().equalsIgnoreCase("attributehider")) {
			if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
				if (!sender.hasPermission(getConfig().getString(
						"command.permission"))) {
					sender.sendMessage(ChatColor
							.translateAlternateColorCodes('&', getConfig()
									.getString("command.no-permission")));
					return true;
				}
				setMaterials();
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
						getConfig().getString("command.reloaded")));
				return true;
			}
		}
		return false;
	}

	private boolean setup() {
		String version = getServer().getClass().getPackage().getName();
		version = version.substring(version.lastIndexOf('.') + 1);
		try {
			Field field = Class.forName(
					"net.minecraft.server." + version + ".ContainerMerchant")
					.getDeclaredField("merchant");
			field.setAccessible(true);
			remover = (Remover) Class
					.forName(
							"io.github.warren1001.attributehider.Remover"
									+ version.replaceAll("[a-zA-Z]", ""))
					.getConstructor(getClass(), Field.class)
					.newInstance(this, field);
			return true;
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException
				| SecurityException | NoSuchFieldException e) {
			getServer()
					.getConsoleSender()
					.sendMessage(
							ChatColor.GREEN
									+ "[AttributeHider] This plugin does not support your Minecraft server version. Disabling...");
			getServer().getPluginManager().disablePlugin(this);
			return false;
		}
	}

	private void setMaterials() {
		reloadConfig();
		materials.clear();
		whitelist = getConfig().getBoolean("list.use-whitelist");
		String listType = whitelist ? "whitelist" : "blacklist";
		List<String> list = getConfig().getStringList("list." + listType);
		for (String s : list) {
			String parsed = s.toUpperCase().trim().replace(' ', '_');
			Material type = Material.valueOf(parsed);
			if (type != null)
				materials.add(type);
			else
				getLogger()
						.warning(
								"Found invalid Material in list " + listType
										+ ": " + s);
		}
	}

	public boolean shouldBeModified(ItemStack item) {
		if (item == null)
			return false;
		Material type = item.getType();
		if (type == Material.AIR)
			return false;
		if (!getConfig().getBoolean("use-list"))
			return true;
		return type != Material.AIR
				&& (whitelist ? materials.contains(type) : !materials
						.contains(type));
	}

	@SuppressWarnings("deprecation")
	public boolean shouldBeModified(int id) {
		if (id == 0)
			return false;
		if (!getConfig().getBoolean("use-list"))
			return true;
		Material type = Material.getMaterial(id);
		return type != null
				&& (whitelist ? materials.contains(type) : !materials
						.contains(type));
	}

}
