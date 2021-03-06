package uk.co.mobsoc.MobsGames.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;


import uk.co.mobsoc.MobsGames.LogoutTimer;
import uk.co.mobsoc.MobsGames.MobsGames;
import uk.co.mobsoc.MobsGames.Data.StoredInventory;
import uk.co.mobsoc.MobsGames.Data.BlockData;
import uk.co.mobsoc.MobsGames.Data.GameData;
import uk.co.mobsoc.MobsGames.Data.LocationData;
import uk.co.mobsoc.MobsGames.Data.Utils;
import uk.co.mobsoc.MobsGames.Player.AbstractPlayerClass;
import uk.co.mobsoc.MobsGames.Player.GhostClass;
/**
 * An Abstraction of a Game, contains basic functions and values needed by most games. All new Games must be a subclass of this and override multiple functions
 * @author triggerhapp
 *
 */
public class AbstractGame {
	/**
	 * Returns the unique name of the game running
	 * @return
	 */
	public String getKey(){
		return gameData.key;
	}
	private boolean fin;
	private int startSpawnIndex;
	/** Time in seconds that the game should last for. */
	public int timeLimit;
	/** Time in seconds that the game has continued for. */
	public int timeElapsed;
	private int lastTickPercentage;
	
	protected ArrayList<LocationData> startLocations = new ArrayList<LocationData>();
	protected ArrayList<BlockData> blockAlterations = new ArrayList<BlockData>();
	protected ArrayList<AbstractPlayerClass> participants = new ArrayList<AbstractPlayerClass>();
	protected GameData gameData;
	/**
	 * Returns the number of Non-Ghost players.
	 * @return
	 */
	public int getNumPlayers(){
		int i = 0;
		for(AbstractPlayerClass apc : participants){
			if(!(apc instanceof GhostClass)){
				i++;
			}
		}
		return i;
	}
	
	/**
	 * Returns a list of all players who are participating, Ghost or not
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<AbstractPlayerClass> getParticipants(){
		return (ArrayList<AbstractPlayerClass>) participants.clone();
	}
	
	/**
	 * Return the Class (Custom or Core) of the player in question. Returns null if the person is not online, or is not participating
	 * @param player
	 * @return 
	 */
	public AbstractPlayerClass getPlayerClass(Player player){
		return getPlayerClass(player.getName());
	}
	
	/**
	 * Returns the Class (Custom or Core) of the player in question. Returns null if the person is not online, or is not participating
	 * @param name
	 * @return
	 */
	public AbstractPlayerClass getPlayerClass(String name){
		name = name.toLowerCase();
		for(AbstractPlayerClass apc: participants){
			if(name.equalsIgnoreCase(apc.getPlayerName())){
				return apc;
			}
		}
		return null;
	}
	
	/**
	 * Needs Overriding! Called when the game should start
	 */
	public void onStart(){
		System.out.println("Abstract game cannot be started!");
	}
	
	/**
	 * Needs Overriding! Called when the game should stop. Sometimes due to checkGameOver returning true, other times due to game being forced to stop early 
	 */
	public void onEnd(){
	}
	
	/**
	 * Needs Overriding! Return false if the game is proceeding, return true if the game should end
	 * @return
	 */
	public boolean checkGameOver(){
		return true;
	}
	
	/**
	 * Needs Overriding! Called every second, if server is not dragging behind
	 */
	public void onTick(){
		
	}

	/**
	 * Removes a player from the field, and from the Ghost group. Should only be called externally when they opt out or logged out. Do not Override
	 * @param name
	 * @param finalRemove
	 */
	public void removeParticipant(String name,boolean finalRemove) {
		AbstractPlayerClass apc = getPlayerClass(name);
		if(apc!=null){
			apc.onDisable();
			participants.remove(apc);
			if(finalRemove){
				onParticipantQuit(apc);
			}
		}
	}

	/**
	 * Returns a random player from the participants.
	 * @param klass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public AbstractPlayerClass getRandomParticipant(Class klass){
		ArrayList<AbstractPlayerClass> apcList = new ArrayList<AbstractPlayerClass>();
		for(AbstractPlayerClass apc : participants){
			if(klass.isInstance(apc)){
				apcList.add(apc);
			}
		}
		if(apcList.size()==0){
			return null;
		}
		Random r= new Random();
		return apcList.get(r.nextInt(apcList.size()));
	}

	/**
	 * Add or Alter the player in question as the specified Player Class. If the player already exists with the same Class, no effect is taken. This might not be what you wish if you store extra data in the class you wish to replace the player with. See me if so.
	 * @param klass
	 */
	public void setPlayerClass(AbstractPlayerClass klass) {
		AbstractPlayerClass lastClass = getPlayerClass(klass.getPlayerName());
		if(lastClass!=null && klass.getClass() == lastClass.getClass()){ return; }
		removeParticipant(klass.getPlayerName(),false);
		participants.add(klass);
		klass.onEnable();
	}

