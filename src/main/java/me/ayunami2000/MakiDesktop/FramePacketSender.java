package me.ayunami2000.MakiDesktop;

import net.minecraft.network.protocol.game.PacketPlayOutMap;
import net.minecraft.world.level.saveddata.maps.WorldMap.b;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

class FramePacketSender extends BukkitRunnable implements Listener {
  private long frameNumber = 0;
  private final Queue<byte[][]> frameBuffers;
  private final MakiDesktop plugin;

  public FramePacketSender(MakiDesktop plugin, Queue<byte[][]> frameBuffers) {
    this.frameBuffers = frameBuffers;
    this.plugin = plugin;
    this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void run() {
    byte[][] buffers = frameBuffers.poll();
    if (buffers == null) {
      return;
    }
    List<PacketPlayOutMap> packets = new ArrayList<>(MakiDesktop.screens.size());
    for (ScreenPart screenPart : MakiDesktop.screens) {
      byte[] buffer = buffers[screenPart.partId];
      if (buffer != null) {
        PacketPlayOutMap packet = getPacket(screenPart.mapId, buffer);
        if (!screenPart.modified) {
          packets.add(0, packet);
        } else {
          packets.add(packet);
        }
        screenPart.modified = true;
        screenPart.lastFrameBuffer = buffer;
      } else {
        screenPart.modified = false;
      }
    }

    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      sendToPlayer(onlinePlayer, packets);
    }

    if (frameNumber % 300 == 0) {
      byte[][] peek = frameBuffers.peek();
      if (peek != null) {
        frameBuffers.clear();
        frameBuffers.offer(peek);
      }
    }
    frameNumber++;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    //do i REALLY need this to be added to the task list? disabled for now...
    new BukkitRunnable() {
      @Override
      public void run() {
        List<PacketPlayOutMap> packets = new ArrayList<>();
        for (ScreenPart screenPart : MakiDesktop.screens) {
          if (screenPart.lastFrameBuffer != null) {
            //this SHOULD work but it doesnt lol
            packets.add(getPacket(screenPart.mapId, screenPart.lastFrameBuffer));
          }
        }
        sendToPlayer(event.getPlayer(), packets);
        //todo: maybe remove from task list once we get here?
      }
    }.runTaskLater(plugin, 10);
    //MakiDesktop.tasks.add(task);
  }

  private void sendToPlayer(Player player, List<PacketPlayOutMap> packets) {
    CraftPlayer craftPlayer = (CraftPlayer) player;
    for (PacketPlayOutMap packet : packets) {
      if (packet != null) {
        craftPlayer.getHandle().networkManager.sendPacket(packet);
      }
    }
  }

  private PacketPlayOutMap getPacket(int mapId, byte[] data) {
    if (data == null) {
      throw new NullPointerException("data is null");
    }
    return new PacketPlayOutMap(
        mapId, (byte) 0, false, null,
        new b(0, 0, 128, 128, data));
  }
}
