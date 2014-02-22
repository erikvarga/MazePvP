package mazepvp;

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.util.BlockIterator;

public final class EventListeners implements Listener {

	@EventHandler
    public void wLoadListener(WorldLoadEvent event) {
		MazePvP.theMazePvP.loadMazeProps(event.getWorld());
	}

	@EventHandler
    public void wUnloadListener(WorldUnloadEvent event) {
		MazePvP.theMazePvP.removeMazes(event.getWorld());
	}
	
	@EventHandler
    public void wSaveListener(WorldSaveEvent event) {
		MazePvP.theMazePvP.saveMazeProps(event.getWorld());
	}

	@EventHandler
    public void explodeListener(EntityExplodeEvent event) {
		Iterator<Block> it = event.blockList().iterator();
		while (it.hasNext()) {
			Block block = it.next();
			Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
			while (mit.hasNext()) {
				Maze maze = mit.next();
				if (block.getType() != Material.TNT
				&& block.getX() >= maze.mazeX && block.getX() <= maze.mazeX+maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
	  	      	&&  block.getZ() >= maze.mazeZ && block.getZ() <= maze.mazeZ+maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
	  	      	&&  block.getY() >= maze.mazeY-Maze.MAZE_PASSAGE_DEPTH && block.getY() <= maze.mazeY+Maze.MAZE_PASSAGE_HEIGHT+2) {
					it.remove();
					break;
				}
			}
		}
    }

