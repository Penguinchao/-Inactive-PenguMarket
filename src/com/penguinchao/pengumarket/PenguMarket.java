package com.penguinchao.pengumarket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;


public class PenguMarket extends JavaPlugin implements Listener {
	public Boolean databaseConnected = true;
	//Basic Plugin sections
	@Override
	public void onEnable(){
		getLogger().info("Initializing PenguMarket");
		saveDefaultConfig();
		debugOut("Debug Enabled");
		debugOut("Register Events - Beginning");
		getServer().getPluginManager().registerEvents(this, this);
		debugOut("Register Events - Finishing");
		debugOut("Openning SQL Connection");
		databaseConnect();
	}
	public void onDisable(){
		//FINAL Close SQL connection
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
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

	HashMap<String, Integer> ActivePlayerShop = new HashMap<String, Integer>();
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
		//Correct Format: B 5 : 3 S
		//B for buy, number for buy price, colon, number for selling price, S for sell
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
	public void establishShop(String owner, String establisher, String establisherName, Integer x, Integer y, Integer z, World world, ItemStack item, Float buy, Float sell){
		String displayName = item.getItemMeta().getDisplayName();
		String material = item.getType().toString();
		String enchantments = enchantmentsToString(item);
		//Insert in to database
		String query = "INSERT INTO shops (shopowner, shopestablisher, x, y, z, world, itemname, itemmaterial, itemenchantments, buy, sell) VALUES ('"+
				owner+"', '"+
				establisher+"', '"+
				x+"', '"+
				y+"', '"+
				z+"', '"+
				world + "', '"+
				displayName+"', '"+
				material+"', '"+
				enchantments+"', '"+
				buy+"', '"+
				sell+"' "+
				");";
		debugOut("Performing Query:");
		debugOut(query);
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			sql.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		//temporary null code
		//Will place the shop in the database
		PlayerMakingShop.remove(establisherName);
		debugOut(establisherName+" is finished making a shop");
		
	}
	public String enchantmentsToString(ItemStack item){
		Map<Enchantment, Integer> a = item.getEnchantments();
		String returnMe = "";
		for(Entry<Enchantment, Integer> entry : a.entrySet()) {
		    returnMe = returnMe+entry.getKey().getName()+","+entry.getValue().toString()+";";
		    debugOut("Enchantments: "+returnMe);
		}
		return returnMe;
	}
	public Boolean compareStringEnchantments(ItemStack playerItem, String shopItem){
		String shopString = shopItem;
		String playerString = enchantmentsToString(playerItem);
		debugOut("Comparing Enchantments:");
		String[] shopBundled = shopString.split(";");
		String[] playerBundled = playerString.split(";");
		int shopLength=shopBundled.length;
		int playerLength=playerBundled.length;
		debugOut("shopString"+"["+shopLength+"]: "+shopString);
		debugOut("playerString"+"["+playerLength+"]: "+playerString);
		if(Integer.valueOf(shopLength)==0 && Integer.valueOf(playerLength)==0){
			return true;
		}else{
			if(Integer.valueOf(playerLength)==Integer.valueOf(shopLength)){
				for(int i=0; i<shopLength; i++){
					if(findMatchingString(shopBundled[i], playerBundled)==false){
						return false;
					}
				}
				return true;
			}
			return false;
		}
	}
	public Boolean findMatchingString(String findMe, String[] list){
		int length = list.length;
		for(int i=0; i<length; i++){
			if(findMe.equalsIgnoreCase(list[i])){
				debugOut(findMe+" is the same as "+list[i]);
				return true;
			}
			debugOut(findMe+" is not the same as "+list[i]);
		}
		debugOut("The enchantment '"+findMe+"' could not be found in this list");
		return false;
	}
	//My Event Handlers
	@EventHandler
	public void onSignHit (BlockDamageEvent event){
		debugOut("BlockDamageEvent");
		//getLogger().info("MEEP");
		if(event.getBlock().getType()==Material.SIGN_POST || event.getBlock().getType()==Material.WALL_SIGN){
			debugOut("A sign was done damage.");
			//Credit: https://bukkit.org/threads/how-to-convert-block-class-to-sign-class.102313/
			BlockState blockState = event.getBlock().getState();
			Sign sign = (Sign) blockState;
			if(isMakingShop(event.getPlayer())){
				//Do this if the player hits a shop while making one
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
						playerSuccess(event.getPlayer(), "Success!");
						String[] buyLine = sign.getLine(2).split(" ");
						Float buy = Float.valueOf(buyLine[1]);
						Float sell = Float.valueOf(buyLine[3]);
						establishShop(ownerUUID, establisherUUID, event.getPlayer().getName(),shopX, shopY, shopZ, event.getBlock().getWorld(), equipped, buy, sell);
					}else{
						debugOut(event.getPlayer().getName()+" hit the shop with Air, which cannot be sold");
						playerError(event.getPlayer(), getConfig().getString("cant-sell-air") );
					}
				}else {
					debugOut(event.getPlayer().getName()+" did not hit the shop the shop (s)he was creating");
					playerError(event.getPlayer(), getConfig().getString("click-wrong-shop"));
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
			} else if(sign.getLine(0).equals(getConfig().getString("sign-header"))){
				//Do this if the player hit a shop, when he wasn't making one.
				Integer thisShopID = getShopID(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event.getBlock().getWorld() );//GET FROM DATABASE
				debugOut(event.getPlayer()+" is not making a shop, but did click on a shop sign");
				if(ActivePlayerShop.containsKey(event.getPlayer().getName())){
					Integer currentValue = ActivePlayerShop.get(event.getPlayer().getName());
					if(Integer.valueOf(currentValue)==Integer.valueOf(thisShopID) && Integer.valueOf(currentValue)!=0 ){
						//BUY FROM THE SHOP OR DEPOSIT
						debugOut("Checking to see if the selected shop is owned");
						String shopOwner = getShopOwner(thisShopID);
						if(shopOwner!="null"){
							Integer currentStock = getStock(thisShopID);
							if(shopOwner.equals(event.getPlayer().getUniqueId().toString()) ){
								debugOut("Shop is owned");
								//Do shop owner stuff
								if(event.getPlayer().isSneaking()){
									//withdraw a stack
									debugOut("withdrawing one stack -- attempt");
									
								}else {
									//withdraw one item
									debugOut("withdrawing one item -- attempt");
									if(Integer.valueOf(currentStock)>0){
										//Add one to player
										debugOut("In Stock");
										debugOut("Retrieving the item");
										ItemStack currentItem = getShopItem(thisShopID, 1, sign.getLine(3) );
										debugOut("Item retrieved -- giving to player");
										event.getPlayer().getInventory().addItem(currentItem);
										debugOut("Decreasing stock by one...");
										setStock(thisShopID, currentStock-1);
										debugOut("Transaction Completed");
									}else {
										debugOut("Out of Stock");
										playerError(event.getPlayer(), getConfig().getString("out-of-stock"));
									}
								}
							}else {
								debugOut("Shop is not owned");
								//Do consumer stuff
								if(event.getPlayer().isSneaking()){
									//buy a stack
									debugOut("buying a stack");
								}else {
									//buy one item
									debugOut("buying one item");
									
								}
							}
						}else {
							debugOut("This shop does not exist; is it finished being created?");
						}
					} else {
						debugOut("A shop was activated, but it was not this one; changing to this shop");
						ActivePlayerShop.put(event.getPlayer().getName(), thisShopID);
						showShopInfo(thisShopID, event.getPlayer());
					}
				} else {
					debugOut("No shop activated, activating it");
					ActivePlayerShop.put(event.getPlayer().getName(), thisShopID);
					showShopInfo(thisShopID, event.getPlayer());
				}
			}
		}
	}
	public ItemStack getShopItem(Integer shopID, int quantity, String seller){
		//Converts database strings for this item to an ItemStack		
		Material itemMaterial = null;
		String material = null;
		String enchantments = null;
		String name = "";
		String query = "SELECT * FROM `shops` WHERE `shop_id`="+shopID+";";
		debugOut("Executing query: "+ query);
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet result = sql.executeQuery();
			debugOut("Query Completed");
			result.next();
			material= result.getString("itemmaterial");
			enchantments= result.getString("itemenchantments");
			name = result.getString("itemname");
		}catch (SQLException e) {
			debugOut("SQL Problem");
			e.printStackTrace();
		}
		debugOut("Beginning to assign variables");
		debugOut("Material");
		itemMaterial= Material.getMaterial(material);
		debugOut("New item stack");
		ItemStack shopItem = new ItemStack(itemMaterial, quantity);
		debugOut("Creating item meta");
		ItemMeta meta = shopItem.getItemMeta();
		debugOut("Item meta: Display Name");
		if(name.equalsIgnoreCase(null) || name.equalsIgnoreCase("") || name.equalsIgnoreCase("null") ){
			//Do Nothing'
			debugOut("No set item name -- Skipping");
		}else{
			debugOut("Setting item name to: "+name);
			meta.setDisplayName(name);
		}
		debugOut("Enchanting...");
		String[] brokenEnchantments = enchantments.split(";");
		if(!enchantments.equalsIgnoreCase("")){
			for(int i=0; i<brokenEnchantments.length; i++){
				debugOut("Splitting: "+brokenEnchantments[i]);
				String[] brokenValues = brokenEnchantments[i].split(",");
				debugOut("Split Version: "+brokenValues[0]+" and "+brokenValues[1]);
				String enchName = brokenValues[0];
				debugOut("Final enchantment name string: "+enchName);
				debugOut("Assigning enchantment variable");
				debugOut(enchName);
				Enchantment currentEnchantment = Enchantment.getByName(enchName);
				debugOut("Assigning enchantment level integer");
				int currentLevel = Integer.parseInt(brokenValues[1]);
				debugOut("Adding (un)safe enchantment: "+currentEnchantment.getName()+" at level "+currentLevel);
				shopItem.addUnsafeEnchantment(currentEnchantment, currentLevel);
				debugOut("Enchantment added!");
				
			}
		}else{
			debugOut("No enchantments -- Skipping");
		}
		if(getConfig().getString("item-receipt")=="true"){
			debugOut("Setting receipt");
			meta.setLore(Arrays.asList(getConfig().getString("item-receipt-text"), seller));
		}
		debugOut("Assigning meta to the itemstack");
		shopItem.setItemMeta(meta);
		debugOut("Returning the itemstack");
		return shopItem;
	}
	public String getShopOwner(Integer shopID){
		String query = "SELECT shopowner FROM `shops` WHERE `shop_id`="+shopID+";";
		debugOut("Trying query: "+query);
		PreparedStatement sql;
		try {
			sql = connection.prepareStatement(query);
			ResultSet result = sql.executeQuery();
			result.next();
			return result.getString("shopowner");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		debugOut("Shop not found");
		return "null";
	}
	@EventHandler
	public void onSignCreate (SignChangeEvent event){
		debugOut("SignChangeEvent");
		//Player player = event.getPlayer();
		if (event.getLine(0).equals(getConfig().getString("sign-header"))){
			debugOut("Shop Sign Placed");
			if(isMakingShop(event.getPlayer())==false){
				debugOut(event.getPlayer().getName()+" is not making a shop yet");
				if(isCorrectPricing(event.getLine(2)) ){
					debugOut(event.getLine(2) + " is the correct shop sign syntax");
					if (event.getPlayer().hasPermission("pengumarket.admin.forcemakeshop")){
						debugOut(event.getPlayer().getName() + " has permission to force make shops");
						debugOut(event.getPlayer().getName() + " is attempting to make a shop for " + event.getLine(3));
						//event.setLine(1, getConfig().getString("sign-placeholder"));
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
	public Integer getShopID(Integer x, Integer y, Integer z, World world){
		//Look up shop ID through database queries
		String query = "SELECT shop_id FROM `shops` WHERE `x`="+x+
				" AND `y`="+y+
				" AND `z`="+z+
				" AND `world`="+world.getName()+
				";";
		debugOut("Trying query:"+query);

			PreparedStatement sql;
			try {
				sql = connection.prepareStatement(query);
				ResultSet result = sql.executeQuery();
				result.next();
				debugOut("Shop ID is "+result.getInt("shop_id"));
				return result.getInt("shop_id");
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				//Commented out error, because this will be empty often
			}
			//getLogger().info("[SEVERE] Cannot get shop by ID");
			//Commented out, because someone may click on an unfinished sign; debug message instead
			debugOut("Sign ID cannot be found");
			return 0;
	}
	public Integer getStock(Integer shopID){
		String query = "SELECT `stock` FROM `shops` WHERE `shop_id`="+shopID+";";
		debugOut(query);
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet result = sql.executeQuery();
			result.next();
			
			Integer stock= result.getInt("stock");
			return stock;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		debugOut("getStock Failed -- returning value of 0");
		return 0;
	}
	public void setStock(Integer shopID, Integer stock){
		String query = "UPDATE `shops` SET stock=? WHERE shop_id=?;";
		debugOut(query);
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			sql.setInt(1, stock);
			sql.setInt(2, shopID);
			sql.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void showShopInfo(Integer shopID, Player player){
		String query = "SELECT * FROM `shops` WHERE `shop_id`="+shopID+";";
		debugOut(query);
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet result = sql.executeQuery();
			debugOut("Query Done");
			result.next();
			String material= result.getString("itemmaterial");
			String enchantments= result.getString("itemenchantments");
			Integer stock= result.getInt("stock");
			Integer buy= result.getInt("buy");
			Integer sell= result.getInt("sell");
			debugOut("Stock:"+stock+" Buy:"+buy+" Sell:"+sell+" Material:"+material+" Enchantments:"+enchantments);
			player.sendMessage(ChatColor.YELLOW+"Shop Information:");
			player.sendMessage(ChatColor.GREEN+"Item: "+ChatColor.BLUE+material);
			if(!enchantments.equals("")){
				sayEnchantments(enchantments, player);
			}
			player.sendMessage(ChatColor.GREEN+"Buying Price: "+ChatColor.BLUE+buy);
			player.sendMessage(ChatColor.GREEN+"Selling Price: "+ChatColor.BLUE+sell);
			player.sendMessage(ChatColor.GREEN+"Current Stock: "+ChatColor.BLUE+stock);
			
			
		} catch (SQLException e) {
			//e.printStackTrace();
		}
	}
	public void sayEnchantments(String enchantments, Player player){
		String[] baseSplit = enchantments.split(";");
		if(Integer.valueOf(baseSplit.length)>0){
			if(baseSplit[0]!=""){
				player.sendMessage(ChatColor.GREEN+"Enchantments:");
				for(int i=0; i<baseSplit.length;i++){
					String[] bigSplit= baseSplit[i].split(",");
					player.sendMessage(ChatColor.GREEN+"-"+ChatColor.BLUE+cleanEnchantmentName(bigSplit[0])+" "+bigSplit[1]);
				}
			}
		}
	}
	public String cleanEnchantmentName(String name){
		//Will make the enchantment names clean -- do later
		return name;
	}
	//Will try a more refined SQL method
	/*
	public void executeQuery(String query){
		Connection connection;
		String mysqlHostName= getConfig().getString("mysqlHostName");
		String mysqlPort	= getConfig().getString("mysqlPort");
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
	*/
	Connection connection;
	public void databaseConnect(){

			String mysqlHostName= getConfig().getString("mysqlHostName");
			String mysqlPort	= getConfig().getString("mysqlPort");
			String mysqlUsername= getConfig().getString("mysqlUsername");
			String mysqlPassword= getConfig().getString("mysqlPassword");
			String mysqlDatabase= getConfig().getString("mysqlDatabase");
			String dburl = "jdbc:mysql://" + mysqlHostName + ":" + mysqlPort + "/" + mysqlDatabase;
			debugOut("Attempting to connect to the database "+mysqlDatabase+" at "+mysqlHostName);
			try{
				connection = DriverManager.getConnection(dburl, mysqlUsername, mysqlPassword);
			}catch(Exception exception){
				getLogger().info("[SEVERE] Could not connect to the database");
				databaseConnected = false;
			}
	}
	
}