package me.ayunami2000.MakiDesktop;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class ScreenClickEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEntityEvent event){
        if(MakiDesktop.paused||MakiDesktop.loc==null||MakiDesktop.locEnd==null)return;
        Player player = event.getPlayer();
        if(player!=MakiDesktop.controller&&!player.isOp())return;
        if(MakiDesktop.directControl&&player==MakiDesktop.controller)return;
        Block block = player.getTargetBlock(5);
        //EntityType entityType = event.getPlayer().getTargetEntity(5).getType();
        //if(entityType==EntityType.ITEM_FRAME||entityType==EntityType.GLOW_ITEM_FRAME) {
            if (ClickOnScreen.clickedOnBlock(block, player,true)) {}//event.setCancelled(true);
        //}
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeftClick(PlayerInteractEvent event) {
        //onRightClick(event);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRightClick(PlayerInteractEvent event) {
        if(MakiDesktop.paused||MakiDesktop.loc==null||MakiDesktop.locEnd==null)return;
        Player player = event.getPlayer();
        if(player!=MakiDesktop.controller&&!player.isOp())return;
        if(MakiDesktop.directControl&&player==MakiDesktop.controller)return;
        Action action = event.getAction();

        if ((action.equals(Action.RIGHT_CLICK_BLOCK)/*||action.equals(Action.LEFT_CLICK_BLOCK)*/)&&event.getHand()==EquipmentSlot.HAND) {
            Block block = event.getClickedBlock();
            Block tblock = player.getTargetBlock(5);
            Location bloc=block.getLocation();
            Location tloc=tblock.getLocation();
            if(bloc.getX()==tloc.getX()&&bloc.getY()==tloc.getY()&&bloc.getZ()==tloc.getZ()){
                if(ClickOnScreen.clickedOnBlock(block,player,true)){}//event.setCancelled(true);
            }
        }
    }
}