	/**
	 * Needs Overriding! Called when players need to be teleported, the arena needs to be set up, and the countdown begins.
	 */
	public void onStartCountdown() {
		
	}
	
	/**
	 * Moves all players into position, and sets them to the class specified by getDefaultClassForPlayer. If a player is already approximatly in location, no further teleport is taken. Ditto on class.
	 */
	private void teleportToStartPositions(){
		boolean skipTele=false;
		if(startLocations==null || startLocations.size()==0){
			startLocations = Utils.getLocations(getKey(), "start", "%");
			if(startLocations.size()==0){
				// Well shit, no saves start locations
				skipTele = true;
			}
		}
		ArrayList<String> players = new ArrayList<String>();
		for(AbstractPlayerClass apc : getParticipants()){
			if(!skipTele){
				boolean inPlace=false;
				for(LocationData ld : startLocations){
					if(apc.getPlayer()==null){ 
						inPlace=false;
					}else{
						Location pl = apc.getPlayer().getLocation();
						Location tl = ld.getLocation();
						if(tl.getBlockX() == pl.getBlockX() && tl.getBlockY() == pl.getBlockY() && tl.getBlockZ() == pl.getBlockZ() && tl.getWorld().getName().equalsIgnoreCase(pl.getWorld().getName())){
							// Just Eww ^
							inPlace=true;
							break;
						}
					}
				}
				if(!inPlace){
					apc.teleportTo(getNextStartSpawn());
				}

			}			
			// setPlayerClass alters the list of participants, must be run after
			players.add(apc.getPlayerName());
		}
		for(String player : players){
			setPlayerClass(getDefaultClassForPlayer(player));
		}
	}

	/**
	 * Override this! Return the class players should spawn as by default on start of match
	 * @param player
	 * @return
	 */
	public AbstractPlayerClass getDefaultClassForPlayer(String player) {
		return new GhostClass(player);
	}

	private Location getNextStartSpawn() {
		if(startSpawnIndex>=startLocations.size()){ startSpawnIndex=0; }
		Location l = startLocations.get(startSpawnIndex).getLocation();
		startSpawnIndex++;
		return l;
	}
	
	private void doTickFor(int percent){
		// First up, check for any blocks that need altering!
		for(BlockData bd : blockAlterations){
			int time = bd.getTimePercent();
			if(time>=0 && time==percent){
				System.out.println("Replacing block : "+bd.getCurrentBlockDataAt());
				addRevert(bd.getCurrentBlockDataAt());
				bd.doPlacement();
			}
		}
	}
	
	public String StartMessage = "Let the games begin!";

	/**
	 * DO NOT Override or call this function. This is called externally by CentralTick Class
	 */
	public void tick(){
		if(fin){ return; }
		timeElapsed++;
		if(timeElapsed==-10){
			MobsGames.announce("10 Seconds to go!");
		}else if(timeElapsed==-5){
			MobsGames.announce("5 Seconds to go!");
		}else if(timeElapsed==0){
			MobsGames.announce(StartMessage);
			onStart();
		}
		if(timeElapsed<=0){
			teleportToStartPositions();
		}

		if(timeElapsed>0 && (checkGameOver() || timeElapsed >= timeLimit)){
			stop();
			return;
		}
		int newTickPercentage = (timeElapsed*100)/timeLimit;
		if(timeElapsed>=0){
			if(newTickPercentage!=lastTickPercentage){
				for(int i = lastTickPercentage+1; i<=newTickPercentage; i++){
					doTickFor(i);
				}
				lastTickPercentage = newTickPercentage;
			}
		}
		if(hasBegun()){
			onTick();
		}

	}

	/**
	 * DO NOT Override or call this function. This is called externally by admin commands, or by automated response if enabled.
	 */
	public void start() {
		addParticipantsFromWaiting();
		fin=false;
		startSpawnIndex=0;
		timeLimit=gameData.timeLimit;
		timeElapsed=-20;
		lastTickPercentage=-1;
		blockAlterations = Utils.getBlocks(getKey(), "%", gameData.world);
		startLocations=null;
		onStartCountdown();
	}

	private void addParticipantsFromWaiting() {
		for(String s : MobsGames.instance.waitingList){
			if(s!=null){
				addParticipant(s);
			}
		}
		
	}

