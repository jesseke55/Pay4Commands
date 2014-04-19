package de.CodingDev.Pay4Commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import de.CodingDev.Pay4Commands.UpdateChecker.Updater;
import de.CodingDev.Pay4Commands.UpdateChecker.Updater.UpdateResult;
import de.CodingDev.Pay4Commands.UpdateChecker.Updater.UpdateType;
import de.CodingDev.Pay4Commands.org.mcstats.Metrics;

public class Pay4Commands extends JavaPlugin implements Listener{
	public static Economy econ = null;
	public String prefix = "§8[§9Pay4Commands§8] §6";
	public boolean newVersion = false;
	public String versionNumber = "";

	public void onEnable(){
		loadConfiguration();
		getServer().getPluginManager().registerEvents(this, this);
		if (!setupEconomy()){
			getLogger().warning(String.format("[%s] - Disabled due to no Vault dependency found!", new Object[] { getDescription().getName() }));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (!getConfig().getBoolean("showPrefix")) {
			this.prefix = "§8[§9Pay4Commands§8] §6";
		}
		getLogger().info("Pay4Commands has been enabled!");
		getLogger().info("-------------------------");
		getLogger().info("By: R3N3PDE");
		getLogger().info("Website: http://codingdev.de/");
		getLogger().info("Website: http://r3n3p.de/");
		getLogger().info("Updates: http://dev.bukkit.org/server-mods/pay4commands/");
		getLogger().info("Version: " + getDescription().getVersion());
		getLogger().info("-------------------------");
		getLogger().info("Checking for Updates...");
		Updater updater = new Updater(this, 45061, this.getFile(), UpdateType.NO_DOWNLOAD, true);
		if (updater.getResult() == UpdateResult.UPDATE_AVAILABLE) {
		    getLogger().info("New version available! " + updater.getLatestName());
		    newVersion = true;
		    versionNumber = updater.getLatestName();
		}else if (updater.getResult() == UpdateResult.NO_UPDATE) {
		    getLogger().info("No new version available");
		}else{
		    getLogger().info("Updater: " + updater.getResult());
		}
		try
		{
			Metrics metrics = new Metrics(this);
			metrics.start();
		}catch (IOException localIOException) {}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e){
		if(newVersion){
			e.getPlayer().sendMessage(String.format(this.prefix + getConfig().getString(new StringBuilder(String.valueOf(getConfig().getString("language"))).append(".newVersion").toString()), versionNumber));
		}
	}
	
	public void debug(Player sender, String message){
		if(getConfig().getBoolean("showDebug")){
			sender.sendMessage(prefix + "[Debug] " + message);
			getServer().broadcastMessage(prefix + "[Debug] " + message);
		}
	}
	private void debug(String string) {
		if(getConfig().getBoolean("showDebug")){
			getServer().broadcastMessage(prefix + "[Debug] " + string);
		}
	}

	public void onDisable(){
		getLogger().info("Pay4Commands has been disabled!");
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("pay4commands")){
			if (sender.hasPermission("pay4commands.reload")){
				reloadConfig();
				sender.sendMessage(this.prefix + "Settings reloaded.");
			}else{
				sender.sendMessage(this.prefix + getConfig().getString("msgNoPermissions"));
			}
			return true;
		}
		return false;
	}

	private boolean setupEconomy(){
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = (Economy)rsp.getProvider();
		return econ != null;
	}

