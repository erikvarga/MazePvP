package mazepvp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class MazePvP extends JavaPlugin {
	
	public static final String MAIN_FILE_NAME = "maze-names.txt";
	public static final int BOSS_TIMER_MAX = 100;
	public static final int BOSS_HEALTH_BARS = 6;
	
	public static MazePvP theMazePvP;

	public MazeTick tickTask = null;
	public ArrayList<Maze> mazes = new ArrayList<Maze>();
	public int wallChangeTimer = 0;
	public int mazeBossRestoreTimer = 0;
	public boolean showHeads = true;
	public boolean replaceBoss = true;
	public int fightStartDelay = 5*20;
	public List<String> joinSignText;
	public List<String> leaveSignText;
	public List<String> joinMazeText;
	public List<String> leaveMazeText;
	public List<String> fightAlreadyStartedText;
	public List<String> mazeFullText;
	public List<String> joinedOtherText;
	public List<String> countdownText;
	public List<String> fightStartedText;
	public List<String> waitBroadcastText;
	public List<String> waitBroadcastFullText;
	public String startedStateText;
	public String waitingStateText;
	public List<String> fightRespawnText;
	public List<String> lastRespawnText;
	public List<String> playerOutText;
	public List<String> winText;
	public List<String> fightStoppedText;
	public MazeConfig rootConfig;
	
	public MazePvP() {
	}
	
	public void onEnable(){
		theMazePvP = this;
		tickTask = new MazeTick(this);
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, tickTask, 1, 1);
		getServer().getPluginManager().registerEvents(new EventListeners(), this);
		getCommand("mpcreate").setExecutor(new CommandCreateMaze(this));
		getCommand("mplist").setExecutor(new CommandListMazes(this));
		getCommand("mpdelete").setExecutor(new CommandDeleteMaze(this));
		getCommand("mpclear").setExecutor(new CommandClearMaze(this));
		getCommand("mpsetwp").setExecutor(new CommandSetWaitingPlace(this));
		getCommand("mpremovewp").setExecutor(new CommandRemoveWaitingPlace(this));
		getCommand("mpsetplayernum").setExecutor(new CommandSetPlayerNum(this));
		getCommand("mpjoinsign").setExecutor(new CommandCreateJoinSign(this));
		getCommand("mpleavesign").setExecutor(new CommandCreateLeaveSign(this));
		getCommand("mpstopfight").setExecutor(new CommandStopFight(this));
		getCommand("mpset").setExecutor(new CommandSetMazeProp(this));
		getCommand("mpget").setExecutor(new CommandGetMazeProp(this));
		getCommand("mpadditem").setExecutor(new CommandAddItem(this));
		getCommand("mpremoveitem").setExecutor(new CommandRemoveItem(this));
		saveDefaultConfig();
		loadConfiguration();
    	MazePvP.writeConfigToYml(rootConfig, getConfig());
    	saveConfig();
		Iterator<World> wit = Bukkit.getServer().getWorlds().iterator();
		while (wit.hasNext()) {
			loadMazeProps(wit.next());
		}
	 }
	 
	public void onDisable() {
		Bukkit.getServer().getScheduler().cancelTasks(this);
		Iterator<World> wit = Bukkit.getServer().getWorlds().iterator();
		while (wit.hasNext()) {
			saveMazeProps(wit.next());
		}
		 Iterator<Maze> it = mazes.iterator();
         while (it.hasNext()) {
         	Maze maze = it.next();
	        if (!maze.canBeEntered && !maze.playerInsideMaze.isEmpty()) {
		        maze.stopFight(false);
	        }
         }
	 }

	public void saveMazeProps(World world) {
        try
        {
        	File mazeNamesFile = new File(world.getWorldFolder(), MAIN_FILE_NAME);
            PrintWriter nameWriter = null;

            Iterator<Maze> it = mazes.iterator();
            while (it.hasNext()) {
            	Maze maze = it.next();
            	if (maze.mazeWorld != world) continue;
                try
                {
            	if (nameWriter == null) nameWriter = new PrintWriter(new FileWriter(mazeNamesFile, false));
            	nameWriter.printf("%s\n", new Object[]{maze.name});
            	File mazeFile = new File(world.getWorldFolder(), maze.name+".maze");
            	PrintWriter var1 = new PrintWriter(new FileWriter(mazeFile, false));
            	var1.printf("%d %d %d %d %f %d %d %d %d %d\n", new Object[] {maze.mazeX, maze.mazeY, maze.mazeZ, maze.mazeSize, maze.bosses.get(0).hp, maze.canBeEntered?1:0, maze.hasWaitArea?1:0, maze.waitX, maze.waitY, maze.waitZ});
                var1.printf("%s\n", new Object[]{(maze.bosses.get(0).id==null)?"":maze.bosses.get(0).toString()});
            	for (int i = 0; i < maze.mazeSize*2+1; i++) {
            		for (int j = 0; j < maze.mazeSize*2+1; j++) {
            			if (j == maze.mazeSize*2) var1.printf("%d\n", new Object[] {maze.maze[i][j]});
            			else var1.printf("%d ", new Object[] {maze.maze[i][j]});
            		}
            	}
            	List<int[]> signList;
            	for (int s = 0; s < 2; s++) {
            		if (s == 0) signList = maze.joinSigns;
            		else signList = maze.leaveSigns;
	            	var1.printf("%d\n", new Object[]{signList.size()});
	            	Iterator<int[]> jit = signList.iterator();
	            	while (jit.hasNext()) {
	            		int[] sign = jit.next();
	            		for (int i = 0; i < sign.length; i++) var1.printf((i+1 == sign.length) ? "%d\n":"%d ", new Object[]{sign[i]});
	            	}
            	}
                var1.close();
                YamlConfiguration mazeConfig = new YamlConfiguration();
                MazePvP.writeConfigToYml(maze.configProps, mazeConfig);
                mazeFile = new File(world.getWorldFolder(), maze.name+".yml");
                mazeConfig.save(mazeFile);
                } catch (Exception var4)
                {
                	getLogger().info("Failed to save properties of maze \""+maze.name+"\": "+var4.getMessage());
                }
            }

            if (nameWriter != null) nameWriter.close();
        } catch (Exception var4)
        {
        	getLogger().info("Failed to save maze properties: " + var4);
        }
    }
   
   @SuppressWarnings("deprecation")
   public void loadConfiguration() {
		Configuration config = getConfig();
		rootConfig = new MazeConfig(false);
		showHeads = config.getBoolean("showHeadsOnSpikes");
		replaceBoss = config.getBoolean("replaceMobsWithBoss");
		fightStartDelay = config.getInt("fightStartDelay")*20;
		MazePvP.loadConfigFromYml(rootConfig, getConfig(), getConfig(), false, true);
		rootConfig.minPlayers = config.getInt("playerNum.min");
		rootConfig.maxPlayers = config.getInt("playerNum.max");
		rootConfig.playerMaxDeaths = config.getInt("playerLives");
		rootConfig.bossName = config.getString("boss.name");
		rootConfig.bossMaxHp = config.getInt("boss.hp");
		rootConfig.bossStrength = config.getInt("boss.attack");
		rootConfig.groundReappearProb = config.getDouble("probabilities.groundReappear");
		rootConfig.chestAppearProb = config.getDouble("probabilities.chestAppear");
		rootConfig.enderChestAppearProb = config.getDouble("probabilities.enderChestAppear");
		rootConfig.spawnMobProb = config.getDouble("probabilities.mobAppear");
		
		int itemCount = config.getInt("chestItems.itemCount");
		
		ItemStack tempChestItems[] = new ItemStack[itemCount];
		double tempChestWeighs[] = new double[itemCount];
		int chestItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = config.getInt("chestItems.item"+(i+1)+".id");
			int amount = config.getInt("chestItems.item"+(i+1)+".amount");
			double weigh = config.getDouble("chestItems.item"+(i+1)+".weigh");
			if (id == 0) tempChestItems[i] = null;
			else {
				tempChestItems[i] = new ItemStack(id, amount);
				tempChestWeighs[i] = weigh;
				chestItemNum++;
			}
		}
		rootConfig.chestWeighs = new double[chestItemNum];
		rootConfig.chestItems = new ItemStack[chestItemNum];
		int place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempChestItems[i] != null) {
				rootConfig.chestItems[place] = tempChestItems[i];
				rootConfig.chestWeighs[place] = tempChestWeighs[i];
				place++;
			}
		}
		
		itemCount = config.getInt("boss.drops.itemCount");
		
		ItemStack tempBossItems[] = new ItemStack[itemCount];
		double tempBossWeighs[] = new double[itemCount];
		int bossItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = config.getInt("boss.drops.item"+(i+1)+".id");
			int amount = config.getInt("boss.drops.item"+(i+1)+".amount");
			double weigh = config.getDouble("boss.drops.item"+(i+1)+".weigh");
			if (id == 0) tempBossItems[i] = null;
			else {
				tempBossItems[i] = new ItemStack(id, amount);
				tempBossWeighs[i] = weigh;
				bossItemNum++;
			}
		}
		rootConfig.bossDropWeighs = new double[bossItemNum];
		rootConfig.bossDropItems = new ItemStack[bossItemNum];
		place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempBossItems[i] != null) {
				rootConfig.bossDropItems[place] = tempBossItems[i];
				rootConfig.bossDropWeighs[place] = tempBossWeighs[i];
				place++;
			}
		}
		
		itemCount = config.getInt("startItems.itemCount");
		ItemStack tempStartItems[] = new ItemStack[itemCount];
		int startItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = config.getInt("startItems.item"+(i+1)+".id");
			int amount = config.getInt("startItems.item"+(i+1)+".amount");
			if (id == 0) tempStartItems[i] = null;
			else {
				tempStartItems[i] = new ItemStack(id, amount);
				startItemNum++;
			}
		}
		rootConfig.startItems = new ItemStack[startItemNum];
		place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempStartItems[i] != null) {
				rootConfig.startItems[place] = tempStartItems[i];
				place++;
			}
		}
		
		joinSignText = config.getStringList("texts.joinSign");
		leaveSignText = config.getStringList("texts.leaveSign");
		joinMazeText = config.getStringList("texts.onJoin");
		leaveMazeText = config.getStringList("texts.onLeave");
		fightAlreadyStartedText = config.getStringList("texts.onJoinAfterFightStarted");
		mazeFullText = config.getStringList("texts.onJoinWhenMazeFull");
		joinedOtherText = config.getStringList("texts.onJoinWhenAlreadyJoinedOtherMaze");
		mazeFullText = config.getStringList("texts.onJoinWhenMazeFull");
		countdownText = config.getStringList("texts.countdown");
		fightStartedText = config.getStringList("texts.fightStarted");
		waitBroadcastText = config.getStringList("texts.joinBroadcast");
		waitBroadcastFullText = config.getStringList("texts.joinBroadcastWhenFull");
		fightRespawnText = config.getStringList("texts.fightRespawn");
		lastRespawnText = config.getStringList("texts.fightRespawnLastLife");
		playerOutText = config.getStringList("texts.fightPlayerOut");
		winText = config.getStringList("texts.fightWin");
		fightStoppedText = config.getStringList("texts.fightStopped");
		startedStateText = config.getString("texts.startedState");
		waitingStateText = config.getString("texts.waitingState");
   }

	public void loadMazeProps(World world) {
		try
        {
        	File mazeNamesFile = new File(world.getWorldFolder(), MAIN_FILE_NAME);
            if (!mazeNamesFile.exists())
            {
                return;
            }
            BufferedReader nameReader = new BufferedReader(new FileReader(mazeNamesFile));
            String var2 = "";
            ArrayList<String> mazeNames = new ArrayList<String>();
            while ((var2 = nameReader.readLine()) != null) {
            	if (var2.length() > 0) mazeNames.add(var2);
            }
            nameReader.close();
            
            Iterator<String> it = mazeNames.iterator();
            while (it.hasNext()) {
            	String str = it.next();
            	try
                {
            	File mazeFile = new File(world.getWorldFolder(), str+".maze");
            	if (!mazeFile.exists()) {
                	throw new Exception("Couldn't find maze file \""+str+".maze\" in world \""+world.getName()+"\"");
            	}
	            BufferedReader var1 = new BufferedReader(new FileReader(mazeFile));
	            String[] var3;
            	Maze maze = new Maze();
            	maze.name = str;
            	boolean pNumFromPrevPlace = false;
	            if ((var2 = var1.readLine()) != null) {
	            	var3 = var2.split("\\s");
	                if (var3.length < 4 || var3.length > 12) {
	                	var1.close();
	                	throw new Exception("Malformed input");
	                }
	                maze.mazeX = Integer.parseInt(var3[0]);
	                maze.mazeY = Integer.parseInt(var3[1]);
	                maze.mazeZ = Integer.parseInt(var3[2]);
	                maze.mazeSize = Integer.parseInt(var3[3]);
	                maze.bosses.get(0).hp = (var3.length >= 5) ? Double.parseDouble(var3[4]) : 0;
	                maze.canBeEntered = (var3.length >= 6) ? (Integer.parseInt(var3[5]) != 0) : true;
	        		if (var3.length >= 7) maze.hasWaitArea = Integer.parseInt(var3[6]) != 0;
	        		if (var3.length >= 8) maze.waitX = Integer.parseInt(var3[7]);
	        		if (var3.length >= 9) maze.waitY = Integer.parseInt(var3[8]);
	        		if (var3.length >= 10) maze.waitZ = Integer.parseInt(var3[9]);
                	if (var3.length >= 11) maze.configProps.minPlayers = Integer.parseInt(var3[10]);
                	if (var3.length >= 12) {
                		maze.configProps.maxPlayers = Integer.parseInt(var3[11]);
                		pNumFromPrevPlace = true;
                	}
	                if ((var2 = var1.readLine()) != null) {
	                	if (var2.equals("")) maze.bosses.get(0).id = null;
	                	else maze.bosses.get(0).id = UUID.fromString(var2);
	                } else {
	                	var1.close();
	                	throw new Exception("Malformed input");
	                }
	                maze.mazeWorld = world;
	                maze.maze = new int[maze.mazeSize*2+1][];
	                maze.isBeingChanged = new boolean[maze.mazeSize*2+1][];
	            	for (int i = 0; i < maze.mazeSize*2+1; i++) {
	            		if ((var2 = var1.readLine()) == null) {
                        	var1.close();
                        	throw new Exception("Malformed input");
	            		}
	                	var3 = var2.split("\\s");
	                    if (var3.length != maze.mazeSize*2+1) {
	                    	var1.close();
	                    	throw new Exception("Malformed input");
	                    }
	                    maze.maze[i] = new int[maze.mazeSize*2+1];
	                    maze.isBeingChanged[i] = new boolean[maze.mazeSize*2+1];
	            		for (int j = 0; j < var3.length; j++) {
	            			maze.maze[i][j] = Integer.parseInt(var3[j]);
	            			maze.isBeingChanged[i][j] = false;
	            		}
	            	}
	            	List<int[]> signList;
	            	for (int s = 0; s < 2; s++) {
	            		if (s == 0) signList = maze.joinSigns;
	            		else signList = maze.leaveSigns;
		            	if ((var2 = var1.readLine()) != null && var2.length() > 0) {
		                	int joinSignNum = Integer.parseInt(var2);
		                	for (int i = 0; i < joinSignNum; i++) {
		                		if ((var2 = var1.readLine()) != null) {
		    	                	var3 = var2.split("\\s");
		                			if (var3.length != 7) {
		                				var1.close();
				                    	throw new Exception("Malformed input");
		                			}
		                			int x1 = Integer.parseInt(var3[0]);
		                			int y1 = Integer.parseInt(var3[1]);
		                			int z1 = Integer.parseInt(var3[2]);
		                			int x2 = Integer.parseInt(var3[3]);
		                			int y2 = Integer.parseInt(var3[4]);
		                			int z2 = Integer.parseInt(var3[5]);
		                			int reversed = Integer.parseInt(var3[6]);
		                			signList.add(new int[]{x1, y1, z1, x2, y2, z2, reversed});
		                		} else {
		                			var1.close();
			                    	throw new Exception("Malformed input");
		                		}
		                	}
		                }
	            	}
	        		Collection<Zombie> entities = maze.mazeWorld.getEntitiesByClass(Zombie.class);
	    			Iterator<Zombie> iter = entities.iterator();
	    			while (iter.hasNext()) {
	    				Zombie en = iter.next();
	    				if (en.getUniqueId().equals(maze.bosses.get(0).id)) {
	    					maze.bosses.get(0).entity = en;
	    					break;
	    				}
	    			}
	            } else {
	            	var1.close();
	            	throw new Exception("Malformed input");
	            }
	            var1.close();
	            
	            YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), maze.name+".yml"));
	            MazePvP.loadConfigFromYml(maze.configProps, config, getConfig(), pNumFromPrevPlace, false);
	    		
	            maze.updateSigns();
	            mazes.add(maze);
	            } catch (Exception var4) {
	            	getLogger().info("Failed to load properties of maze \""+str+"\": "+var4.getMessage());
	            }
            }
        } catch (Exception var4)
        {
        	getLogger().info("Failed to load maze properties: "+var4.getMessage());
        	mazes = new ArrayList<Maze>();
        }
	}

	public Maze findMaze(String mazeName, World world) {
		Iterator<Maze> it = mazes.iterator();
		while (it.hasNext()) {
			Maze maze = it.next();
			if (maze.mazeWorld == world && maze.name.equals(mazeName)) return maze;
		}
		return null;
	}

	public void removeMazes(World world) {
		Iterator<Maze> it = mazes.iterator();
		while (it.hasNext()) {
			Maze maze = it.next();
			if (maze.mazeWorld == world) {
				it.remove();
			}
		}
	}

	public static BlockFace getRandomRotation() {
		int rand = (int)Math.floor(Math.random()*16);
		if (rand == 0) return BlockFace.EAST;
		if (rand == 1) return BlockFace.EAST_NORTH_EAST;
		if (rand == 2) return BlockFace.EAST_SOUTH_EAST;
		if (rand == 3) return BlockFace.NORTH;
		if (rand == 4) return BlockFace.NORTH_EAST;
		if (rand == 5) return BlockFace.NORTH_NORTH_EAST;
		if (rand == 6) return BlockFace.NORTH_NORTH_WEST;
		if (rand == 7) return BlockFace.NORTH_WEST;
		if (rand == 8) return BlockFace.SOUTH;
		if (rand == 9) return BlockFace.SOUTH_EAST;
		if (rand == 10) return BlockFace.SOUTH_SOUTH_EAST;
		if (rand == 11) return BlockFace.SOUTH_SOUTH_WEST;
		if (rand == 12) return BlockFace.SOUTH_WEST;
		if (rand == 13) return BlockFace.WEST;
		if (rand == 14) return BlockFace.WEST_NORTH_WEST;
		return BlockFace.WEST_SOUTH_WEST;
	}

	public static int getSafeY(int posX, int posY, int posZ, World world) {
		posY--;
		int yy, prevPosY = posY;
		for (yy = posY; yy <= 254; yy++) {
			if (world.getBlockAt(posX, yy, posZ).getType().isSolid() && !world.getBlockAt(posX, yy+1, posZ).getType().isSolid()) {
				posY = yy;
				break;
			}
		}
		for (yy = posY; yy >= 0; yy--) {
			if (world.getBlockAt(posX, yy, posZ).getType().isSolid() && !world.getBlockAt(posX, yy+1, posZ).getType().isSolid()) {
				if (posY == prevPosY || posY-prevPosY > prevPosY-yy) posY = yy;
				break;
			}
		}
		posY++;
		return posY;
	}

	public static void cleanUpPlayer(Player player, boolean keepEnderChest) {
		player.getInventory().clear();
		if (!player.isDead()) {
			player.setHealth(player.getMaxHealth());
			player.setFoodLevel(20);
		}
		while (!player.getActivePotionEffects().isEmpty())
			player.removePotionEffect(player.getActivePotionEffects().iterator().next().getType());
		player.getInventory().setHelmet(null);
		player.getInventory().setChestplate(null);
		player.getInventory().setLeggings(null);
		player.getInventory().setBoots(null);
		if (!keepEnderChest) player.getEnderChest().clear();
	}

	public static ItemStack[] cloneItems(ItemStack[] items) {
		if (items == null) return null;
		ItemStack[] cloneItems = new ItemStack[items.length];
		for (int i = 0; i < items.length; i++) {
			cloneItems[i] = (items[i]==null)?null:items[i].clone();
		}
		return cloneItems;
	}

	public static ItemStack[] getClonedArmor(EntityEquipment armor) {
		ItemStack[] items = armor.getArmorContents();
		ItemStack[] cloneArmor = new ItemStack[items.length];
		for (int i = 0; i < items.length; i++) {
			cloneArmor[i] = (items[i]==null)?null:items[i].clone();
		}
		return cloneArmor;
	}

	public static ItemStack[] cloneISArray(ItemStack[] array) {
		ItemStack[] clone = new ItemStack[array.length];
		for (int i = 0; i < array.length; i++) clone[i] = array[i].clone();
		return clone;
	}

	public static boolean propHasIntValue(String prop) {
		return (prop.equals("playerLives") || prop.equals("playerNum.min") || prop.equals("playerNum.max") || prop.equals("boss.hp")
			 || prop.equals("boss.attack"));
	}

	public static boolean propHasDoubleValue(String prop) {
		if (prop.equals("probabilities.groundReappear") || prop.equals("probabilities.chestAppear") || prop.equals("probabilities.enderChestAppear") || prop.equals("probabilities.mobAppear"))
			return true;
		return false;
	}

	public static boolean propHasStringValue(String prop) {
		return (prop.equals("boss.name"));
	}

	public static boolean propHasItemValue(String prop) {
		return (prop.startsWith("chestItems") || prop.startsWith("startItems") || prop.startsWith("boss.drops"));
	}

	public static String getItemValue(String prop) {
		if (prop.startsWith("chestItems")) return "chestItems";
		if (prop.startsWith("startItems")) return "startItems";
		if (prop.startsWith("boss.drops")) return "boss.drops";
		return null;
	}

	public static void copyConfigValues(MazeConfig src, MazeConfig dest) {
		dest.bossName = src.bossName;
		dest.bossMaxHp = src.bossMaxHp;
		dest.bossStrength = src.bossStrength;
		dest.groundReappearProb = src.groundReappearProb;
		dest.chestAppearProb = src.chestAppearProb;
		dest.enderChestAppearProb = src.enderChestAppearProb;
		dest.spawnMobProb = src.spawnMobProb;
		dest.chestWeighs = src.chestWeighs.clone();
		dest.chestItems = MazePvP.cloneISArray(src.chestItems.clone());
		dest.bossDropWeighs = src.bossDropWeighs.clone();
		dest.bossDropItems = MazePvP.cloneISArray(src.bossDropItems);
		dest.playerMaxDeaths = src.playerMaxDeaths;
		dest.startItems = MazePvP.cloneISArray(src.startItems.clone());
		dest.minPlayers = src.minPlayers;
		dest.maxPlayers = src.maxPlayers;
		dest.blockTypes = MazeConfig.cloneBlockTypes(src.blockTypes);
	}
	
	@SuppressWarnings("deprecation")
	public static void writeConfigToYml(MazeConfig config, Configuration ymlConf) {
		if (config == MazePvP.theMazePvP.rootConfig) ymlConf.set("showHeadsOnSpikes", MazePvP.theMazePvP.showHeads);
		if (config == MazePvP.theMazePvP.rootConfig) ymlConf.set("replaceMobsWithBoss", MazePvP.theMazePvP.replaceBoss);
		if (config == MazePvP.theMazePvP.rootConfig) ymlConf.set("fightStartDelay", MazePvP.theMazePvP.fightStartDelay/20);
		ymlConf.set("playerLives", config.playerMaxDeaths);
        ymlConf.set("playerNum.min", config.minPlayers);
        ymlConf.set("playerNum.max", config.maxPlayers);
        ymlConf.set("boss.name", config.bossName);
        ymlConf.set("boss.hp", config.bossMaxHp);
        ymlConf.set("boss.attack", config.bossStrength);
        int itemNum = config.bossDropItems.length;
        ymlConf.set("boss.drops.itemCount", itemNum);
        for (int i = 0; i < itemNum; i++) {
            ymlConf.set("boss.drops.item"+(i+1)+".id", config.bossDropItems[i].getTypeId());
            ymlConf.set("boss.drops.item"+(i+1)+".amount", config.bossDropItems[i].getAmount());
            ymlConf.set("boss.drops.item"+(i+1)+".weigh", config.bossDropWeighs[i]);
        }
		if (config == MazePvP.theMazePvP.rootConfig) {
			ymlConf.set("texts.startedState", MazePvP.theMazePvP.startedStateText);
			ymlConf.set("texts.waitingState", MazePvP.theMazePvP.waitingStateText);
			ymlConf.set("texts.joinSign", MazePvP.theMazePvP.joinSignText);
			ymlConf.set("texts.leaveSign", MazePvP.theMazePvP.leaveSignText);
			ymlConf.set("texts.joinBroadcast", MazePvP.theMazePvP.waitBroadcastText);
			ymlConf.set("texts.joinBroadcastWhenFull", MazePvP.theMazePvP.waitBroadcastFullText);
			ymlConf.set("texts.countdown", MazePvP.theMazePvP.countdownText);
			ymlConf.set("texts.fightStarted", MazePvP.theMazePvP.fightStartedText);
			ymlConf.set("texts.fightRespawn", MazePvP.theMazePvP.fightRespawnText);
			ymlConf.set("texts.fightRespawnLastLife", MazePvP.theMazePvP.lastRespawnText);
			ymlConf.set("texts.fightPlayerOut", MazePvP.theMazePvP.playerOutText);
			ymlConf.set("texts.fightWin", MazePvP.theMazePvP.winText);
			ymlConf.set("texts.fightStopped", MazePvP.theMazePvP.fightStoppedText);
			ymlConf.set("texts.onJoin", MazePvP.theMazePvP.joinMazeText);
			ymlConf.set("texts.onLeave", MazePvP.theMazePvP.leaveMazeText);
			ymlConf.set("texts.onJoinAfterFightStarted", MazePvP.theMazePvP.fightAlreadyStartedText);
			ymlConf.set("texts.onJoinWhenMazeFull", MazePvP.theMazePvP.mazeFullText);
			ymlConf.set("texts.onJoinWhenAlreadyJoinedOtherMaze", MazePvP.theMazePvP.joinedOtherText);
		}
        ymlConf.set("probabilities.groundReappear", config.groundReappearProb);
        ymlConf.set("probabilities.chestAppear", config.chestAppearProb);
        ymlConf.set("probabilities.enderChestAppear", config.enderChestAppearProb);
        ymlConf.set("probabilities.mobAppear", config.spawnMobProb);
        itemNum = config.chestItems.length;
        ymlConf.set("chestItems.itemCount", itemNum);
        for (int i = 0; i < itemNum; i++) {
            ymlConf.set("chestItems.item"+(i+1)+".id", config.chestItems[i].getTypeId());
            ymlConf.set("chestItems.item"+(i+1)+".amount", config.chestItems[i].getAmount());
            ymlConf.set("chestItems.item"+(i+1)+".weigh", config.chestWeighs[i]);
        }
        itemNum = config.startItems.length;
        ymlConf.set("startItems.itemCount", itemNum);
        for (int i = 0; i < itemNum; i++) {
            ymlConf.set("startItems.item"+(i+1)+".id", config.startItems[i].getTypeId());
            ymlConf.set("startItems.item"+(i+1)+".amount", config.startItems[i].getAmount());
        }
        for (int place = 0; place < config.blockTypes.length; place++) {
            List<String> blockList = new LinkedList<String>();
        	for (int i = 0; i < config.blockTypes[place].length; i++) {
	        	String blocks = "";
	        	for (int j = 0; j < config.blockTypes[place][i].length; j++) {
	        		blocks += config.blockTypes[place][i][j][0];
	        		if (config.blockTypes[place][i][j][1] != 0)
	        			blocks += ":"+config.blockTypes[place][i][j][1];
	        		if (j+1 < config.blockTypes[place][i].length) blocks += " ";
	        	}
	        	blockList.add(blocks);
	        }
        	ymlConf.set("blocks."+MazeConfig.blockTypeNames[place], blockList);
        }
	}
	

	@SuppressWarnings("deprecation")
	public static void loadConfigFromYml(MazeConfig config, Configuration ymlConf, Configuration rootConf, boolean pNumFromPrevPlace, boolean rootProps) {
		config.playerMaxDeaths = MazeConfig.getInt(ymlConf, rootConf, rootProps, "playerLives"); 
        if (!pNumFromPrevPlace) {
        	config.minPlayers = MazeConfig.getInt(ymlConf, rootConf, rootProps, "playerNum.min");
        	config.maxPlayers = MazeConfig.getInt(ymlConf, rootConf, rootProps, "playerNum.max");
		}
        config.bossName = MazeConfig.getString(ymlConf, rootConf, rootProps, "boss.name");
        config.bossMaxHp = MazeConfig.getInt(ymlConf, rootConf, rootProps, "boss.hp");
        config.bossStrength = MazeConfig.getInt(ymlConf, rootConf, rootProps, "boss.attack");
        config.groundReappearProb = MazeConfig.getDouble(ymlConf, rootConf, rootProps, "probabilities.groundReappear");
        config.chestAppearProb = MazeConfig.getDouble(ymlConf, rootConf, rootProps, "probabilities.chestAppear");
        config.enderChestAppearProb = MazeConfig.getDouble(ymlConf, rootConf, rootProps, "probabilities.enderChestAppear");
        config.spawnMobProb = MazeConfig.getDouble(ymlConf, rootConf, rootProps, "probabilities.mobAppear");
		
		int itemCount = MazeConfig.getInt(ymlConf, rootConf, rootProps, "chestItems.itemCount");
		
		ItemStack tempChestItems[] = new ItemStack[itemCount];
		double tempChestWeighs[] = new double[itemCount];
		int chestItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = MazeConfig.getInt(ymlConf, rootConf, rootProps, "chestItems.item"+(i+1)+".id");
			int amount = MazeConfig.getInt(ymlConf, rootConf, rootProps, "chestItems.item"+(i+1)+".amount");
			double weigh = MazeConfig.getDouble(ymlConf, rootConf, rootProps, "chestItems.item"+(i+1)+".weigh");
			if (id == 0) tempChestItems[i] = null;
			else {
				tempChestItems[i] = new ItemStack(id, amount);
				tempChestWeighs[i] = weigh;
				chestItemNum++;
			}
		}
		config.chestWeighs = new double[chestItemNum];
		config.chestItems = new ItemStack[chestItemNum];
		int place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempChestItems[i] != null) {
				config.chestItems[place] = tempChestItems[i];
				config.chestWeighs[place] = tempChestWeighs[i];
				place++;
			}
		}
		
		itemCount = MazeConfig.getInt(ymlConf, rootConf, rootProps, "boss.drops.itemCount");
		
		ItemStack tempBossItems[] = new ItemStack[itemCount];
		double tempBossWeighs[] = new double[itemCount];
		int bossItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = MazeConfig.getInt(ymlConf, rootConf, rootProps, "boss.drops.item"+(i+1)+".id");
			int amount = MazeConfig.getInt(ymlConf, rootConf, rootProps, "boss.drops.item"+(i+1)+".amount");
			double weigh = MazeConfig.getDouble(ymlConf, rootConf, rootProps, "boss.drops.item"+(i+1)+".weigh");
			if (id == 0) tempBossItems[i] = null;
			else {
				tempBossItems[i] = new ItemStack(id, amount);
				tempBossWeighs[i] = weigh;
				bossItemNum++;
			}
		}
		config.bossDropWeighs = new double[bossItemNum];
		config.bossDropItems = new ItemStack[bossItemNum];
		place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempBossItems[i] != null) {
				config.bossDropItems[place] = tempBossItems[i];
				config.bossDropWeighs[place] = tempBossWeighs[i];
				place++;
			}
		}
		
		itemCount = MazeConfig.getInt(ymlConf, rootConf, rootProps, "startItems.itemCount");
		ItemStack tempStartItems[] = new ItemStack[itemCount];
		int startItemNum = 0;
		for (int i = 0; i < itemCount; i++) {
			int id = MazeConfig.getInt(ymlConf, rootConf, rootProps, "startItems.item"+(i+1)+".id");
			int amount = MazeConfig.getInt(ymlConf, rootConf, rootProps, "startItems.item"+(i+1)+".amount");
			if (id == 0) tempStartItems[i] = null;
			else {
				tempStartItems[i] = new ItemStack(id, amount);
				startItemNum++;
			}
		}
		config.startItems = new ItemStack[startItemNum];
		place = 0;
		for (int i = 0; i < itemCount; i++) {
			if (tempStartItems[i] != null) {
				config.startItems[place] = tempStartItems[i];
				place++;
			}
		}
		
		config.blockTypes = new int[MazeConfig.blockTypeDimensions.length][][][];
		for (place = 0; place < MazeConfig.blockTypeNames.length; place++) {
			List<String> blocks = MazeConfig.getStringList(ymlConf, rootConf, rootProps, "blocks."+MazeConfig.blockTypeNames[place]);
			Iterator<String> it = blocks.iterator();
			String blockStr = "";
			while (it.hasNext()) {
				blockStr += it.next()+" ";
			}
			String idStr = "", dataStr = "";
			int strPlace = 0;
			boolean readingData = false;
			config.blockTypes[place] = new int
			   [MazeConfig.blockTypeDimensions[place][1]]
			   [MazeConfig.blockTypeDimensions[place][0]]
			   [2];
	        for (int i = 0; i < MazeConfig.blockTypeDimensions[place][1]; i++) {
	        	for (int j = 0; j < MazeConfig.blockTypeDimensions[place][0]; ) {
	        		if (strPlace >= blockStr.length()) {
        				config.blockTypes[place][i][j][0] = 1;
        				config.blockTypes[place][i][j][1] = 0;
        				j++;
	        		} else if (blockStr.charAt(strPlace) >= '0' && blockStr.charAt(strPlace) <= '9') {
	        			if (readingData) dataStr += blockStr.charAt(strPlace);
	        			else idStr += blockStr.charAt(strPlace);
	        		} else if (blockStr.charAt(strPlace) == ':' && !readingData) readingData = true;
	        		else {
	        			if (idStr.length() > 0) {
	        				int id, data;
	        				if (dataStr.length() == 0) data = 0;
	        				else data = Integer.parseInt(dataStr);
	        				id = Integer.parseInt(idStr);
	        				idStr = "";
	        				dataStr = "";
	        				config.blockTypes[place][i][j][0] = id;
	        				config.blockTypes[place][i][j][1] = data;
	        				readingData = false;
	        				j++;
	        			}
	        		}
	        		strPlace++;
	        	}
	        }
		}
	}

}