	/**
	 * DO NOT Override or call this function. This is called externally by admin commands.
	 */
	public void stop() {
		fin=true;
		doRevert();
		addParticipantsToWaiting();
		onEnd();
		for(AbstractPlayerClass apc : participants){
			apc.teleToSpawn();
			apc.onDisable();
		}
		MobsGames.setGame(null);

	}

	private void addParticipantsToWaiting() {
		MobsGames.instance.waitingList=new ArrayList<String>();
		for(AbstractPlayerClass apc : participants){
			MobsGames.instance.waitingList.add(apc.getPlayerName().toLowerCase());
		}
	}

	/**
	 * Override ONLY if new players should not join as ghost. If they can join anytime after the game starts, handle teleporting them in from here
	 */
	public void addParticipant(String player) {
		setPlayerClass(new GhostClass(player));
	}

	/**
	 * Removes a player from the field, and from the Ghost group. Should only be called externally when they opt out or logged out. Do not Override
	 * @param player
	 * @param finalRemove
	 */
	public void removeParticipant(Player player, boolean finalRemove) {
		removeParticipant(player.getName(), finalRemove);
	}

	/**
	 * DO NOT Override. This returns true if .stop() has been called before.
	 * @return
	 */
	public boolean isFinished() {
		return fin;
	}

	/**
	 * DO NOT Override or call internally. Sets the specific game data for this game.
	 * @param gameData
	 */
	public void setGameData(GameData gameData) {
		setOtherData(gameData.extraData);
		this.gameData = gameData;
	}

	/**
	 * Override if you want your plugin to use extra data stored in the "otherData" section of the game. Forward thinking is fun!
	 * @param otherData
	 */
	public void setOtherData(HashMap<String,String> otherData) {
		
	}

	/**
	 * timeElapsed starts at -20 ("Game will start in 20 seconds"), so this returns true if the game has begun.
	 * @return
	 */
	public boolean hasBegun() {
		return timeElapsed>=0;
	}

	public boolean isParticipant(Player player) {
		for(AbstractPlayerClass apc : participants){
			if(apc.isPlayerEqual(player, apc.getPlayer())){
				return true;
			}
		}
		return false;
	}
	
	private ArrayList<BlockData> revertList = new ArrayList<BlockData>();

	/**
	 * Adds a block to the list of blocks that will revert to original state. If your plugin alters any blocks during gameplay (other than via those of type 'time:x') then they should be added to this list BEFORE being altered.
	 * @param bd
	 */
	public void addRevert(BlockData bd) {
		for(BlockData bd2 : revertList){
			if(bd2.isEqualLocation(bd)){
				return;
			}
		}
		revertList.add(bd);	
	}
	
	private void doRevert(){
		System.out.println("Reverting : "+revertList.size()+" blocks");
		for(int i = revertList.size()-1 ; i>=0 ; i--){
			revertList.get(i).doPlacement();
		}
		for(StoredInventory sI : inventories){
			sI.revertNow();
		}
	}

	/**
	 * Needs Overriding! for catching players who have logged out or used /game leave
	 * @param player
	 */
	public void onParticipantQuit(AbstractPlayerClass player) {
		
	}
	
	/**
	 * Returns a list of Players who are not GhostClass
	 * @return
	 */
	public ArrayList<AbstractPlayerClass> getNonGhosts(){
		ArrayList<AbstractPlayerClass> noGhost = new ArrayList<AbstractPlayerClass>();
		for(AbstractPlayerClass apc : participants){
			if(!(apc instanceof GhostClass)){
				noGhost.add(apc);
			}
		}
		return noGhost;
	}
	
	/**
	 * Force all players into GhostClass
	 */
	public void ghostAll(){
		for(AbstractPlayerClass apc : getParticipants()){
			setPlayerClass(new GhostClass(apc.getPlayerName()));
		}
	}

	/**
	 * Adds a new Block-Inventory to the list of inventories to revert to original state
	 */
	ArrayList<StoredInventory> inventories = new ArrayList<StoredInventory>();
	public void addRevert(InventoryHolder iH) {
		if(!(iH instanceof BlockState)){ return; }
		for(StoredInventory sI : inventories){
			if(sI.equalLocation((BlockState)iH)){
				return; 
			}
		}
		StoredInventory sI = new StoredInventory(iH);
		inventories.add(sI);
	}

	/**
	 * DO NOT OVERRIDE. Called externally when a player logs out to start the timer, if the player remains logged out then they are forfeited from the game
	 * @param apc
	 */
	public void startLogOutTimer(AbstractPlayerClass apc) {
		new LogoutTimer(apc, 30);
	}
	
}
