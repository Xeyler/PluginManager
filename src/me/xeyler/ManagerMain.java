package me.xeyler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ManagerMain extends JavaPlugin implements Listener {

	static ManagerMain instance = null;
	
	Inventory pluginsInv = Bukkit.createInventory(null, 54, "Plugins");
	HashMap<ItemStack, Plugin> pluginsMap = new HashMap<ItemStack, Plugin>();
	
	boolean sound;
	int interval;
	
	@Override
	public void onEnable() {

		this.getServer().getPluginManager().registerEvents(this, this);
		instance = this;
		
		File config = new File(getDataFolder(), "config.yml");
		if(!config.exists()) {
			
			getLogger().info("Generating config.yml...");
			saveDefaultConfig();
			getLogger().info("Done generating config.yml!");
		
		}
		
		sound = getConfig().getBoolean("playSound", true);
		if(getConfig().getInt("updateInterval") >= 1) {
			interval = getConfig().getInt("updateInterval") * 20;
		} else {
			interval = 20;
		}
		
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(instance, new Runnable() {
		
		@Override
		public void run() {
			
			Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(instance, new Runnable() {

				@Override
				public void run() {
					pluginsInv.clear();
					
					for(Plugin plugin : Bukkit.getPluginManager().getPlugins()) {

						ItemStack item;
						ChatColor color;
						String state;
						if(plugin.isEnabled()) {
							item = new ItemStack(Material.EMERALD_BLOCK);
							state = "Enabled";
							color = ChatColor.GREEN;
						} else {
							item = new ItemStack(Material.REDSTONE_BLOCK);
							state = "Disabled";
							color = ChatColor.RED;
						}
						ItemMeta meta = item.getItemMeta();
						meta.setDisplayName(color + ChatColor.BOLD.toString() + plugin.getName() + " v" + plugin.getDescription().getVersion() + " - " + state);
						
						meta.setLore(lore(plugin));
						item.setItemMeta(meta);
						
						pluginsInv.addItem(item);
						pluginsMap.put(item, plugin);
						
					}

				}
				
			}, interval, 0);
				
		}
			
		}, 0);

	}
	
	@Override
	public void onDisable() {
	
		
		
	}
	
	@EventHandler
	public void preCommand(PlayerCommandPreprocessEvent event) {
		
		if(event.getMessage().equalsIgnoreCase("/plugins")) {
			
			event.setCancelled(true);
			
			if(event.getPlayer().hasPermission("Manager.view")) {
				
				event.getPlayer().openInventory(pluginsInv);
					
			}
		
		}
			
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent event) {
		
		if(event.getInventory().equals(pluginsInv)) {
			
			event.setCancelled(true);
			
			if(event.getRawSlot() < event.getView().getTopInventory().getSize()) {

				Plugin plugin = pluginsMap.get(event.getCurrentItem());
				pluginsMap.remove(event.getCurrentItem());
			
				if(event.getCurrentItem().getType().equals(Material.EMERALD_BLOCK)) {
				
					if(!plugin.equals(this) || getConfig().getBoolean("selfDestruct", false)) {

						if(event.getClick().equals(ClickType.RIGHT) || event.getClick().equals(ClickType.SHIFT_RIGHT)) {

							if(event.getWhoClicked().hasPermission("Manager.eject")) {
							
							ItemMeta meta = event.getCurrentItem().getItemMeta();
							meta.setDisplayName(ChatColor.BLUE + ChatColor.BOLD.toString() + plugin.getName() + " v" + plugin.getDescription().getVersion() + " - Ejecting");
							event.getCurrentItem().setItemMeta(meta);
							event.getCurrentItem().setType(Material.LAPIS_BLOCK);
							
							try{
								getLogger().info("Unloading " + plugin.getName());
								((Player) event.getWhoClicked()).sendMessage(ChatColor.BLUE + "Ejecting " + ChatColor.BLUE + ChatColor.BOLD.toString() + plugin.getName() + "...");
								ManagerFunctions.remove(plugin);
							} catch(Exception error) {
								getLogger().warning("There was an error ejecting " + plugin.getName() + ": " + error.getLocalizedMessage());
								return;
							}
						
							getLogger().info(" --- " + plugin.getName() + " was ejected by " + event.getWhoClicked().getName() + " --- ");
							((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "You ejected " + ChatColor.RED + ChatColor.BOLD.toString() + plugin.getName() + ".");
							pluginsInv.remove(event.getCurrentItem());
							pluginsMap.remove(event.getCurrentItem());
							if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.CLICK, 1, 1);
							
							}
							
						} else {
							
							if(event.getWhoClicked().hasPermission("Manager.disable")) {
							
							try{
								Bukkit.getPluginManager().disablePlugin(plugin);
							} catch(Exception error) {
								getLogger().warning("There was an error disabling " + plugin.getName() + ": " + error.getLocalizedMessage());
								return;
							}
						
							getLogger().info(" --- " + plugin.getName() + " was disabled by " + event.getWhoClicked().getName() + " --- ");
							((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "You disabled " + ChatColor.RED + ChatColor.BOLD.toString() + plugin.getName() + ".");
							event.getCurrentItem().setType(Material.REDSTONE_BLOCK);
							ItemMeta meta = event.getCurrentItem().getItemMeta();
							meta.setDisplayName(ChatColor.RED + ChatColor.BOLD.toString() + plugin.getName() + " v" + plugin.getDescription().getVersion() + " - Disabled");
							event.getCurrentItem().setItemMeta(meta);
							pluginsMap.put(event.getCurrentItem(), plugin);
							if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.CLICK, 1, 1);
							
							}
							
						}
						
					} else {
						
						((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + this.getName() + ChatColor.RED + " can't disable itself!");
						if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.BLAZE_HIT, 1, 1);
						
					}
					
				} else if(event.getCurrentItem().getType().equals(Material.REDSTONE_BLOCK)) {

					if(event.getClick().equals(ClickType.RIGHT) || event.getClick().equals(ClickType.SHIFT_RIGHT)) {

						if(event.getWhoClicked().hasPermission("Manager.eject")) {
						
						ItemMeta meta = event.getCurrentItem().getItemMeta();
						meta.setDisplayName(ChatColor.BLUE + ChatColor.BOLD.toString() + plugin.getName() + " v" + plugin.getDescription().getVersion() + " - Ejecting");
						event.getCurrentItem().setItemMeta(meta);
						event.getCurrentItem().setType(Material.LAPIS_BLOCK);
						
						try{
							getLogger().info("Unloading " + plugin.getName());
							ManagerFunctions.remove(plugin);
						} catch(Exception error) {
							getLogger().warning("There was an error ejecting " + plugin.getName() + ": " + error.getLocalizedMessage());
							return;
						}
					
						getLogger().info(" --- " + plugin.getName() + " was ejected by " + event.getWhoClicked().getName() + " --- ");
						((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "You ejected " + ChatColor.RED + ChatColor.BOLD.toString() + plugin.getName() + ".");
						pluginsInv.remove(event.getCurrentItem());
						pluginsMap.remove(event.getCurrentItem());
						if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.CLICK, 1, 1);
						
						}
						
					} else {

						if(event.getWhoClicked().hasPermission("Manager.enable")) {
						
						try{
							Bukkit.getPluginManager().enablePlugin(plugin);
						} catch(Exception error) {
							getLogger().warning("There was an error enabling " + plugin.getName() + ": " + error.getLocalizedMessage());
							return;
						}
					
						getLogger().info(" --- " + plugin.getName() + " was enabled by " + event.getWhoClicked().getName() + " --- ");
						((Player) event.getWhoClicked()).sendMessage(ChatColor.GREEN + "You enabled " + ChatColor.GREEN + ChatColor.BOLD.toString() + plugin.getName() + ".");
						event.getCurrentItem().setType(Material.EMERALD_BLOCK);
						ItemMeta meta = event.getCurrentItem().getItemMeta();
						meta.setDisplayName(ChatColor.GREEN + ChatColor.BOLD.toString() + plugin.getName() + " v" + plugin.getDescription().getVersion() + " - Enabled");
						event.getCurrentItem().setItemMeta(meta);
						pluginsMap.put(event.getCurrentItem(), plugin);
						if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.CLICK, 1, 1);
				
						}
						
					}
					
				}
			
			}
			
		}
		
	}

	private List<String> lore(Plugin plugin) {
		
		List<String> pluginLore = new ArrayList<String>();
		pluginLore.add("");
		pluginLore.add(ChatColor.GRAY + ChatColor.UNDERLINE.toString() + ChatColor.BOLD.toString() + "Authors");
		if(!plugin.getDescription().getAuthors().toString().equals("[]")) {
			String author = plugin.getDescription().getAuthors().toString();
			pluginLore.add("  " + ChatColor.GRAY + author.substring(1, author.length()-1));
		} else {
			pluginLore.add(ChatColor.GRAY + "  (Unavailable)");
		}
		pluginLore.add(ChatColor.GRAY + ChatColor.UNDERLINE.toString() + ChatColor.BOLD.toString() + "Description");
		if(!(plugin.getDescription().getDescription() == null)) {
			for(String description : WordUtils.wrap(plugin.getDescription().getDescription(), 30).split(System.getProperty("line.separator"))) {
				pluginLore.add(ChatColor.GRAY + "  " + description);
			}
		} else {
			pluginLore.add(ChatColor.GRAY + "  (Unavailable)");
		}
		pluginLore.add("");
		pluginLore.add(ChatColor.GRAY + ChatColor.BOLD.toString() + "Left-Click " + ChatColor.GRAY + "to enable/disable.");
		pluginLore.add(ChatColor.GRAY + ChatColor.BOLD.toString() + "Right-Click " + ChatColor.GRAY + "to eject plugin.");
		return pluginLore;
		
	}

	public static ManagerMain getInstance() {
		
		return instance;
		
	}
	
}
