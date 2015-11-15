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
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ManagerMain extends JavaPlugin implements Listener {

	static ManagerMain instance = null;
	
	List<Inventory> inventories = new ArrayList<Inventory>();
	HashMap<ItemStack, Plugin> itemsMap = new HashMap<ItemStack, Plugin>();

	boolean sound;
	boolean destruct;
	
	@Override
	public void onEnable() {

		// register events
		this.getServer().getPluginManager().registerEvents(this, this);
		instance = this;
		
		// generate config.yml
		File config = new File(getDataFolder(), "config.yml");
		if(!config.exists()) {
			
			getLogger().info("Generating config.yml...");
			saveDefaultConfig();
			getLogger().info("Done generating config.yml!");
		
		}
		
		// get boolean "playSound"
		try {
			sound = getConfig().getBoolean("playSound");
		} catch(Exception error) {
			getLogger().warning("The 'playSound' parameter in PluginManager's config is not a boolean!");
			getLogger().warning("Reverting to default value of 'false'");
			getConfig().set("playSound", false);
			sound = false;
		}
		
		//get boolean "selfDestruct"
		try {
			destruct = getConfig().getBoolean("selfDestruct");
		} catch(Exception error) {
			getLogger().warning("The 'selfDestruct' parameter in PluginManager's config is not a boolean!");
			getLogger().warning("Reverting to default value of 'false'");
			getConfig().set("selfDestruct", false);
			destruct = false;
		}
		
		// schedule this for later so it happens after all plugins are loaded
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			
			// add blocks that represent plugins to inventories
				@Override
				public void run() {
			
					// create enough inventories to fit all the plugins
					for(int i = 1; i <= (int) Math.ceil(Bukkit.getPluginManager().getPlugins().length/45.0); i++) {
						Inventory inv = Bukkit.createInventory(null, 54, "Plugins - " + i);
						// add "next" arrow
						if((int) Math.ceil(Bukkit.getPluginManager().getPlugins().length/45.0) > i) {
							ItemStack next = new ItemStack(Material.ARROW, i + 1);
							ItemMeta meta = next.getItemMeta();
							meta.setDisplayName(ChatColor.GRAY + ChatColor.BOLD.toString() + "Next" + ChatColor.GRAY + " inventory");
							next.setItemMeta(meta);
							inv.setItem(53, next);
						}
						// add "previous" arrow
						if(i > 1) {
							ItemStack last = new ItemStack(Material.ARROW, i - 1);
							ItemMeta meta = last.getItemMeta();
							meta.setDisplayName(ChatColor.GRAY + ChatColor.BOLD.toString() + "Previous" + ChatColor.GRAY + " inventory");
							last.setItemMeta(meta);
							inv.setItem(45, last);
						}
						inventories.add(inv);
					}
				
					// add the blocks to the inventories
					int i = 1;
					for(Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
						
						ItemStack item = new ItemStack(Material.AIR);
						if(plugin.isEnabled()) {
							setEnabled(item, plugin);
						} else {
							setDisabled(item, plugin);
						}
						ItemMeta meta = item.getItemMeta();
						meta.setLore(lore(plugin));
						item.setItemMeta(meta);
						
						inventories.get((int) Math.ceil(i/45.0) - 1).addItem(item);
						itemsMap.put(item, plugin);
						
						i++;
						
					}
					
				} 
				
		}, 0);

	}
	
	// Stop the /plugins command and replace it with the custom /plugins command
	@EventHandler
	public void preCommand(PlayerCommandPreprocessEvent event) {
		
		if(event.getMessage().equalsIgnoreCase("/plugins")) {
			
			event.setCancelled(true);
			
			if(event.getPlayer().hasPermission("Manager.view")) {
				
				// TODO: Open inventory that player last had open(be sure to check it exists first)
				event.getPlayer().openInventory(inventories.get(0));
					
			}
		
		}
			
	}
	
	// Catch plugin enable events to update the blocks' states
	@EventHandler
	public void onPluginEnable(PluginEnableEvent event) {
		
		for(ItemStack item : itemsMap.keySet()) {
			
			if(itemsMap.get(item) == event.getPlugin()) {
				
				setEnabled(item, event.getPlugin());
				return;
				
			}
			
		}
		
	}
	
	// Catch plugin disable events to update blocks' states
	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		
		for(ItemStack item : itemsMap.keySet()) {
			
			if(itemsMap.get(item) == event.getPlugin()) {
				
				setDisabled(item, event.getPlugin());
				return;
				
			}
			
		}
		
	}

	private void setEnabled(ItemStack item, Plugin plugin) {
		
		itemsMap.remove(item);
		item.setType(Material.EMERALD_BLOCK);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + ChatColor.BOLD.toString() + plugin.getName() + " v" + plugin.getDescription().getVersion() + " - Enabled");
		item.setItemMeta(meta);
		itemsMap.put(item, plugin);
		
	}
	
	private void setDisabled(ItemStack item, Plugin plugin) {
		
		itemsMap.remove(item);
		item.setType(Material.REDSTONE_BLOCK);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.RED + ChatColor.BOLD.toString() + plugin.getName() + " v" + plugin.getDescription().getVersion() + " - Disabled");
		item.setItemMeta(meta);
		itemsMap.put(item, plugin);
		
	}

	// Catch inventory click events to enable or disable plugins
	@EventHandler
	public void onClick(InventoryClickEvent event) {
		
		// if the clicked inventory was a PluginManager inventory
		if(inventories.contains(event.getClickedInventory()) && event.getWhoClicked() instanceof Player) {
			
			event.setCancelled(true);
			
			// if it was clicked on the top part(not the player's inventory)
			if(event.getRawSlot() < event.getView().getTopInventory().getSize()) {

				if(event.getCurrentItem().getType().equals(Material.ARROW)) {
					
					int current = inventories.indexOf(event.getClickedInventory());
					
					if(event.getRawSlot() == 45) {
						
						((Player) event.getWhoClicked()).openInventory(inventories.get(current - 1));
						
					}
					
					if(event.getRawSlot() == 53) {
						
						((Player) event.getWhoClicked()).openInventory(inventories.get(current + 1));
						
					}
					
				}
				
				if(event.getCurrentItem().getType().equals(Material.EMERALD_BLOCK)) {

					Plugin plugin = itemsMap.get(event.getCurrentItem());
					
					if(!plugin.equals(this) || destruct) {

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
								getLogger().warning("There was an error ejecting  " + plugin.getName() + ": " + error.getLocalizedMessage());
								((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "There was an error ejecting " + plugin.getName() + "!");
								if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.BLAZE_HIT, 1, 1);
								return;
							}
						
							getLogger().info(" --- " + plugin.getName() + " was ejected by " + event.getWhoClicked().getName() + " --- ");
							((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "You ejected " + ChatColor.RED + ChatColor.BOLD.toString() + plugin.getName() + ".");
							event.getClickedInventory().remove(event.getCurrentItem());
							if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.CLICK, 1, 1);
							
							}
							
						} else {
							
							if(event.getWhoClicked().hasPermission("Manager.disable")) {
							
							try{
								Bukkit.getPluginManager().disablePlugin(plugin);
							} catch(Exception error) {
								getLogger().warning("There was an error disabling  " + plugin.getName() + ": " + error.getLocalizedMessage());
								((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "There was an error disabling " + plugin.getName() + "!");
								if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.BLAZE_HIT, 1, 1);
								return;
							}
						
							getLogger().info(" --- " + plugin.getName() + " was disabled by " + event.getWhoClicked().getName() + " --- ");
							((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "You disabled " + ChatColor.RED + ChatColor.BOLD.toString() + plugin.getName() + ".");
							
							setDisabled(event.getCurrentItem(), plugin);
							itemsMap.put(event.getCurrentItem(), plugin);
							if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.CLICK, 1, 1);
							
							}
							
						}
						
					} else {
						
						((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + this.getName() + ChatColor.RED + " can't disable itself!");
						if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.BLAZE_HIT, 1, 1);
						
					}
					
				} else if(event.getCurrentItem().getType().equals(Material.REDSTONE_BLOCK)) {

					Plugin plugin = itemsMap.get(event.getCurrentItem());
					
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
							getLogger().warning("There was an error ejecting  " + plugin.getName() + ": " + error.getLocalizedMessage());
							((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "There was an error ejecting " + plugin.getName() + "!");
							if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.BLAZE_HIT, 1, 1);
							return;
						}
					
						getLogger().info(" --- " + plugin.getName() + " was ejected by " + event.getWhoClicked().getName() + " --- ");
						((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "You ejected " + ChatColor.RED + ChatColor.BOLD.toString() + plugin.getName() + ".");
						event.getClickedInventory().remove(event.getCurrentItem());
						itemsMap.remove(event.getCurrentItem());
						if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.CLICK, 1, 1);
						
						}
						
					} else {

						if(event.getWhoClicked().hasPermission("Manager.enable")) {
						
						try{
							Bukkit.getPluginManager().enablePlugin(plugin);
						} catch(Exception error) {
							getLogger().warning("There was an error enabling  " + plugin.getName() + ": " + error.getLocalizedMessage());
							((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "There was an error enabling " + plugin.getName() + "!");
							if(sound)((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.BLAZE_HIT, 1, 1);
							return;
						}
					
						getLogger().info(" --- " + plugin.getName() + " was enabled by " + event.getWhoClicked().getName() + " --- ");
						((Player) event.getWhoClicked()).sendMessage(ChatColor.GREEN + "You enabled " + ChatColor.GREEN + ChatColor.BOLD + plugin.getName() + ".");
						setEnabled(event.getCurrentItem(), plugin);
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
		pluginLore.add(ChatColor.GRAY + ChatColor.UNDERLINE.toString() + ChatColor.BOLD + "Authors");
		if(!plugin.getDescription().getAuthors().toString().equals("[]")) {
			String author = plugin.getDescription().getAuthors().toString();
			pluginLore.add("  " + ChatColor.GRAY + author.substring(1, author.length()-1));
		} else {
			pluginLore.add(ChatColor.GRAY + "  (Unavailable)");
		}
		pluginLore.add(ChatColor.GRAY + ChatColor.UNDERLINE.toString() + ChatColor.BOLD + "Description");
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