	@EventHandler
    public void projHitListener(ProjectileHitEvent event) {
		if (!(event.getEntity().getShooter() instanceof LivingEntity)) return;
		if (!(event.getEntity() instanceof Egg || event.getEntity() instanceof Snowball || event.getEntity() instanceof EnderPearl)) return;
		Iterator<Maze> it = MazePvP.theMazePvP.mazes.iterator();
		while (it.hasNext()) {
			Maze maze = it.next();
			World world = event.getEntity().getWorld();
			if (event.getEntity() instanceof EnderPearl && event.getEntity().getShooter() != null && maze.isInsideMaze(((LivingEntity)event.getEntity().getShooter()).getLocation())) {
	      		Point2D.Double loc = maze.getMazeBossNewLocation(world);
	      		LivingEntity thrower = (LivingEntity)event.getEntity().getShooter();
        		for (int j = 0; j <= 8; j++) {
        			world.playEffect(thrower.getLocation(), Effect.SMOKE, j);
        			if (j != 4) {
        				world.playEffect(new Location(world, thrower.getLocation().getX(), thrower.getLocation().getY()+1, thrower.getLocation().getZ()), Effect.SMOKE, j);
        			}
        		}
            	thrower.teleport(new Location(world, loc.x, maze.mazeY+1, loc.y));
        		for (int j = 0; j <= 8; j++) {
        			world.playEffect(thrower.getLocation(), Effect.SMOKE, j);
        			if (j != 4) {
        				world.playEffect(new Location(world, thrower.getLocation().getX(), thrower.getLocation().getY()+1, thrower.getLocation().getZ()), Effect.SMOKE, j);
        			}
        		}
                thrower.setFallDistance(0.0F);
	      	}
			BlockIterator bit = new BlockIterator(world, event.getEntity().getLocation().toVector(), event.getEntity().getVelocity().normalize(), 0.0D, 4);
			Block block;
			if (bit.hasNext()) {
				block = null;
				while (bit.hasNext()) {
					block = bit.next();
		            if (block.getType() != Material.AIR) {
		                break;
		            }
		            if (!bit.hasNext()) return;
		        }
				int yOffs = 0;
				int yOffs2 = 0;
				if (event.getEntity() instanceof Egg) yOffs = 1;
				if (event.getEntity() instanceof EnderPearl || event.getEntity() instanceof Snowball) yOffs2 = 2;
		  		if (block.getType() != Material.AIR) {
			  		if (block.getX() >= maze.mazeX && block.getX() <= maze.mazeX+maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
			      	&&  block.getZ() >= maze.mazeZ && block.getZ() <= maze.mazeZ+maze.mazeSize*(1+Maze.MAZE_PASSAGE_WIDTH)
			      	&&  block.getY() >= maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+yOffs && block.getY() <= maze.mazeY+Maze.MAZE_PASSAGE_HEIGHT+yOffs2) {
			  			if (event.getEntity() instanceof Egg) {
			  	      		int mazeX = maze.blockToMazeCoord(block.getX()-maze.mazeX);
			  	      		int mazeZ = maze.blockToMazeCoord(block.getZ()-maze.mazeZ);
			  	      		int sideHit = 0;
			  	      		//System.out.println("B: "+block.getLocation());
			  	      		//System.out.println("S: "+event.getEntity().getLocation());
			  	      		if (Math.abs(event.getEntity().getLocation().getY()-0.5-block.getY()) >
			  	      		    Math.max(Math.abs(event.getEntity().getLocation().getZ()-0.5-block.getZ()), Math.abs(event.getEntity().getLocation().getX()-0.5-block.getX()))) {
				  	      		if (event.getEntity().getLocation().getY()-0.5 < block.getY()) sideHit = 0;
			  	      			else sideHit = 1;
			  	      		} else if (mazeX%2 == 0 && mazeZ%2 == 0) {
			  	      			if (Math.abs(event.getEntity().getLocation().getZ()-0.5-block.getZ()) > Math.abs(event.getEntity().getLocation().getX()-0.5-block.getX())) {
				  	      			if (event.getEntity().getLocation().getZ()-0.5 < block.getZ()) sideHit = 2;
				  	      			else sideHit = 3;
			  	      			} else {
				  	      			if (event.getEntity().getLocation().getX()-0.5 < block.getX()) sideHit = 4;
				  	      			else sideHit = 5;
			  	      			}
			  	      		} else if (mazeZ%2 == 0) {
			  	      			if (event.getEntity().getLocation().getZ()-0.5 < block.getZ()) sideHit = 2;
			  	      			else sideHit = 3;
			  	      		} else if (mazeX%2 == 0) {
			  	      			if (event.getEntity().getLocation().getX()-0.5 < block.getX()) sideHit = 4;
			  	      			else sideHit = 5;
			  	      		}
			  	      		//System.out.println("side: "+sideHit);
				      		if (block.getY() > maze.mazeY) {
				      			if (mazeX%2 == 0 && mazeZ%2 == 0) {
				      				if (sideHit == 5) mazeZ++;
				      				else if (sideHit == 4) mazeZ--;
				      				else if (sideHit == 3) mazeX--;
				      				else if (sideHit == 2) mazeX++;
				      			}
				      		} else if (sideHit > 1) {
				      			if (mazeX%2 == 0) {
				      				if (world.getBlockAt(new Location(world, block.getX()+1, block.getY(), block.getZ())).getType() != Material.AIR) mazeX++;
				      				else if (world.getBlockAt(new Location(world, block.getX()-1, block.getY(), block.getZ())).getType() != Material.AIR) mazeX--;
				      				else return;
				      			} else if (mazeZ%2 == 0) {
				      				if (world.getBlockAt(new Location(world, block.getX(), block.getY(), block.getZ()+1)).getType() != Material.AIR) mazeZ++;
				      				else if (world.getBlockAt(new Location(world, block.getX(), block.getY(), block.getZ()-1)).getType() != Material.AIR) mazeZ--;
				      				else return;
				      			}
				      		}
				      		maze.removeMazeBlocks(mazeX, mazeZ, world);
			  			} else if (event.getEntity() instanceof Snowball) {
			  	      		int mazeX = maze.blockToMazeCoord(block.getX()-maze.mazeX);
			  	      		int mazeZ = maze.blockToMazeCoord(block.getZ()-maze.mazeZ);
			  	      		int sideHit = 0;
			  	      		//System.out.println("B: "+block.getLocation());
			  	      		//System.out.println("S: "+event.getEntity().getLocation());
			  	      		if (Math.abs(event.getEntity().getLocation().getY()-0.5-block.getY()) >
			  	      		    Math.max(Math.abs(event.getEntity().getLocation().getZ()-0.5-block.getZ()), Math.abs(event.getEntity().getLocation().getX()-0.5-block.getX()))) {
				  	      		if (event.getEntity().getLocation().getY()-0.5 < block.getY()) sideHit = 0;
			  	      			else sideHit = 1;
			  	      		} else if (mazeX%2 == 0 && mazeZ%2 == 0) {
			  	      			if (Math.abs(event.getEntity().getLocation().getZ()-0.5-block.getZ()) > Math.abs(event.getEntity().getLocation().getX()-0.5-block.getX())) {
				  	      			if (event.getEntity().getLocation().getZ()-0.5 < block.getZ()) sideHit = 2;
				  	      			else sideHit = 3;
			  	      			} else {
				  	      			if (event.getEntity().getLocation().getX()-0.5 < block.getX()) sideHit = 4;
				  	      			else sideHit = 5;
			  	      			}
			  	      		} else if (mazeZ%2 == 0) {
			  	      			if (event.getEntity().getLocation().getZ()-0.5 < block.getZ()) sideHit = 2;
			  	      			else sideHit = 3;
			  	      		} else if (mazeX%2 == 0) {
			  	      			if (event.getEntity().getLocation().getX()-0.5 < block.getX()) sideHit = 4;
			  	      			else sideHit = 5;
			  	      		}
			  	      		//System.out.println("side: "+sideHit);
			  				if (block.getY() > maze.mazeY) {
			  	              	float midPos;
			  	      			if (mazeX%2 == 0 && mazeZ%2 == 0) {
			  	      				if (sideHit == 2) mazeZ--;
			  	      				else if (sideHit == 3) mazeZ++;
			  	      				else if (sideHit == 4) mazeX--;
			  	      				else if (sideHit == 5) mazeX++;
			  	      			} else if (mazeX%2 == 0 && mazeZ%2 != 0) {
			  	      	        	midPos = maze.mazeToBlockCoord(mazeZ)+Maze.MAZE_PASSAGE_WIDTH*0.5f;
			  	      	        	if (block.getZ() >= maze.mazeZ+midPos) {
			  	      	        		//if (sideHit == 4 && mazeX > 0 && mazeZ < maze.mazeSize*2 && maze.maze[mazeX-1][mazeZ+1] == 1) return;
			  	      	        		//if (sideHit == 5 && mazeX < maze.mazeSize*2 && mazeZ < maze.mazeSize*2 && maze.maze[mazeX+1][mazeZ+1] == 1) return;
			  	      	        		mazeZ++;
			  	    	        		if (sideHit == 4) mazeX--;
			  	    	        		else if (sideHit == 5) mazeX++;
			  	      	        	} else {
			  	      	        		//if (sideHit == 4 && mazeX > 0 && mazeZ > 0 && maze.maze[mazeX-1][mazeZ-1] == 1) return;
			  	      	        		//if (sideHit == 5 && mazeX < maze.mazeSize*2 && mazeZ > 0 && maze.maze[mazeX+1][mazeZ-1] == 1) return;
			  	      	        		mazeZ--;
			  	    	        		if (sideHit == 4) mazeX--;
			  	    	        		else if (sideHit == 5) mazeX++;
			  	      	        	}
			  	      			} else if (mazeX%2 != 0 && mazeZ%2 == 0) {
			  	      				midPos = maze.mazeToBlockCoord(mazeX)+Maze.MAZE_PASSAGE_WIDTH*0.5f;
			  		    	        	if (block.getX() >= maze.mazeX+midPos) {
			  		    	        		//if (sideHit == 2 && mazeZ > 0 && mazeX < maze.mazeSize*2 && maze.maze[mazeX+1][mazeZ-1] == 1) return;
			  		    	        		//if (sideHit == 3 && mazeZ < maze.mazeSize*2 && mazeX < maze.mazeSize*2 && maze.maze[mazeX+1][mazeZ+1] == 1) return;
			  		    	        		mazeX++;
			  		    	        		if (sideHit == 2) mazeZ--;
			  		    	        		else if (sideHit == 3) mazeZ++;
			  		    	        	} else {
			  		    	        		//if (sideHit == 2 && mazeZ > 0 && mazeX > 0 && maze.maze[mazeX-1][mazeZ-1] == 1) return;
			  		    	        		//if (sideHit == 3 && mazeZ < maze.mazeSize*2 && mazeX > 0 && maze.maze[mazeX-1][mazeZ+1] == 1) return;
			  		    	        		mazeX--;
			  		    	        		if (sideHit == 2) mazeZ--;
			  		    	        		else if (sideHit == 3) mazeZ++;
			  		    	        	}
			  	      			}
			  	      		} else {
			  	      			if (mazeX%2 == 0) {
			  	      				if (sideHit == 5) mazeX++;
			  	      				else if (sideHit == 4) mazeX--;
			  	      			} else if (mazeZ%2 == 0) {
			  	      				if (sideHit == 3) mazeZ++;
			  	      				else if (sideHit == 2) mazeZ--;
			  	      			}
			  	      		}
			  	      		maze.restoreMazeBlocks(mazeX, mazeZ);
				      	}
					}
				}
			}
		}
    }
	@EventHandler
    public void teleportListener(PlayerTeleportEvent event) {
    	if(event.getCause().equals(TeleportCause.ENDER_PEARL)) {
			Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
			while (mit.hasNext()) {
				Maze maze = mit.next();
	    		if (maze.isInsideMaze(event.getPlayer().getLocation())) {
	    			event.setCancelled(true);
	    			break;
	    		}
			}
    	}
    }
	
