package me.ayunami2000.MakiDesktop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class InputEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMoveEvent(PlayerMoveEvent event){
        if(!MakiDesktop.directControl)return;
        Player player=event.getPlayer();
        if(player!=MakiDesktop.controller)return;
        float endYaw=event.getFrom().getYaw()-event.getTo().getYaw();
        MakiDesktop.clickMouse(10*endYaw-32767,10*event.getTo().getPitch()-32767,0,false);
        event.setCancelled(true);//moves player back
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent event){
        if(!MakiDesktop.directControl)return;
        Player player=event.getPlayer();
        if(player!=MakiDesktop.controller)return;
        MakiDesktop.pressKey("Sneak",event.isSneaking());
        event.setCancelled(true);
    }
}