	@EventHandler
	public void onPlayerCommandPreProcess(PlayerCommandPreprocessEvent event){
		Player player = event.getPlayer();
		String cmd = event.getMessage();
		if (player.hasPermission("pay4commands.use")){
			if (!player.hasPermission("pay4commands.free")){
				for (Object o : getConfig().getList("commands")){
					String result = (String)o;
					String[] getInfos = result.split(":");
					String[] items = getInfos[1].split(",");
					
					if(getInfos.length == 2){
						boolean payed = false;
						int run = 0;
						for(String itemD : items){
							if(!payed){
								String itemData[] = itemD.split(";");
								if(itemData.length == 1){
									debug(player, "Check 1");
									if (check(getInfos[0], cmd)) {
										EconomyResponse r = econ.withdrawPlayer(player.getName(), Integer.parseInt(itemD));
										if (r.transactionSuccess()){
											if(getConfig().getBoolean("showSuccessMessage")){
												player.sendMessage(this.prefix + replace(getConfig().getString(new StringBuilder(String.valueOf(getConfig().getString("language"))).append(".msgSuccess").toString()), String.valueOf(r.amount), getInfos[0], getConfig().getString("currency")));
											}
											payed = true;
											event.setCancelled(false);
											debug(player, "Pay with Money");
											//method(2);
										}else{
											if(run > 0){
												player.sendMessage(this.prefix + replace(getConfig().getString(new StringBuilder(String.valueOf(getConfig().getString("language"))).append(".msgNotEnoughMoney").toString()), getInfos[1], getInfos[0], getConfig().getString("currency")));
											}else{	
												player.sendMessage(this.prefix + replace(getConfig().getString(new StringBuilder(String.valueOf(getConfig().getString("language"))).append(".msgNotEnoughMoney").toString()), getInfos[1], getInfos[0], getConfig().getString("currency")));
											}
											event.setCancelled(true);
										}
									}
								}else if(itemData.length == 3){
									debug(player, "Check 2");
									try{
										int item = Integer.parseInt(itemData[0]);
										int data = Integer.parseInt(itemData[1]);
										int amount = Integer.parseInt(itemData[2]);
										ItemStack i = new ItemStack(item, amount, (byte)data);
										if (!check(getInfos[0], cmd)) {
											continue;
										}
										if (checkForAmount(new ArrayList(player.getInventory().all(Material.getMaterial(item)).values()), amount, (byte)data)){
											player.getInventory().removeItem(new ItemStack[] { i });
											if(getConfig().getBoolean("showSuccessMessage")){
												player.sendMessage(this.prefix + replace(getConfig().getString(new StringBuilder(String.valueOf(getConfig().getString("language"))).append(".msgSuccess").toString()), Integer.toString(amount), getInfos[0], betterName(i)));
											}
											payed = true;
											event.setCancelled(false);
											debug(player, "Pay with Item");
											//method(1);
										}else{
											if(run > 0){
												player.sendMessage(this.prefix + replace(getConfig().getString(new StringBuilder(String.valueOf(getConfig().getString("language"))).append(".msgNotEnoughItemsOr").toString()), Integer.toString(amount), getInfos[0], betterName(i)));
											}else{
												player.sendMessage(this.prefix + replace(getConfig().getString(new StringBuilder(String.valueOf(getConfig().getString("language"))).append(".msgNotEnoughItems").toString()), Integer.toString(amount), getInfos[0], betterName(i)));
											}
											event.setCancelled(true);
										}
									}catch (Exception e){
										System.out.println("Error: " + e);
										event.setCancelled(true);
									}
								}else{
									invalidPaymentString(result, itemD, null);
								}
								run++;
							}
						}
					}else{
						invalidPaymentString(result, null, null);
					}
				}
			}else{
				debug(player, "Pay with None");
				//method(0);
			}
		}
		else{
			player.sendMessage(this.prefix + getConfig().getString(new StringBuilder(String.valueOf(getConfig().getString("language"))).append(".msgNoPermissions").toString()));
			event.setCancelled(true);
		}
	}

	private void invalidPaymentString(String result, String item, Object object) {
		getLogger().warning("Error on Command: "+result+" on item "+item+ ".");
	}

	private boolean checkForAmount(ArrayList<ItemStack> items, int amount, byte data){
		for (ItemStack i : items) {
			if (i.getData().getData() == data) {
				amount -= i.getAmount();
			}
		}
		if (amount <= 0) {
			return true;
		}
		return false;
	}