	@SuppressWarnings("deprecation")
	@EventHandler
    public void entityDeathListener(EntityDeathEvent event) {
		Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
		while (mit.hasNext()) {
			Maze maze = mit.next();
			boolean isBoss = false;
			Iterator<Boss> bit = maze.bosses.iterator();
			while (bit.hasNext()) {
				Boss boss = bit.next();
				if (boss.entity == event.getEntity()) {
					isBoss = true;
					boss.entity.getEquipment().setBootsDropChance(0);
					boss.entity.getEquipment().setLeggingsDropChance(0);
					boss.entity.getEquipment().setChestplateDropChance(0);
					boss.entity.getEquipment().setHelmetDropChance(0);
					boss.entity.getEquipment().setItemInHandDropChance(0);
					event.getDrops().clear();
					if (maze.configProps.bossDropItems.length > 0) {
						double currentWeigh = 0.0;
						double weighSum = 0.0;
						int i;
						for (i = 0; i < maze.configProps.bossDropWeighs.length; i++) weighSum += maze.configProps.bossDropWeighs[i];
						double randNum = Math.random()*weighSum;
						for (i = 0; i < maze.configProps.bossDropWeighs.length; i++) {
							currentWeigh += maze.configProps.bossDropWeighs[i];
							if (currentWeigh >= randNum) break;
						}
						event.getDrops().add(maze.configProps.bossDropItems[i].clone());
					}
					boss.entity.setCustomName(null);
					boss.entity.setCustomNameVisible(false);
					boss.entity = null;
					break;
				}
			}
			if (MazePvP.theMazePvP.showHeads && event.getEntity() instanceof Player || event.getEntity() instanceof Spider || event.getEntity() instanceof Zombie || event.getEntity() instanceof Skeleton || event.getEntity() instanceof Creeper) {
				if (maze.isInsideMaze(event.getEntity().getLocation())) {
					Location loc = event.getEntity().getLocation();
					if (loc.getY() <= maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+2.5) {
						int x = (int)Math.round(loc.getX()-0.5);
						int z = (int)Math.round(loc.getZ()-0.5);
						if ((x-maze.mazeX)%(Maze.MAZE_PASSAGE_WIDTH+1) == 0) x++;
						if ((z-maze.mazeZ)%(Maze.MAZE_PASSAGE_WIDTH+1) == 0) z++;
						event.getEntity().getWorld().getBlockAt(x, maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+2, z).setTypeIdAndData(144, (byte)1, true);
						Skull skull = (Skull)(event.getEntity().getWorld().getBlockAt(x, maze.mazeY-Maze.MAZE_PASSAGE_DEPTH+2, z).getState());
						if (event.getEntity() instanceof Skeleton) skull.setSkullType(SkullType.SKELETON);
						else if (isBoss) skull.setSkullType(SkullType.WITHER);
						else if (event.getEntity() instanceof Zombie) skull.setSkullType(SkullType.ZOMBIE);
						else if (event.getEntity() instanceof Player) {
							skull.setSkullType(SkullType.PLAYER);
							skull.setOwner(((Player)event.getEntity()).getName());
						} else if (event.getEntity() instanceof Creeper) skull.setSkullType(SkullType.CREEPER);
						else if (event.getEntity() instanceof Spider) {
							skull.setSkullType(SkullType.PLAYER);
							skull.setOwner("MHF_Spider");
						}
						skull.setRotation(MazePvP.getRandomRotation());
						skull.update();
					}
				}
			}
			if (event.getEntity() instanceof Player) {
				Player player = (Player)event.getEntity();
				if (!maze.canBeEntered && maze.fightStarted) {
					PlayerProps props = maze.joinedPlayerProps.get(player.getName());
					if (props != null) {
						props.deathCount++;
						if (maze.lastPlayer == null && props.deathCount >= maze.configProps.playerMaxDeaths) {
							List<Player> players = maze.getPlayersInGame();
							if (players.size() == 1) {
								Player lastPlayer = players.get(0);
								if (lastPlayer != null) {
									maze.sendStringListToPlayer(lastPlayer, MazePvP.theMazePvP.winText);
									maze.fightStartTimer = 0;
									maze.lastPlayer = lastPlayer;
								}
							} else maze.sendPlayerOutMessageToPlayers();
						}
					}
					new ForceRespawnRunnable(player.getName()).runTaskLater(MazePvP.theMazePvP, 60L);
				}
			}
		}
    }
	
