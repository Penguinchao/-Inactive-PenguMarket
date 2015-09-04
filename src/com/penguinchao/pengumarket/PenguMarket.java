package com.penguinchao.pengumarket;


import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PenguMarket extends JavaPlugin implements Listener {
	public void onEnable(){
		getLogger().info("Initializing PenguMarket");
		saveDefaultConfig();
		debugOut("Debug Enabled");
		debugOut("Register Events - Beginning");
		getServer().getPluginManager().registerEvents(this, this);
		debugOut("Register Events - Finishing");
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		return false;
	}

	//My Functions
	public void debugOut(String message){
		if (getConfig().getString("debugenabled") == "true"  ) {
			getLogger().info("[Debug] " + message);
		}
	}
	public Boolean usingShop(Player player){
		//temporary null code
		return true;
	}
	public Boolean isItem(String itemname){
		//May not use -- may just check item stack of player
		//temporary null code
		//Will tie in to Vault
		return true;
	}
	public Boolean isCorrectPricing(String priceline){
		//temporary null code
		//Will check format of line
		return true;
	}
	public void establishShop(Player owner, Player establisher, Integer x, Integer y, Integer z, String world, ItemStack item){
		//temporary null code
		//Will place the shop in the database
	}
	public Boolean isMakingShop(Player player){
		//tests to see if the player is currently creating another shop
		return true;
	}
	//My Event Handlers
	@EventHandler
	public void onSignHit (BlockDamageEvent event){
		debugOut("BlockDamageEvent");
		//getLogger().info("MEEP");
	}
	@EventHandler
	public void onSignCreate (SignChangeEvent event){
		debugOut("SignChangeEvent");
		//Player player = event.getPlayer();
		if (event.getLine(0).equals("[Shop]")){
		//REVISIT THIS CODE -- IT ASSUMES THAT ITEMS CAN BE TYPED OUT, INSTEAD OF THINGS LIKE ENCHANTMENTS
		//
		//	if(isItem(event.getLine(1) )){
		//		debugOut(event.getLine(1) + " is an actual item");
		//FIXED -- Does not check for item upon placement, but left code just in case
			debugOut("Shop Sign Placed");
			if(!isMakingShop(event.getPlayer() )){
				if(isCorrectPricing(event.getLine(2) )){
					debugOut(event.getLine(2) + " is the correct shop sign syntax");

					if (event.getPlayer().hasPermission("pengumarket.admin.forcemakeshop")){
						debugOut(event.getPlayer().getName() + " has permission to force make shops");
						debugOut(event.getPlayer().getName() + " is making a shop for " + event.getLine(3));
					}else {
						debugOut(event.getPlayer().getName() + " doesn't have permission to force make shops");
						debugOut(event.getPlayer().getName() + " is making a shop for himself");
						event.setLine(3, event.getPlayer().getName());
					}
				}
				else{
					event.getBlock().breakNaturally();
				}
			
			}else{
				event.getBlock().breakNaturally();
			}
		}
	}
	@EventHandler
	public void onChat (AsyncPlayerChatEvent event){ //Used to change sign values
		debugOut("[AsyncPlayerChatEvent] " + event.getPlayer().getDisplayName() + ": " + event.getMessage());
		if (NumberUtils.isNumber(event.getMessage())){
			debugOut("[AsyncPlayerChatEvent] " + event.getMessage() + " is a number");
			if(usingShop(event.getPlayer() )== true){
				debugOut("[AsyncPlayerChatEvent] " + event.getPlayer().getDisplayName() + " is using a shop.");
			}else {
				debugOut("[AsyncPlayerChatEvent] " + event.getPlayer().getDisplayName() + " is not using a shop.");
			}
		} else {
			debugOut("[AsyncPlayerChatEvent] NOT Number");
		}
	}
}