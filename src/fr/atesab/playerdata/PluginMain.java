package fr.atesab.playerdata;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

public class PluginMain extends JavaPlugin {
	private static final Gson gson = new Gson();
	@Override
	public void onEnable() {
		getCommand("getinfo").setExecutor(new CommandExecutor() {
			@Override
			public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
				if(args.length==0) return false;
				ChatComponentBuilder b = new ChatComponentBuilder("");
		        List<PlayerData> players = new ArrayList<PlayerData>();
				try {
					HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mojang.com/profiles/minecraft")
							.openConnection();
					connection.setRequestMethod("POST");
					connection.setRequestProperty("Content-Type", "application/json");
					String content = gson.toJson(args);
					connection.setRequestProperty("Content-Length", "" + content.getBytes().length);
					connection.setUseCaches(false);
					connection.setDoInput(true);
					connection.setDoOutput(true);
					DataOutputStream output = new DataOutputStream(connection.getOutputStream());
					output.writeBytes(content);
					output.flush();
			        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					StringBuilder result = new StringBuilder();String line;
			        while ((line = reader.readLine()) != null)
			            result.append("\n"+line);
			        @SuppressWarnings("unchecked")
					List<LinkedTreeMap<String, Object>> rl = gson.fromJson(result.toString().substring(1),List.class);
			        for (LinkedTreeMap<String, Object> lmt: rl) 
			        	players.add(gson.fromJson((gson.toJson(lmt)), PlayerData.class));
			        reader.close();
				} catch (IOException e) {
					b.append("Exception: " + e.getMessage()).setColor(ChatColor.RED);
					e.printStackTrace();
				}
				b.append("UUID : ").setColor(ChatColor.DARK_AQUA);
				for (String player: args) {
					b.append("\n- ").setColor(ChatColor.GRAY).append(player).setColor(ChatColor.WHITE);
					for (OfflinePlayer op: getServer().getOperators())
						if(op.getName().equalsIgnoreCase(player)) {
							b.append("[").setColor(ChatColor.DARK_PURPLE).append("Op").setColor(ChatColor.LIGHT_PURPLE)
							.append("]").setColor(ChatColor.DARK_PURPLE);
							break;
						}
					for (PlayerData pd: players)
						if(pd.getName().equalsIgnoreCase(player)) {
							String uuid = pd.getId();
							b.append("\n-- Online: ").setColor(ChatColor.AQUA).append(uuid.length()==32?
									uuid.substring(0, 8)+"-"+uuid.substring(8, 12)+"-"+uuid.substring(12, 16)+"-"+uuid.substring(16, 20)+"-"+uuid.substring(20)
									:uuid).setColor(ChatColor.WHITE);
							break;
						}
					b.append("\n-- Offline: ").setColor(ChatColor.AQUA).append(UUID.nameUUIDFromBytes(("OfflinePlayer:" + player).getBytes(Charsets.UTF_8)).toString())
						.setColor(ChatColor.WHITE);
					Player p;
					if((p = getServer().getPlayer(player))!=null) {
						b.append("\n-- Ip: ").setColor(ChatColor.AQUA).append(p.getAddress().getAddress().getHostAddress()+":"+
								p.getAddress().getPort()).setColor(ChatColor.WHITE)
						.append("\n-- EntityId: ").setColor(ChatColor.AQUA).append(String.valueOf(p.getEntityId())).setColor(ChatColor.WHITE);
						int ping = -1;
						try {
							ping = (int) getField("ping", getMethod("getHandle", getCraftBukkitClass("entity.CraftPlayer").cast(p)));
						} catch (Exception e) {}
						b.append("\n-- Ping: ").setColor(ChatColor.AQUA).append(String.valueOf(ping))
							.setColor((ping < 0 ? ChatColor.WHITE : (ping < 150 ? ChatColor.DARK_GREEN : (ping < 300 ? ChatColor.GREEN : (ping < 600 ? ChatColor.GOLD : (ping < 1000 ? ChatColor.RED : ChatColor.DARK_RED))))))
							.append("\n-- GameMode: ").setColor(ChatColor.AQUA).append(p.getGameMode().toString()).setColor(ChatColor.WHITE);
					}
				}
				b.send(sender);
				return true;
			}
		});
		super.onEnable();
    }
	public static Class<?> getSClass(String type, String name) {
		try {
			return Class.forName(type + "." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + "." + name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static Object getMethod(String name, Object object) throws Exception {
		return getMethod(name, object, new Class<?>[] {}, new Object[] {});
	}
	public static Object getMethod(String name, Object object, Class<?>[] parameterTypes, Object[] parameters) throws Exception {
		return object.getClass().getMethod(name, parameterTypes).invoke(object, parameters);
	}
	public static Class<?> getCraftBukkitClass(String name){
		return getSClass("org.bukkit.craftbukkit", name);
	}
	public static class PlayerData {
		private String id;
		private String name;
		public String getId() {
			return id;
		}
		public String getName() {
			return name;
		}
	}
	public static Object getField(String name, Object object) throws Exception {
		return object.getClass().getField(name).get(object);
	}
	public static void setField(String name, Object object, Object value) throws Exception {
		Field f = object.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(object, value);
	}
}