	@EventHandler
    public void entityDamageByEntityListener(EntityDamageByEntityEvent event) {
		Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
		while (mit.hasNext()) {
			Maze maze = mit.next();
			Boss damagerBoss = null;
			Boss damagedBoss = null;
			Iterator<Boss> bit = maze.bosses.iterator();
			while (bit.hasNext()) {
				Boss boss = bit.next();
				if (event.getDamager() == boss.entity) damagerBoss = boss;
				if (event.getEntity() == boss.entity) damagedBoss = boss;
				if (boss.entity != null && event.getEntity() instanceof Player) {
	    			boss.entity.setCustomName(maze.configProps.bossName);
	    			boss.entity.setCustomNameVisible(false);
				}
			}
	    	if (damagerBoss != null && event.getEntity() instanceof LivingEntity) {
	    		if (damagerBoss.tpCooldown > 0) {
	    			event.setCancelled(true);
	    		} else {
	    			event.setDamage(maze.configProps.bossStrength == 0 ? ((LivingEntity)event.getEntity()).getHealth()*10 : maze.configProps.bossStrength);
	    		}
	    	}
	    	if (damagedBoss != null) {
	    		if (event.getDamager() instanceof Player) {
	    			damagedBoss.targetPlayer = ((Player)event.getDamager()).getName();
	    			damagedBoss.targetTimer = Math.min(MazePvP.BOSS_TIMER_MAX, damagedBoss.targetTimer+20);
	    		}
	    	}
		}
    }


