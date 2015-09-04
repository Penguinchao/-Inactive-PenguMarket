package com.penguinchao.pengumarket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
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
	public void playerError(Player player, String message){
		player.sendMessage(ChatColor.RED + message);
	}
	public void playerSuccess(Player player, String message){
		player.sendMessage(ChatColor.GREEN + message);
	}

	HashMap<String, String> PlayerMakingShop = new HashMap<String, String>();
	public Boolean isMakingShop(Player player){
		//tests to see if the player is currently creating another shop
		String name = player.getName();
		if(PlayerMakingShop.containsKey(player.getName())){
			String hashValue = PlayerMakingShop.get(name);
			if(hashValue != "false"){
				return true;
			}
		} 
		return false;
	}
	public void setMakingShop(Player owner, Player establisher, Integer x, Integer y, Integer z, World world){
		establisher.sendMessage(ChatColor.GREEN + getConfig().getString("ask-for-item"));
		UUID ownerID = owner.getUniqueId();
		UUID establisherID = establisher.getUniqueId();
		String blockLocation = x + "," + y + "," + z + "," + world.getName()+","+ownerID.toString()+","+establisherID.toString();
		debugOut("Marking " + establisher.getName()+" as creating a shop at "+blockLocation);
		PlayerMakingShop.put(establisher.getName(), blockLocation);
	}
	public Boolean usingShop(Player player){
		//temporary null code
		return false;
	}
	public Boolean isCorrectPricing(String priceline){
		//Will check format of line
		//Correct Format: B 5 : 3 S

		String[] brokenPrice = priceline.split(" ");
		if(brokenPrice == null){
			debugOut("Price line was empty!");
			return false;
		}else if(brokenPrice.length != 5) {
			debugOut("Price line did not have the correct amount of elements (5)");
			return false;
		}else{
			debugOut("brokenPrice = " + brokenPrice[0]+brokenPrice[1]+brokenPrice[2]+brokenPrice[3]+brokenPrice[4]);
			if(brokenPrice[0].equals("B") ){
				if(NumberUtils.isNumber(brokenPrice[1])){
					if(brokenPrice[2].equals(":") ){
						if(NumberUtils.isNumber(brokenPrice[3])){
							if(brokenPrice[4].equals("S") ){
								return true;
							} else {
								debugOut(brokenPrice[4]+ " should read 'S'");
							}
						} else {
							debugOut(brokenPrice[3] + " is not a number");
						}
					} else {
						debugOut(brokenPrice[2]+" is not a colon");
					}
				} else {
					debugOut(brokenPrice[1] + " is not a number");
				}
			} else {
				debugOut(brokenPrice[0]+" should read 'B'");
			}
			return false;
		}
	}
	public void establishShop(String owner, String establisher, String establisherName, Integer x, Integer y, Integer z, World world, ItemStack item){
		//temporary null code
		//Will place the shop in the database
		PlayerMakingShop.remove(establisherName);
		debugOut(establisherName+"is finished making a shop");
	}
	//My Event Handlers
	@EventHandler
	public void onSignHit (BlockDamageEvent event){
		debugOut("BlockDamageEvent");
		//getLogger().info("MEEP");
		if(event.getBlock().getType()==Material.SIGN_POST || event.getBlock().getType()==Material.WALL_SIGN){
			debugOut("A sign was done damage.");
			if(isMakingShop(event.getPlayer())){
				debugOut(event.getPlayer()+" is making a shop");
				String[] playerHashInfo = PlayerMakingShop.get(event.getPlayer().getName() ).split(",");
				Integer shopX = event.getBlock().getX();
				Integer shopY = event.getBlock().getY();
				Integer shopZ = event.getBlock().getZ();
				Integer playerX = Integer.valueOf(playerHashInfo[0]);
				Integer playerY = Integer.valueOf(playerHashInfo[1]);
				Integer playerZ = Integer.valueOf(playerHashInfo[2]);
				String shopWorld = event.getBlock().getWorld().getName();
				String playerWorld = playerHashInfo[3];
				debugOut("Shop: "+shopX+shopY+shopZ+shopWorld);
				debugOut("Play: "+playerX+playerY+playerZ+playerWorld);
				if(shopX.intValue()==playerX.intValue() && shopY.intValue()==playerY.intValue() && shopZ.intValue() == playerZ.intValue() && shopWorld.equals(playerWorld)){
					debugOut(event.getPlayer().getName()+" hit the shop that (s)he was creating");
					ItemStack equipped = event.getPlayer().getItemInHand();
					if(equipped.getType()!=Material.AIR){
						debugOut("Begin establishing Shop at xyz ("+shopX+","+shopY+","+shopZ+") in "+shopWorld);
						String ownerUUID = playerHashInfo[4];
						String establisherUUID = playerHashInfo[5];
						establishShop(ownerUUID, establisherUUID, event.getPlayer().getName(),shopX, shopY, shopZ, event.getBlock().getWorld(), equipped);
					}else{
						debugOut(event.getPlayer().getName()+" hit the shop with Air, which cannot be sold");
						playerError(event.getPlayer(), getConfig().getString("cant-sell-air") );
					}
				}else {
					debugOut(event.getPlayer().getName()+" did not hit the shop the shop (s)he was creating");
					if(shopX.intValue()==playerX.intValue()){
						debugOut("X matches");
					}else{
						debugOut(playerX + " does not equal " + shopX);
					}
					if(shopY.intValue()==playerY.intValue()){
						debugOut("Y matches");
					}else{
						debugOut(playerY + " does not equal " + shopY);
					}
					if(shopZ.intValue() == playerZ.intValue()){
						debugOut("Z matches");
					}else{
						debugOut(playerZ + " does not equal " + shopZ);
					}
					if(shopWorld.equals(playerWorld)){
						debugOut("Worlds match");
					}else{
						debugOut(playerWorld + " does not equal " + shopWorld);
					}
				}
			}
		}
	}
	@EventHandler
	public void onSignCreate (SignChangeEvent event){
		debugOut("SignChangeEvent");
		//Player player = event.getPlayer();
		if (event.getLine(0).equals("[Shop]")){
			debugOut("Shop Sign Placed");
			if(isMakingShop(event.getPlayer())==false){
				debugOut(event.getPlayer().getName()+" is not making a shop yet");
				if(isCorrectPricing(event.getLine(2)) ){
					debugOut(event.getLine(2) + " is the correct shop sign syntax");
					if (event.getPlayer().hasPermission("pengumarket.admin.forcemakeshop")){
						debugOut(event.getPlayer().getName() + " has permission to force make shops");
						debugOut(event.getPlayer().getName() + " is attempting to make a shop for " + event.getLine(3));
						event.setLine(1, getConfig().getString("sign-placeholder"));
						if(Bukkit.getServer().getPlayer(event.getLine(3)) != null && !event.getLine(3).equals("") ){
							if(Bukkit.getServer().getPlayer(event.getLine(3)) != event.getPlayer()){
								getLogger().info("[Internal Controls] " + event.getPlayer().getName() + "is attempting to create a shop for " + Bukkit.getServer().getPlayer(event.getLine(3)).getName() + " at xyz (" + event.getBlock().getX() + ", " + event.getBlock().getY() + ", " + event.getBlock().getZ() + ") in world (" + event.getBlock().getWorld() + ")" );
							}
							setMakingShop(Bukkit.getServer().getPlayer(event.getLine(3)), event.getPlayer(), event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event.getBlock().getWorld());
						} else {
							event.getPlayer().sendMessage(ChatColor.RED + getConfig().getString("player-not-online"));
							debugOut(event.getLine(3) + " is not online -- cancelling");
							event.getBlock().breakNaturally();
						}
					}else {
						debugOut(event.getPlayer().getName() + " doesn't have permission to force make shops");
						debugOut(event.getPlayer().getName() + " is making a shop for himself");
						event.setLine(3, event.getPlayer().getName());
						event.setLine(1, getConfig().getString("sign-placeholder"));
						setMakingShop(event.getPlayer(), event.getPlayer(), event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event.getBlock().getWorld());
					}
				}
				else{
					debugOut("Incorrect pricing syntax");
					event.getPlayer().sendMessage(ChatColor.RED + getConfig().getString("wrong-pricing-syntax"));
					event.getBlock().breakNaturally();
				}
			
			} else{
				debugOut(event.getPlayer().getName() + " attempted to create two shops at once. Cancelling the second one.");
				event.getPlayer().sendMessage(ChatColor.RED + getConfig().getString("two-shops-at-once"));
				event.getBlock().breakNaturally();
			}
		}
	}
	/*//Deleting this code probably, because sign values are created when the shop is
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
	*/
	public void executeQuery(String query){
		Connection connection;
		String mysqlHostName= getConfig().getString("mysqlHostName");
		String mysqlPort= getConfig().getString("mysqlPort");
		String mysqlUsername= getConfig().getString("mysqlUsername");
		String mysqlPassword= getConfig().getString("mysqlPassword");
		String mysqlDatabase= getConfig().getString("mysqlDatabase");
		String dburl = "jdbc:mysql://" + mysqlHostName + ":" + mysqlPort + "/" + mysqlDatabase;
		debugOut("Attempting to connect to the database "+mysqlDatabase+" at "+mysqlHostName);
		try{
			debugOut("Connection success!");
			connection = DriverManager.getConnection(dburl, mysqlUsername, mysqlPassword);
			PreparedStatement statement = connection.prepareStatement(query);
			statement.executeQuery();
		}catch(Exception exception){
			getLogger().info("[SEVERE] Could not connect to the database");
		}
	}
}