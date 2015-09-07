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

import net.milkbowl.vault.economy.Economy;

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
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;


public class PenguMarket extends JavaPlugin implements Listener {
	//Global Variables
	public Boolean databaseConnected = true;
	private static Economy eco = null; //Eco credit: https://youtu.be/qNZmhhmU4VI?list=PLZLJ30gbD1Y6uZBef_4YB2JWLjvffLnQY
	HashMap<String, Integer> ActivePlayerShop = new HashMap<String, Integer>();
	HashMap<String, String> PlayerMakingShop = new HashMap<String, String>();
	HashMap<String, Integer> PlayerDestroyingShop = new HashMap<String, Integer>();
	Connection connection;
	
	//Basic Bukkit sections
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
		debugOut("Opened SQL Connection");
		if(!setupEconomy()){
			Bukkit.getPluginManager().disablePlugin(this);
		}
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

	//Vault Functions
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        eco = rsp.getProvider();
        return eco != null;
    }

	//Plugin Functions
	public void debugOut(String message){ //Outputs message to console if debug is enabled
		if (getConfig().getString("debugenabled") == "true"  ) {
			getLogger().info("[Debug] " + message);
		}
	}
	public void playerError(Player player, String message){ //Sends red text message to player
		player.sendMessage(ChatColor.RED + message);
	}
	public void playerSuccess(Player player, String message){ //Sends green text message to player
		player.sendMessage(ChatColor.GREEN + message);
	}
	public Boolean isMakingShop(Player player){ //tests to see if the player is currently creating another shop
		String name = player.getName();
		if(PlayerMakingShop.containsKey(player.getName())){
			String hashValue = PlayerMakingShop.get(name);
			if(hashValue != "false"){
				return true;
			}
		} 
		return false;
	}
	public void setMakingShop(Player owner, Player establisher, Integer x, Integer y, Integer z, World world){ //Adds player to the hashMap PlayerMakingShop to mark them as making a shop
		establisher.sendMessage(ChatColor.GREEN + getConfig().getString("ask-for-item"));
		UUID ownerID = owner.getUniqueId();
		UUID establisherID = establisher.getUniqueId();
		String blockLocation = x + "," + y + "," + z + "," + world.getName()+","+ownerID.toString()+","+establisherID.toString();
		debugOut("Marking " + establisher.getName()+" as creating a shop at "+blockLocation);
		PlayerMakingShop.put(establisher.getName(), blockLocation);
	}
	public Boolean isCorrectPricing(String priceline){ //tests to see if the buy line of a sign has the correct syntax
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
	public void establishShop(String owner, String establisher, String establisherName, Integer x, Integer y, Integer z, World world, ItemStack item, Float buy, Float sell){ //Adds the shop to the database and unmarks the player as making a shop
		String displayName = item.getItemMeta().getDisplayName();
		String material = item.getType().toString();
		String enchantments = enchantmentsToString(item);
		String data = item.getData().toString();
		//Insert in to database
		String query = "INSERT INTO shops (shopowner, shopestablisher, x, y, z, world, itemname, itemmaterial, itemenchantments, buy, sell, data) VALUES ('"+
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
				sell+"', '"+
				data+"' "+
				");";
		debugOut("Performing Query:");
		debugOut(query);
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			sql.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		PlayerMakingShop.remove(establisherName);
		debugOut(establisherName+" is finished making a shop");
		
	}
	public String enchantmentsToString(ItemStack item){ //Returns enchantments in the form of a string: EnchantmentOne,levelOne;EnchantmentTwo,levelTwo;
		Map<Enchantment, Integer> a = null;
		
		if(item.getData().toString().contains("ENCHANTED_BOOK")){
			debugOut("Pulling enchantment information from enchanted book");
			//credit for code http://www.massapi.com/class/org/bukkit/inventory/meta/EnchantmentStorageMeta.html
			EnchantmentStorageMeta esm = (EnchantmentStorageMeta) item.getItemMeta();
            a = esm.getStoredEnchants();
		}else{
			debugOut("Pulling enchantment information from enchanted non-book item");
			a= item.getEnchantments();
		}
		String returnMe = "";
		for(Entry<Enchantment, Integer> entry : a.entrySet()) {
		    returnMe = returnMe+entry.getKey().getName()+","+entry.getValue().toString()+";";
		    debugOut("Enchantments: "+returnMe);
		}
		return returnMe;
	}
	public Boolean compareStringEnchantments(ItemStack playerItem, String shopItem){ //Returns true if the enchantments given are the same; Returns false if there is any difference
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
	public Boolean findMatchingString(String findMe, String[] list){ //Goes through the entire list of strings to see if any entry is the same as the given string (findMe)
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
	public double getVaultBalance(UUID playerUUID){ //Returns the player's vault balance in the form of a double; I have no idea why they use memory-hogging doubles, but it's vault's method of storage, not mine
		debugOut("getting player from UUID");
		org.bukkit.OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID );
		
		debugOut("getting vault balance for player with UUID: "+playerUUID.toString() );
		debugOut("player name is: "+player.getName() );
		double returnMe = eco.getBalance(player);
		
		debugOut("balance is: "+returnMe);
		return returnMe;
	}
	public void changeVaultBalance(UUID playerUUID, float amount){ //Changes the vault balance of the specified player by the given amount; Negative numbers ARE allowed
		debugOut("Changing vault balance...");
		double amountDouble = Double.valueOf(amount);
		double absoluteValueDouble = Math.abs(amountDouble);
		org.bukkit.OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID );
		if(amountDouble == 0){
			//Do nothing
			debugOut("Value is zero -- skipping");
		}else if(amountDouble > 0){
			//Number is positive -- Deposit
			debugOut("Depositing to seller: "+absoluteValueDouble);
			eco.depositPlayer(player, absoluteValueDouble);
		}else {
			//Number is negative -- Withdraw
			debugOut("Withdrawing from buyer: "+absoluteValueDouble);
			eco.withdrawPlayer(player, absoluteValueDouble);
		}
		debugOut("Done!");
	}
	public float getShopBuyPrice(Integer shopID){ //Returns the cost to buy a single item from the specified shop
		//Returns the price that it costs the customer to buy one item
		String query = "SELECT `buy` FROM `shops` WHERE `shop_id`="+shopID+";";
		debugOut("Executing query: "+ query);
		String buyPrice = "0";
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet result = sql.executeQuery();
			debugOut("Query Completed");
			result.next();
			buyPrice= result.getString("buy");
		}catch (SQLException e) {
			debugOut("SQL Problem -- could not find shop's buy price");
			//e.printStackTrace();
		}
		debugOut("getShopBuyPrice returned a value of "+buyPrice+" for the shopID "+shopID);
		float buyPriceFloat = Float.parseFloat(buyPrice);
		return buyPriceFloat;
	}
	public float getShopSellPrice(Integer shopID){ //Returns the asking price to sell a single item to the specified shop
		//Returns the price that the shop owner will pay the consumer for one item
		String query = "SELECT `sell` FROM `shops` WHERE `shop_id`="+shopID+";";
		debugOut("Executing query: "+ query);
		String sellPrice = "0";
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet result = sql.executeQuery();
			debugOut("Query Completed");
			result.next();
			sellPrice= result.getString("sell");
		}catch (SQLException e) {
			debugOut("SQL Problem -- could not find shop's sell price");
			//e.printStackTrace();
		}
		debugOut("getShopBuyPrice returned a value of "+sellPrice+" for the shopID "+shopID);
		float sellPriceFloat = Float.parseFloat(sellPrice);
		return sellPriceFloat;
	}
	public void softAddItem(ItemStack item, Player player){ //Adds an item to the player's inventory; if it cannot fit, it is dropped to the floor to prevent it from being lost
		//add item to a player's inventory, then drop the extra's on the ground
		//Credit: https://bukkit.org/threads/check-if-there-is-enough-space-in-inventory.134923/
		HashMap<Integer,ItemStack> excess = player.getInventory().addItem(item);
		for( Map.Entry<Integer, ItemStack> me : excess.entrySet() ){
			player.getWorld().dropItem(player.getLocation(), me.getValue() );
		}
	}
	public Boolean canAddItem(ItemStack item, Player player){ //Checks to see if there is room to add an item to a player's inventory
		//Credit: https://bukkit.org/threads/check-if-there-is-enough-space-in-inventory.134923/
		int freeSpace = 0;
		for(ItemStack i : player.getInventory() ){
			if(i == null){
				freeSpace += item.getType().getMaxStackSize();
			} else if (i.getType() == item.getType() ){
				freeSpace += (i.getType().getMaxStackSize() - i.getAmount());
			}
		}
		debugOut("Item has: "+item.getAmount()+" and freeSpace is: "+freeSpace);
		if(item.getAmount() > freeSpace){
			debugOut("There is not enough freeSpace in the inventory");
			return false;
		}else{
			debugOut("There is enough freeSpace in the inventory");
			return true;
		}
	}
	public ItemStack getShopItem(Integer shopID, Boolean fullStack, String seller){ //Returns the itemStack that is attempting to be purchased
		//Converts database strings for this item to an ItemStack	
		Material itemMaterial = null;
		String material = null;
		String enchantments = null;
		String name = "";
		String data = "";
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
			data = result.getString("data");
		}catch (SQLException e) {
			debugOut("SQL Problem");
			e.printStackTrace();
		}
		debugOut("Beginning to assign variables");
		debugOut("Data");
		String dataAsInt = getData(data);
		debugOut("Material");
		itemMaterial= Material.getMaterial(material);
		//get the amount after checking full stack
		int quantity = 0;
		if(fullStack){
			quantity= itemMaterial.getMaxStackSize();
		}else {
			quantity = 1;
		}
		debugOut("Creating New item stack");
		ItemStack shopItem = new ItemStack(itemMaterial, quantity, (short) Short.parseShort(dataAsInt) ) ;
		debugOut("Creating item meta");
		ItemMeta meta = null;
		EnchantmentStorageMeta bookMeta = null;
		Boolean isBook = null;
		//Check if is a book
		if(material.contains("ENCHANTED_BOOK")){
			//Do for book items
			debugOut("Item is an enchanted book -- making note of it");
			bookMeta = (EnchantmentStorageMeta) shopItem.getItemMeta();
			isBook = true;
		}else {
			//Do for non-book items
			debugOut("Item is not an enchanted book");
			meta = shopItem.getItemMeta();
			isBook = false;
		}
		debugOut("Item meta: Display Name");
		if(name.equalsIgnoreCase(null) || name.equalsIgnoreCase("") || name.equalsIgnoreCase("null") ){
			//Do Nothing'
			debugOut("No set item name -- Skipping");
		}else{
			debugOut("Setting item meta name to: "+name);
			if(isBook){
				bookMeta.setDisplayName(name);
			}else{
				meta.setDisplayName(name);
			}
		}
		if(material.contains("SWORD") || material.contains("SPADE") || material.contains("PICKAXE") || material.contains("AXE") ||
				material.contains("HELMET") || material.contains("CHESTPLATE") || material.contains("LEGGINGS") || material.contains("BOOTS") ||
				isBook){ 
			//Test the material of the item, to prevent generic blocks from having lore
			if(getConfig().getString("item-receipt")=="true"){
				debugOut("Setting receipt");
				if(isBook){
					debugOut("Assigning book lore");
					bookMeta.setLore(Arrays.asList(getConfig().getString("item-receipt-text"), seller));
				}else{
					debugOut("Assigning non-book lore");
					meta.setLore(Arrays.asList(getConfig().getString("item-receipt-text"), seller));
				}
			}
		}
		if(!isBook){
			debugOut("Assigning non-book meta to the itemstack");
			shopItem.setItemMeta(meta);
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
				if(!isBook){
					debugOut("Adding (un)safe enchantment to non-book: "+currentEnchantment.getName()+" at level "+currentLevel);
					shopItem.addUnsafeEnchantment(currentEnchantment, currentLevel);
				}else{
					debugOut("Adding stored enchantment: "+currentEnchantment.getName()+" at level "+ currentLevel);
					if(bookMeta.addStoredEnchant(currentEnchantment, currentLevel, true)){
						debugOut("Item meta was changed as a result of addStoredEnchant");
					}else{
						debugOut("Item meta not changed! -- this should not happen");
					}
				}
				debugOut("Enchantment added!");				
			}
			if(isBook){
				debugOut("Assigning book meta to the itemstack");
				shopItem.setItemMeta(bookMeta);
			}
		}else{
			debugOut("No enchantments -- Skipping");
		}
		debugOut("Returning the itemstack");
		return shopItem;
	}
	public String getShopOwner(Integer shopID){ //Returns the owner's UUID of the specified shop from the database
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
	public Integer getShopID(Integer x, Integer y, Integer z, World world){ //Return the shop ID through database queries
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
				//e.printStackTrace();
				//Commented out error, because this will be empty often
			}
			//getLogger().info("[SEVERE] Cannot get shop by ID");
			//Commented out, because someone may click on an unfinished sign; debug message instead
			debugOut("Sign ID cannot be found");
			return 0;
	}
	public Integer getStock(Integer shopID){ //Returns the stock of the shop through database queries
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
	public void setStock(Integer shopID, Integer stock){ //Changes the stock in the database for the specified shop
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
	public void showShopInfo(Integer shopID, Player player){ //Display's the shop's information to the specified player
		String query = "SELECT * FROM `shops` WHERE `shop_id`="+shopID+";";
		debugOut(query);
		try {
			PreparedStatement sql = connection.prepareStatement(query);
			ResultSet result = sql.executeQuery();
			debugOut("Query Done");
			result.next();
			debugOut("Getting material");
			String material= result.getString("itemmaterial");
			debugOut("getting enchantments");
			String enchantments= result.getString("itemenchantments");
			debugOut("getting stock");
			Integer stock= result.getInt("stock");
			debugOut("getting buy price");
			Integer buy= result.getInt("buy");
			debugOut("getting sell price");
			Integer sell= result.getInt("sell");
			debugOut("getting data");
			String data= getData(result.getString("data"));
			debugOut("Giving all info:");
			debugOut("Stock:"+stock+" Buy:"+buy+" Sell:"+sell+" Material:"+material+" Data:"+data+" Enchantments:"+enchantments);
			player.sendMessage(ChatColor.YELLOW+"Shop Information:");
			player.sendMessage(ChatColor.GREEN+"Item: "+ChatColor.BLUE+material);
			if(!data.equals( 0 ) ){
				debugOut("data not null");
				player.sendMessage(ChatColor.GREEN+"Data: "+ChatColor.BLUE+data);
			}
			if(!enchantments.equals("")){
				sayEnchantments(enchantments, player);
			}
			player.sendMessage(ChatColor.GREEN+"Buying Price: "+ChatColor.BLUE+buy);
			player.sendMessage(ChatColor.GREEN+"Selling Price: "+ChatColor.BLUE+sell);
			player.sendMessage(ChatColor.GREEN+"Current Stock: "+ChatColor.BLUE+stock);
			player.sendMessage(ChatColor.YELLOW+"Hit again to buy or activate to sell");
			
		} catch (SQLException e) {
			//e.printStackTrace();
		}
	}
	
	public String getData(String rawData){
		//Convert data like "RED WOOL(14)" to 14
		//Do something
		return "1";
	}	
	public void sayEnchantments(String enchantments, Player player){ //Display's the specified enchantments in a list format to the specified player; does nothing if list is empty
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
	public String cleanEnchantmentName(String name){ //Formats the enchantment name to be more user-friendly for display
		return getConfig().getString(name);
	}
	public int getInventoryItemCount(Player player, ItemStack shopItem){ //Returns how many of a specific item are in the player's inventory
		debugOut("Getting player Inventory and comparing with the shop item");
		ItemStack[] inventory = player.getInventory().getContents();
		int returnMe = 0;
		int invSlot = 1;
		for(ItemStack item:inventory){
			debugOut("Inventory slot: "+invSlot);
			invSlot++;
			if(areEqualItems(item, shopItem)){
				debugOut("Items match -- getting stack size");
				returnMe += item.getAmount();
			}else {
				debugOut("Not the same item");
			}
		}
		return returnMe;
	}
	public void removePlayerItem(Player player, ItemStack item, int amountToRemove){ //Removes the selected amount of the item from the players inventory
		int amountLeft = amountToRemove;
		debugOut("Searching for "+amountLeft+" items");
		ItemStack[] inventory = player.getInventory().getContents();
		int invSlot = 1;
		for(ItemStack currentItem:inventory){
			if(amountLeft > 0){
				debugOut("Inventory slot: "+invSlot);
				debugOut("Amount remaining:"+amountLeft);
				invSlot++;
				if(areEqualItems(currentItem, item)){
					debugOut("Items match -- getting stack size");
					debugOut("Stack size:"+currentItem.getAmount());
					int stackSize = currentItem.getAmount();
					if(stackSize > amountLeft){
						debugOut("There are more items in this stack than needed");
						currentItem.setAmount(stackSize-amountLeft);
						amountLeft = 0;
					}else {
						debugOut("This stack does not have enough to deposit the item -- deducting amount");
						player.getInventory().removeItem(currentItem);
						debugOut("removingItemAmount: "+currentItem.getAmount() );
						amountLeft -= currentItem.getAmount();
					}
					
				}else {
					debugOut("Not the same item");
				}
			}else {
				debugOut("Amount left is 0; breaking loop");
				break;
			}
		}
	}
	public Boolean areEqualItems(ItemStack playerItem, ItemStack shopItem){ //Checks to see if the two items are equivalent
		debugOut("Comparing two item stacks");
		if(playerItem == null || shopItem == null){
			debugOut("At least one item is null -- returning false");
			return false;
		}else {
			debugOut("Items are not null. Continuing...");
		}
		debugOut("Getting material: playerItem");
		Material playerItemMaterial = playerItem.getType();
		Material shopItemMaterial = shopItem.getType();
		if(playerItemMaterial.equals(shopItemMaterial) ){
			debugOut("Item types are the same");
			if(playerItem.getItemMeta().getDisplayName() == shopItem.getItemMeta().getDisplayName() ){
				debugOut("Display names are the same");
				String shopEnchants = enchantmentsToString(shopItem);
				if(compareStringEnchantments(playerItem, shopEnchants)){
					debugOut("Enchantments for the two items are the same -- Item matches");
					return true;
				}else {
					debugOut("Enchantments are not the same -- Item doesn't match");
					return false;
				}
			}else {
				debugOut("Display names are different");
			}
		}else {
			debugOut("Item types are different");
			return false;
		}
		return false;
	}
	public Boolean getDestroyingShop(String playerName, Integer shopID){ //Returns true if player is trying to destroy the specified shop
		if(PlayerDestroyingShop.containsKey(playerName)){
			debugOut("Player is destroying a shop. Checking which one");
			if(PlayerDestroyingShop.get(playerName).equals(shopID) ){
				debugOut("Player is breaking a shop that was previously broken");
				return true;
			}else {
				return false;
			}
		}else{
			return false;
		}
	}
	public void setDestroyingShop(String playerName, Integer shopID){ //Sets a player as destroying the specified shop
		PlayerDestroyingShop.put(playerName, shopID);
	}
	public void deleteShop(int shopID){
		if(shopID > 0){
			String query = "DELETE FROM shops WHERE shop_id="+shopID+";";
			debugOut("Performing Deletion Query:");
			debugOut(query);
			try {
				PreparedStatement sql = connection.prepareStatement(query);
				sql.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}else{
			debugOut("Shop value is not in database -- skipping");
		}
	}

	//Event Listeners
	//Event Handlers
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
					if(equipped.getType()!=Material.AIR && equipped.getType()!=Material.WRITTEN_BOOK){
						debugOut("Begin establishing Shop at xyz ("+shopX+","+shopY+","+shopZ+") in "+shopWorld);
						String ownerUUID = playerHashInfo[4];
						String establisherUUID = playerHashInfo[5];
						playerSuccess(event.getPlayer(), "Success!");
						String[] buyLine = sign.getLine(2).split(" ");
						Float buy = Float.valueOf(buyLine[1]);
						Float sell = Float.valueOf(buyLine[3]);
						establishShop(ownerUUID, establisherUUID, event.getPlayer().getName(),shopX, shopY, shopZ, event.getBlock().getWorld(), equipped, buy, sell);
					}else if(equipped.getType()==Material.AIR){
						debugOut(event.getPlayer().getName()+" hit the shop with Air, which cannot be sold");
						playerError(event.getPlayer(), getConfig().getString("cant-sell-air") );
					}else if(equipped.getType()==Material.WRITTEN_BOOK) {
						debugOut(event.getPlayer().getName()+" hit the shop with a written book, which cannot be sold");
						playerError(event.getPlayer(), getConfig().getString("cant-sell-book") );
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
						//BUY FROM THE SHOP OR WITHDRAW
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
									ItemStack currentItem = getShopItem(thisShopID, true, sign.getLine(3) );
									Integer stackSize = currentItem.getMaxStackSize();
									if(Integer.valueOf(currentStock)>=stackSize){
										//Withdraw a stack
										debugOut("There are enough items in stock");
										if(canAddItem(currentItem, event.getPlayer()) ){
											debugOut("The player can fit all of the items in inventory");
											event.getPlayer().getInventory().addItem(currentItem);
											setStock(thisShopID, currentStock-stackSize);
										}else{
											debugOut("The player cannot fit all items in inventory");
											if(getConfig().getString("buy-when-full").matches("true")){
												debugOut("buy-when-full is true -- dropping extra items on floor");
												//Drop extra items on floor
												softAddItem(currentItem, event.getPlayer());
												setStock(thisShopID, currentStock-stackSize);
											} else {
												debugOut("buy-when-full is false -- cancelling trade");
												//Do not sell
												playerError(event.getPlayer(), getConfig().getString("full-inventory-error"));
											}
										}
									}else {
										//Not enough in stock
										debugOut("There are not enough items in stock to withdraw a stack");
										playerError(event.getPlayer(), getConfig().getString("not-enough-items"));
									}
									
								}else {
									//withdraw one item
									debugOut("withdrawing one item -- attempt");
									if(Integer.valueOf(currentStock)>0){
										//Add one to player
										debugOut("In Stock");
										debugOut("Retrieving the item");
										ItemStack currentItem = getShopItem(thisShopID, false, sign.getLine(3) );
										debugOut("Item retrieved");
										if(canAddItem(currentItem, event.getPlayer()) ){
											debugOut("Player can fit item -- adding");
											softAddItem(currentItem, event.getPlayer());
											debugOut("Decreasing stock by one...");
											setStock(thisShopID, currentStock-1);
										} else {
											debugOut("Player cannot fit item");
											if(getConfig().getString("buy-when-full").matches("true")){
												debugOut("buy-when-full is true -- dropping extra items on floor");
												softAddItem(currentItem, event.getPlayer());
												debugOut("Decreasing stock by one...");
												setStock(thisShopID, currentStock-1);
											}else {
												debugOut("buy-when-full is false -- cancelling trade");
												playerError(event.getPlayer(), getConfig().getString("full-inventory-error"));
											}
										}
										debugOut("Transaction Completed");
									}else {
										debugOut("Out of Stock");
										playerError(event.getPlayer(), getConfig().getString("out-of-stock"));
									}
								}
							}else {
								debugOut("Shop is not owned");
								//Do consumer stuff
								debugOut("getShopBuyPrice");
								float buyPrice = getShopBuyPrice(thisShopID);
								debugOut("getVaultbalance");
								float customerBalance = (float) getVaultBalance(event.getPlayer().getUniqueId());
								debugOut("Creating 'currentItem'");
								ItemStack currentItem;
								if(event.getPlayer().isSneaking()){
									//buy a stack
									debugOut("attempting to buy a stack");
									currentItem = getShopItem(thisShopID, true, sign.getLine(3));
								}else {
									//buy one item
									debugOut("attempting to buy one item");
									currentItem = getShopItem(thisShopID, false, sign.getLine(3));
								}
								if(currentStock>=currentItem.getAmount()){
									debugOut("Item has sufficient stock");
									if(customerBalance < (buyPrice*currentItem.getAmount()) ){
										debugOut("Insufficient Funds -- player has: "+customerBalance+" | the shop needs: "+(buyPrice*currentItem.getAmount() ));
										playerError(event.getPlayer(), getConfig().getString("insufficient-funds"));
									} else {
										debugOut("Player has sufficient funds: "+customerBalance);
										UUID ownerUUID = UUID.fromString(getShopOwner(thisShopID));
										if(canAddItem(currentItem, event.getPlayer()) ){
											//Do transaction
											debugOut("Player can fit item -- adding");
											softAddItem(currentItem, event.getPlayer());
											debugOut("Decreasing stock");
											setStock(thisShopID, currentStock-currentItem.getAmount());
											debugOut("Changing funds...");
											changeVaultBalance(ownerUUID, buyPrice*currentItem.getAmount());
											changeVaultBalance(event.getPlayer().getUniqueId(), -buyPrice*currentItem.getAmount());
											event.getPlayer().sendMessage(ChatColor.GREEN + "You have bought "+ChatColor.YELLOW+currentItem.getAmount()+" "+ChatColor.GREEN+" item(s) for "+ChatColor.YELLOW+buyPrice*currentItem.getAmount());
											
										}else {
											debugOut("Player cannot fit item");
											if(getConfig().getString("buy-when-full").matches("true")){
												debugOut("buy-when-full is enabled -- continuing transaction");
												softAddItem(currentItem, event.getPlayer());
												debugOut("Decreasing stock");
												setStock(thisShopID, currentStock-currentItem.getAmount());
												debugOut("Changing funds...");
												changeVaultBalance(ownerUUID, buyPrice*currentItem.getAmount());
												changeVaultBalance(event.getPlayer().getUniqueId(), -buyPrice*currentItem.getAmount()) ;
												debugOut("Notifying Player...");
												event.getPlayer().sendMessage(ChatColor.GREEN + "You have bought "+ChatColor.YELLOW+currentItem.getAmount()+" "+ChatColor.GREEN+" item(s) for "+ChatColor.YELLOW+buyPrice*currentItem.getAmount());
												
											}else{
												debugOut("buy-when-full is not enabled -- cancelling transaction");
												playerError(event.getPlayer(), getConfig().getString("full-inventory-error"));
											}
										}
									}
								}else {
									debugOut("Not enough in stock");
									playerError(event.getPlayer(), getConfig().getString("not-enough-items"));
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
						//event.setLine(1, getConfig().getString("sign-placeholder"));
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
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onSignActivate (PlayerInteractEvent event){
		debugOut("PlayerInteractEvent");
		if(event.getClickedBlock().getType() == Material.SIGN_POST || event.getClickedBlock().getType() == Material.WALL_SIGN){
			debugOut("A "+event.getClickedBlock().getType().toString()+" was interacted with");
			if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
				debugOut("Sign was back-clicked");
				//Credit: https://bukkit.org/threads/how-to-convert-block-class-to-sign-class.102313/
				BlockState blockState = event.getClickedBlock().getState();
				Sign sign = (Sign) blockState;
				if(sign.getLine(0).equals(getConfig().getString("sign-header")) ){
					debugOut("A shop sign was clicked");
					if(!isMakingShop(event.getPlayer()) ){
						debugOut("Player is not making a shop");
						debugOut("Getting clicked Shop ID");
						Integer thisShopID = getShopID(event.getClickedBlock().getX(), event.getClickedBlock().getY(), event.getClickedBlock().getZ(), event.getClickedBlock().getWorld());
						debugOut("Player's current shop ID is: "+thisShopID);
						debugOut("Checking player's current shop for username: "+event.getPlayer().getName() );
						Integer currentPlayerShop = 0;
						if(ActivePlayerShop.containsKey(event.getPlayer().getName() )){
							currentPlayerShop = ActivePlayerShop.get(event.getPlayer().getName());
						}
						debugOut("currentPlayerShop ID: "+currentPlayerShop);
						if(Integer.valueOf(currentPlayerShop)==Integer.valueOf(thisShopID) && Integer.valueOf(currentPlayerShop)!=0 ){
							debugOut("The active shop was clicked -- gettingItem");
							ItemStack shopItem = getShopItem(thisShopID, false, ""); //Depositing items does not need lore or item value, since each part of the item is analyzed separately.
							debugOut("getting stack size");
							int stackSize = shopItem.getMaxStackSize();
							debugOut("Size is: "+stackSize+" | Getting sell Price");
							float sellPrice = getShopSellPrice(thisShopID);
							debugOut("Sell price is: "+sellPrice+" | Checking if crouched...");
							int putAmount = 0;
							if(event.getPlayer().isSneaking()){
								putAmount = stackSize;
								sellPrice = sellPrice*stackSize;
								debugOut("Player is crouching, changing sell price to the cost of a whole stack: "+sellPrice);
							}else {
								putAmount = 1;
								debugOut("Player is not crouching; only selling/putting one");
							}
							//Check if player is owner of the shop
							if(event.getPlayer().getUniqueId().toString().equals(getShopOwner(thisShopID)) ){
								debugOut("Player is shop owner");
								if(getInventoryItemCount(event.getPlayer(), shopItem) >= putAmount ){
									debugOut("Player can put this amount in the shop");
									removePlayerItem(event.getPlayer(), shopItem, putAmount);
									setStock(thisShopID, getStock(thisShopID)+putAmount);
									debugOut("Successfully put "+putAmount+" items in the shop from the owner's inventory");
									event.getPlayer().updateInventory();
								}else {
									debugOut("Player cannot put this amount in the shop");
									playerError(event.getPlayer(), getConfig().getString("need-more-items"));
								}
							}else {
								debugOut("Player is not shop owner");
								if(getInventoryItemCount(event.getPlayer(), shopItem) >= putAmount ){
									debugOut("Player has enough items. Checking shop owner funds...");
									UUID shopOwnerUUID = UUID.fromString(getShopOwner(thisShopID));
									double shopFunds = getVaultBalance(shopOwnerUUID);
									if(shopFunds < sellPrice){
										debugOut("Shop owner has insufficient funds");
										playerError(event.getPlayer(), getConfig().getString("shop-insufficient-funds"));
									}else {
										debugOut("Shop owner has sufficient funds. Testing player inventory...");
										if(getInventoryItemCount(event.getPlayer(), shopItem) >= putAmount ){
											debugOut("Player has enough of this item");
											debugOut("Checking player balance...");
											debugOut("Removing item(s) from player inventory");
											removePlayerItem(event.getPlayer(), shopItem, putAmount);
											debugOut("Adjusting stock of shop");
											setStock(thisShopID, getStock(thisShopID)+putAmount);
											debugOut("Successfully sold "+putAmount+" items to the shop from the owner's inventory");
											debugOut("Adjusting accounts...");
											debugOut("Decreasing shop owner's bank by "+sellPrice);
											changeVaultBalance(shopOwnerUUID, -sellPrice);
											debugOut("Increasing seller's bank by "+sellPrice);
											changeVaultBalance(event.getPlayer().getUniqueId(), sellPrice);
											debugOut("Notifying Player");
											event.getPlayer().sendMessage(ChatColor.GREEN + "You have sold "+ChatColor.YELLOW+putAmount+" "+ChatColor.GREEN+" item(s) for "+ChatColor.YELLOW+sellPrice);
											event.getPlayer().updateInventory();
										}else {
											debugOut("Player does not have enough of this item");
											playerError(event.getPlayer(), getConfig().getString("need-more-items"));
										}
									}
								}else{
									debugOut("Player cannot put this amount in the shop");
									playerError(event.getPlayer(), getConfig().getString("need-more-items"));
								}
							}
						}else {
							debugOut("Active shop was not clicked");
						}
					}else {
						debugOut("Player is making a shop -- doing nothing");
					}
				}else{
					debugOut("A shop sign was not clicked");
				}
			}
		}
	}
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onSignDestroy (BlockBreakEvent event){
		debugOut("BlockBreakEvent");
		if(event.getBlock().getType()==Material.SIGN_POST || event.getBlock().getType()==Material.WALL_SIGN){
			debugOut("A Sign was attempted to be destroyed");
			BlockState blockState = event.getBlock().getState();
			Sign sign = (Sign) blockState;
			if(sign.getLine(0).equals(getConfig().getString("sign-header")) ){
				debugOut("Attempting to destroy shop");
				Integer shopID = getShopID(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event.getBlock().getWorld() );
				if(getShopOwner(shopID).equals(event.getPlayer().getUniqueId().toString()) || event.getPlayer().hasPermission("pengumarket.admin.destroyothershop") ){
					debugOut("Player owns shop or is an admin");
					if(event.getPlayer().isSneaking()){
						if(getDestroyingShop(event.getPlayer().getName(), shopID) ){
							debugOut("Player is destroying this shop");
							int shopStock = getStock(shopID);
							ItemStack shopItem = getShopItem(shopID, true, event.getPlayer().getName() );
							int stackSize = shopItem.getMaxStackSize();
							if(stackSize >= shopStock){
								debugOut("Stock is less than a stack");
								shopItem.setAmount(shopStock);
								softAddItem(shopItem, event.getPlayer() );
								event.getPlayer().updateInventory();
							}else{
								while(shopStock>stackSize) {
									softAddItem(shopItem, event.getPlayer());
									debugOut("Giving one stack of "+stackSize);
									shopStock = shopStock - stackSize;
								}
								debugOut("Shop has finished giving stacks -- giving remainder now");
								shopItem.setAmount(shopStock);
								softAddItem(shopItem, event.getPlayer() );
								event.getPlayer().updateInventory();
								debugOut("Done giving items");
							}
							debugOut("Deleting entry from database");
							deleteShop(shopID);
							debugOut("Shop Destroyed!");
						}else{
							debugOut("Player is not destroying this shop -- changing...");
							setDestroyingShop(event.getPlayer().getName(), shopID);
							playerError(event.getPlayer(), getConfig().getString("break-shop-warning"));
							event.setCancelled(true);
						}
					}else{
						debugOut("Player not sneaking...Cancelling");
						playerError(event.getPlayer(), getConfig().getString("not-sneaking-warning"));
						event.setCancelled(true);
					}
				}else {
					debugOut("Player does not own shop and is not an admin");
					event.setCancelled(true);
				}
			}else{
				//Do Nothing -- not a shop
			}
		}else{
			//Do Nothing -- not a sign
		}
	}
	
	//Database Functions
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
				getLogger().info("[ERROR] Could not connect to the database -- disabling");
				databaseConnected = false;
				Bukkit.getPluginManager().disablePlugin(this);
			}
	}
	
}