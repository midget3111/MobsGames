package uk.co.mobsoc.MobsGames;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldLoadEvent;

import uk.co.mobsoc.MobsGames.Data.BlockData;
import uk.co.mobsoc.MobsGames.Data.SavedData;

public class GenericListener implements Listener {
	/**
	 * This is in the wrong place, but I didnt want to make a listener just for one measly function.
	 * This captures players selecting a block (By right clicking with flint in hand) 
	 * @param event
	 */
	@EventHandler
	public void onInteract(PlayerInteractEvent event){
		if(event.isCancelled()){ return; }
		if(!event.hasBlock()){ return; }
		if(!event.getPlayer().hasPermission("games.alter")){ return ; }
		if(event.getPlayer().getItemInHand().getType() == Material.FLINT){
			event.setCancelled(true);
			Block b = event.getClickedBlock();
			MobsGames.instance.blockSelected.put(event.getPlayer().getName().toLowerCase(), new BlockData(b));
			event.getPlayer().sendMessage("Selected block : "+b.getX()+" "+b.getY()+" "+b.getZ());
		}
	}
	
	@EventHandler
	public void onFireSet(BlockBurnEvent event){
		event.setCancelled(true);
	}
	
	public void onWorldLoad(WorldLoadEvent event){
		SavedData.loadWorld(event.getWorld());
	}
}