	public boolean check(String result, String cmd){
		debug("Res = " + result + " " + cmd);
		if (count(result, "*") > 0){
			String tempResult = result.replace("*", "");
			cmd.startsWith(tempResult);
			if (cmd.startsWith(tempResult)) {
				return true;
			}
				return false;
		}
		if (cmd.equalsIgnoreCase(result)) {
			return true;
		}
		return false;
	}

	public int count(String input, String countString){
		return input.split("\\Q" + countString + "\\E", -1).length - 1;
	}

	String replace(String text, String price, String command, String currency){
		try{
			text = text.replace("%price%", price);
			text = text.replace("%command%", command);
			text = text.replace("%currency%", currency);
		}catch (Exception e){
			System.out.println(e);
		}
		return text;
	}
	
	public void loadConfiguration(){
		List commands = new ArrayList();
		commands.add("/pay:388;0;10");
		commands.add("/help:10");
		commands.add("/spawn:351;4;10,351;3;10");
		commands.add("/time set *:50");
		commands.add("/time set*:50");
		commands.add("/time:3");
		getConfig().options().header(
				  "############################################\n"
				+ "                 Hello Admin               #\n"
				+ "############################################\n"
				+ "Need Help with the Commands? http://dev.bukkit.org/bukkit-plugins/pay4commands/pages/how-to-using-pay4commands/ \n"
				+ "");
		
		getConfig().addDefault("commands", commands);
		getConfig().addDefault("usePermission", true);
		getConfig().addDefault("showPrefix", true);
		getConfig().addDefault("currency", "Coins");
		getConfig().addDefault("language", "enUS");
		getConfig().addDefault("showSuccessMessage", true);
		getConfig().addDefault("showDebug", false);
		
		getConfig().addDefault("enUS.msgNoPermissions", "You do not have the permission to use this command.");
		getConfig().addDefault("enUS.msgNotEnoughItems", "Not enough items you need §c%price% %currency%§6.");
		getConfig().addDefault("enUS.msgNotEnoughItemsOr", "Or you need §c%price% %currency%§6.");
		getConfig().addDefault("enUS.msgNotEnoughMoney", "Not enough money you need §c%price% %currency%§6.");
		getConfig().addDefault("enUS.msgNotEnoughMoneyOr", "Or you need §c%price% %currency%§6.");
		getConfig().addDefault("enUS.msgSuccess", "You have paid §c%price% %currency%§6 to use the command §c%command%§6.");
		getConfig().addDefault("enUS.newVersion", "We have released the version §c%s§6.");

		getConfig().addDefault("deDE.msgNoPermissions", "Du hast nicht die Berechtigung, um diesen Befehl zu verwenden.");
		getConfig().addDefault("deDE.msgNotEnoughItems", "Nicht genug Items, Du brauchst §c%price% %currency%§6.");
		getConfig().addDefault("deDE.msgNotEnoughItemsOr", "Oder Du brauchst §c%price% %currency%§6.");
		getConfig().addDefault("deDE.msgNotEnoughMoney", "Nicht genug Geld, Du brauchst §c%price% %currency%§6.");
		getConfig().addDefault("deDE.msgNotEnoughMoneyOr", "Oder Du brauchst §c%price% %currency%§6.");
		getConfig().addDefault("deDE.msgSuccess", "Du zahlst §c%price% %currency%§6, damit du den Befehl %command%§6 nutzen kannst.");
		getConfig().addDefault("deDE.newVersion", "Wir haben die Version §c%s§6 veröffentlicht!");

		getConfig().addDefault("frFR.msgNoPermissions", "Vous n'avez pas la permission d'utiliser cette commande.");
		getConfig().addDefault("frFR.msgNotEnoughItems", "Pas assez d'items vous avez besoin de §c%price% %currency%§6.");
		getConfig().addDefault("frFR.msgNotEnoughItemsOr", "Or you need §c%price% %currency%§6.");
		getConfig().addDefault("frFR.msgNotEnoughMoney", "Pas assez d'argent vous avez besoin de §c%price% %currency%§6.");
		getConfig().addDefault("frFR.msgNotEnoughMoneyOr", "Or you need §c%price% %currency%§6.");
		getConfig().addDefault("frFR.msgSuccess", "Vous payez §c%price% %currency%§6 pour utiliser la commande §c%command%§6.");
		getConfig().addDefault("frFR.newVersion", "We have has released the version §c%s§6.");

		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	
	private String betterName(ItemStack i) {
		String newName = formatName(i.getType().name().replace('_', ' ').toLowerCase());
		if(i.getType() == Material.INK_SACK){
			if(i.getDurability() == 0){
				newName = "Ink Sack";
			}else if(i.getDurability() == 1){
				newName = "Rose Red";
			}else if(i.getDurability() == 2){
				newName = "Cactus Green";
			}else if(i.getDurability() == 3){
				newName = "Coco Beans";
			}else if(i.getDurability() == 4){
				newName = "Lapis Lazuli";
			}else if(i.getDurability() == 5){
				newName = "Purple Dye";
			}else if(i.getDurability() == 6){
				newName = "Cyan Dye";
			}else if(i.getDurability() == 7){
				newName = "Light Gray Dye";
			}else if(i.getDurability() == 8){
				newName = "Gray Dye";
			}else if(i.getDurability() == 9){
				newName = "Pink Dye";
			}else if(i.getDurability() == 10){
				newName = "Lime Dye";
			}else if(i.getDurability() == 11){
				newName = "Dandelion Yellow";
			}else if(i.getDurability() == 12){
				newName = "Light Blue Dye";
			}else if(i.getDurability() == 13){
				newName = "Magenta Dye";
			}else if(i.getDurability() == 14){
				newName = "Orange Dye";
			}else if(i.getDurability() == 15){
				newName = "Bone Meal";
			}
		}else if(i.getType() == Material.WOOL){
			if(i.getDurability() == 0){
				newName = "White Wool";
			}else if(i.getDurability() == 1){
				newName = "Orange Wool";
			}else if(i.getDurability() == 2){
				newName = "Magenta Wool";
			}else if(i.getDurability() == 3){
				newName = "Light Blue Wool";
			}else if(i.getDurability() == 4){
				newName = "Yellow Wool";
			}else if(i.getDurability() == 5){
				newName = "Lime Wool";
			}else if(i.getDurability() == 6){
				newName = "Pink Wool";
			}else if(i.getDurability() == 7){
				newName = "Gray Wool";
			}else if(i.getDurability() == 8){
				newName = "Light Gray Wool";
			}else if(i.getDurability() == 9){
				newName = "Cyan Wool";
			}else if(i.getDurability() == 10){
				newName = "Purple Wool";
			}else if(i.getDurability() == 11){
				newName = "Blue Wool";
			}else if(i.getDurability() == 12){
				newName = "Brown Wool";
			}else if(i.getDurability() == 13){
				newName = "Green Wool";
			}else if(i.getDurability() == 14){
				newName = "Red Wool";
			}else if(i.getDurability() == 15){
				newName = "Black Wool";
			}
		}else if(i.getType() == Material.STAINED_CLAY){
			if(i.getDurability() == 0){
				newName = "White Stained Clay";
			}else if(i.getDurability() == 1){
				newName = "Orange Stained Clay";
			}else if(i.getDurability() == 2){
				newName = "Magenta Stained Clay";
			}else if(i.getDurability() == 3){
				newName = "Light Blue Stained Clay";
			}else if(i.getDurability() == 4){
				newName = "Yellow Stained Clay";
			}else if(i.getDurability() == 5){
				newName = "Lime Stained Clay";
			}else if(i.getDurability() == 6){
				newName = "Pink Stained Clay";
			}else if(i.getDurability() == 7){
				newName = "Gray Stained Clay";
			}else if(i.getDurability() == 8){
				newName = "Light Gray Stained Clay";
			}else if(i.getDurability() == 9){
				newName = "Cyan Stained Clay";
			}else if(i.getDurability() == 10){
				newName = "Purple Stained Clay";
			}else if(i.getDurability() == 11){
				newName = "Blue Stained Clay";
			}else if(i.getDurability() == 12){
				newName = "Brown Stained Clay";
			}else if(i.getDurability() == 13){
				newName = "Green Stained Clay";
			}else if(i.getDurability() == 14){
				newName = "Red Stained Clay";
			}else if(i.getDurability() == 15){
				newName = "Black Stained Clay";
			}
		}else if(i.getTypeId() == 95){
			if(i.getDurability() == 0){
				newName = "White Stained Glass";
			}else if(i.getDurability() == 1){
				newName = "Orange Stained Glass";
			}else if(i.getDurability() == 2){
				newName = "Magenta Stained Glass";
			}else if(i.getDurability() == 3){
				newName = "Light Blue Stained Glass";
			}else if(i.getDurability() == 4){
				newName = "Yellow Stained Glass";
			}else if(i.getDurability() == 5){
				newName = "Lime Stained Glass";
			}else if(i.getDurability() == 6){
				newName = "Pink Stained Glass";
			}else if(i.getDurability() == 7){
				newName = "Gray Stained Glass";
			}else if(i.getDurability() == 8){
				newName = "Light Gray Stained Glass";
			}else if(i.getDurability() == 9){
				newName = "Cyan Stained Glass";
			}else if(i.getDurability() == 10){
				newName = "Purple Stained Glass";
			}else if(i.getDurability() == 11){
				newName = "Blue Stained Glass";
			}else if(i.getDurability() == 12){
				newName = "Brown Stained Glass";
			}else if(i.getDurability() == 13){
				newName = "Green Stained Glass";
			}else if(i.getDurability() == 14){
				newName = "Red Stained Glass";
			}else if(i.getDurability() == 15){
				newName = "Black Stained Glass";
			}
		}else if(i.getTypeId() == 160){
			if(i.getDurability() == 0){
				newName = "White Stained Glass Pane";
			}else if(i.getDurability() == 1){
				newName = "Orange Stained Glass Pane";
			}else if(i.getDurability() == 2){
				newName = "Magenta Stained Glass Pane";
			}else if(i.getDurability() == 3){
				newName = "Light Blue Stained Glass Pane";
			}else if(i.getDurability() == 4){
				newName = "Yellow Stained Glass Pane";
			}else if(i.getDurability() == 5){
				newName = "Lime Stained Glass Pane";
			}else if(i.getDurability() == 6){
				newName = "Pink Stained Glass Pane";
			}else if(i.getDurability() == 7){
				newName = "Gray Stained Glass Pane";
			}else if(i.getDurability() == 8){
				newName = "Light Gray Stained Glass Pane";
			}else if(i.getDurability() == 9){
				newName = "Cyan Stained Glass Pane";
			}else if(i.getDurability() == 10){
				newName = "Purple Stained Glass Pane";
			}else if(i.getDurability() == 11){
				newName = "Blue Stained Glass Pane";
			}else if(i.getDurability() == 12){
				newName = "Brown Stained Glass Pane";
			}else if(i.getDurability() == 13){
				newName = "Green Stained Glass Pane";
			}else if(i.getDurability() == 14){
				newName = "Red Stained Glass Pane";
			}else if(i.getDurability() == 15){
				newName = "Black Stained Glass Pane";
			}
		}
		
		return newName;
	}

	private String formatName(String replace) {

		final StringBuilder result = new StringBuilder(replace.length());
		String[] words = replace.split(" ");
		for(int i=0,l=words.length;i<l;++i) {
		  if(i>0) result.append(" ");      
		  result.append(words[i].substring(0, 1).toUpperCase() + words[i].substring(1));

		}
		return result.toString();
	}
}