	@EventHandler
	public void entityDamageListener(EntityDamageEvent event) {
		Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
		while (mit.hasNext()) {
			Maze maze = mit.next();
			Iterator<Boss> bit = maze.bosses.iterator();
			while (bit.hasNext()) {
				Boss boss = bit.next();
		    	if (boss.entity == event.getEntity()) {
		    		boss.hp = Math.max(0.0,  boss.hp-event.getDamage());
		    		maze.updateBossHpStr(boss);
		    		if (boss.hp <= 0.0 && maze.configProps.bossMaxHp > 0) {
		    			boss.entity.setHealth(0);
		    		} else {
		    			event.setDamage(0);
		    		}
		    		if (event.getCause() == DamageCause.SUFFOCATION) {
		    			maze.relocateMazeBoss(false, boss);
		    		}
		    		break;
		    	}
			}
		}
	}
	
	@EventHandler
    public void playerQuitListener(PlayerQuitEvent event) {
		Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
		while (mit.hasNext()) {
			Maze maze = mit.next();
			if (maze.playerInsideMaze.containsKey(event.getPlayer().getName()) && maze.playerInsideMaze.get(event.getPlayer().getName())) {
				if (!maze.canBeEntered) {
					maze.playerQuit(event.getPlayer());
					maze.updateSigns();
				} else {
					maze.playerInsideMaze.remove(event.getPlayer().getName());
				}
			}
		}
    }
	
	@EventHandler
    public void playerJoinListener(PlayerJoinEvent event) {
		Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
		while (mit.hasNext()) {
			Maze maze = mit.next();
			if (maze.canBeEntered && maze.isInsideMaze(event.getPlayer().getLocation())) {
				maze.playerInsideMaze.put(event.getPlayer().getName(), true);
			}
		}
    }
	
	@EventHandler
	public void blockBreakListener(BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.SIGN_POST || event.getBlock().getType() == Material.WALL_SIGN) {
			Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
			while (mit.hasNext()) {
				Maze maze = mit.next();
				if (maze.canBeEntered) continue;
				if (maze.mazeWorld != event.getBlock().getWorld()) continue;
				int[] sign = maze.findSign(event.getBlock().getLocation(), maze.joinSigns);
				if (sign != null) {
					maze.removeSign(sign, maze.joinSigns);
					break;
				}
				sign = maze.findSign(event.getBlock().getLocation(), maze.leaveSigns);
				if (sign != null) {
					maze.removeSign(sign, maze.leaveSigns);
					break;
				}
			}
		}
	}
	
	@EventHandler
	public void playerInteractListener(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getClickedBlock().getType() == Material.WALL_SIGN || event.getClickedBlock().getType() == Material.SIGN_POST) {
				Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
				while (mit.hasNext()) {
					Maze maze = mit.next();
					if (maze.canBeEntered) continue;
					if (maze.mazeWorld != event.getClickedBlock().getWorld()) continue;
					int[] sign = maze.findSign(event.getClickedBlock().getLocation(), maze.joinSigns);
					if (sign != null) {
						String pName = event.getPlayer().getName();
						if (maze.playerInsideMaze.containsKey(pName) && maze.playerInsideMaze.get(pName)) {
							maze.sendStringListToPlayer(event.getPlayer(), MazePvP.theMazePvP.leaveMazeText);
							maze.playerQuit(event.getPlayer());
							maze.updateSigns();
						} else {
							if (maze.fightStarted) {
								maze.sendStringListToPlayer(event.getPlayer(), MazePvP.theMazePvP.fightAlreadyStartedText);
							} else if (maze.configProps.maxPlayers <= maze.playerInsideMaze.size()) {
								maze.sendStringListToPlayer(event.getPlayer(), MazePvP.theMazePvP.mazeFullText);
							} else if (Maze.playerInsideAMaze.containsKey(pName) && Maze.playerInsideAMaze.get(pName)) {
								maze.sendStringListToPlayer(event.getPlayer(), MazePvP.theMazePvP.joinedOtherText);
							} else {
								maze.sendStringListToPlayer(event.getPlayer(), MazePvP.theMazePvP.joinMazeText);
								maze.playerJoin(event.getPlayer());
								maze.updateSigns();
							}
						}
						break;
					}
					sign = maze.findSign(event.getClickedBlock().getLocation(), maze.leaveSigns);
					if (sign != null) {
						String pName = event.getPlayer().getName();
						if (maze.playerInsideMaze.containsKey(pName) && maze.playerInsideMaze.get(pName)) {
							maze.sendStringListToPlayer(event.getPlayer(), MazePvP.theMazePvP.leaveMazeText);
							maze.playerQuit(event.getPlayer());
							maze.updateSigns();
						}
					}
				}
			}
		}
	}
	
	@EventHandler
	public void playerRespawnListener(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if (Maze.playerInsideAMaze.containsKey(player.getName()) && Maze.playerInsideAMaze.get((player.getName()))) {
			Iterator<Maze> mit = MazePvP.theMazePvP.mazes.iterator();
			while (mit.hasNext()) {
				Maze maze = mit.next();
				if (maze.canBeEntered || !maze.fightStarted) continue;
				PlayerProps props = maze.joinedPlayerProps.get(player.getName());
				if (props != null) {
					if (event.getPlayer() != maze.lastPlayer && props.deathCount < maze.configProps.playerMaxDeaths) {
						Point2D.Double loc = maze.getMazeBossNewLocation(maze.mazeWorld);
						MazePvP.cleanUpPlayer(player, props.deathCount != 0);
						maze.giveStartItemsToPlayer(player);
						event.setRespawnLocation(new Location(maze.mazeWorld, loc.x, maze.mazeY+1, loc.y));
					 	maze.mazeWorld.playEffect(event.getRespawnLocation(), Effect.MOBSPAWNER_FLAMES, 0);
						if (props.deathCount+1 < maze.configProps.playerMaxDeaths) maze.sendStringListToPlayer(player, MazePvP.theMazePvP.fightRespawnText);
						else maze.sendStringListToPlayer(player, MazePvP.theMazePvP.lastRespawnText);
					} else {
						event.setRespawnLocation(props.prevLocation);
						maze.playerQuit(player);
					}
					break;
				}
			}
		}
	}
	
}
