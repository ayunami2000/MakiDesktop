package me.ayunami2000.MakiDesktop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public final class MakiDesktop extends JavaPlugin implements Listener {

    private final Logger logger = getLogger();

    public static final Set<ScreenPart> screens = new TreeSet<>(
        Comparator.comparingInt(to -> to.mapId));
    private static VideoCapture videoCapture;

    public static boolean paused = false;
    public static Location loc = null;
    public static Location locEnd = null;
    public static int locDir = 2;

    public static Player controller = null;
    public static boolean alwaysMoveMouse = true;

    public static AudioPlayer audioPlayer = new AudioPlayer();

    @Override
    public void onEnable() {
        ConfigFile configFile = new ConfigFile(this);
        configFile.run();

        ImageManager manager = ImageManager.getInstance();
        manager.init();

        logger.info("Enabling MakiDesktop!");

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(new ScreenClickEvent(), this);
        getServer().getPluginCommand("makikey").setTabCompleter(new TabCompletion());

        double[] locarr=ConfigFile.getLocArr();
        loc=new Location(getServer().getWorld(ConfigFile.getLocWorld()),locarr[0],locarr[1],locarr[2]);
        float yaw=(float)locarr[3];
        loc.setYaw(yaw);
        while(yaw < 0){yaw+=360;}
        yaw = yaw % 360;
        Vector tmpDir=new Vector(0,0,0);
        if(yaw < 45 || yaw >= 315){
            //south
            tmpDir=new Vector(0,0,1);
            locDir=0;
        }else if(yaw < 135){
            //west
            tmpDir=new Vector(-1,0,0);
            locDir=1;
        }else if(yaw < 225){
            //north
            tmpDir=new Vector(0,0,-1);
            locDir=2;
        }else if(yaw < 315){
            //east
            tmpDir=new Vector(1,0,0);
            locDir=3;
        }
        locEnd=loc.clone().add(tmpDir.rotateAroundY(-Math.PI/2.0).multiply(ConfigFile.getMapWidth()-1).add(new Vector(0,1-ConfigFile.getMapSize()/ConfigFile.getMapWidth(),0)));

        logger.info("Config file loaded \n"+
                "Size: " + ConfigFile.getSize() +"\n"+
                " - Map Size: " + ConfigFile.getMapSize() +"\n"+
                " - Map Width: " + ConfigFile.getMapWidth() +"\n"+
                " - Width: " + ConfigFile.getVCWidth() +"\n"+
                " - Height: " + ConfigFile.getVCHeight() +"\n"+
                "URL: <REDACTED FOR PRIVACY> \n"+
                "Delay: " + ConfigFile.getDelay() +"\n"+
                "Location: " + ConfigFile.getLocStr()
        );

        int mapSize = ConfigFile.getMapSize();
        int mapWidth = ConfigFile.getMapWidth();

        videoCapture = new VideoCapture(this,
                ConfigFile.getVCWidth(),
                ConfigFile.getVCHeight()
        );
        videoCapture.start();

        FrameProcessorTask frameProcessorTask = new FrameProcessorTask(mapSize, mapWidth);
        frameProcessorTask.runTaskTimerAsynchronously(this, 0, 1);
        FramePacketSender framePacketSender =
            new FramePacketSender(this, frameProcessorTask.getFrameBuffers());
        framePacketSender.runTaskTimerAsynchronously(this, 0, 1);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, (Runnable) () -> {
            audioPlayer.resetReqs();
        }, 0, 100);
    }

    @Override
    public void onDisable() {
        logger.info("Disabling MakiDesktop!");
        this.saveConfig();
        videoCapture.cleanup();
    }



    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        if(command.getName().equals("makitype")) {
            if(paused){
                sender.sendMessage("Error: MakiDesktop is currently paused!");
                return true;
            }
            if(!(sender instanceof ConsoleCommandSender||sender.isOp()||(Player)sender==controller)){
                sender.sendMessage("Error: You are not currently in control!");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage("Usage: /makitype <text>");
            } else {
                typeText(String.join(" ",args));
            }
            return true;
        }else if(command.getName().equals("makikey")){
            if(paused){
                sender.sendMessage("Error: MakiDesktop is currently paused!");
                return true;
            }
            if(!(sender instanceof ConsoleCommandSender||sender.isOp()||(Player)sender==controller)){
                sender.sendMessage("Error: You are not currently in control!");
                return true;
            }
            if(args.length==0){
                sender.sendMessage("Usage: /makikey <keyname>\n§lWarning: Case sensitive!");
            }else{
                for (String arg : args) {
                    pressKey(arg);
                }
            }
            return true;
        }else if (command.getName().equals("maki")) {
            if(args.length==0){
                sender.sendMessage("Usage: /maki [audio|ctrl|give|clear|toggle|loc|size|ip|delay]\n - audio: Set audio URL for audio support.\n - ctrl: Take control of MakiDesktop.\n - give: Generates new maps and gives them to you.\n - clear: Clears all map data.\n - toggle: Toggles map playback.\n - loc: Sets the top left corner of the screen to the block you are looking at.\n - size: Sets or gets the current size value.\n - ip: Sets or gets the current VNC ip:port.\n - delay: Sets or gets the current delay value.");
                return true;
            }
            if(args[0].equals("ctrl")){
                if(sender instanceof ConsoleCommandSender){
                    sender.sendMessage("Error: This command cannot be run from console!");
                }else {
                    Player player=(Player)sender;
                    if(player==controller){
                        controller=null;
                        sender.sendMessage("You are no longer controlling!");
                    }else{
                        alwaysMoveMouse=true;
                        controller = (Player) sender;
                        sender.sendMessage("You are now controlling!\n§lTo use:\n - Right click the screen to perform an action\n - Slots 1-3: Left, Middle, and Right mouse buttons (single clicks)\n - Slots 4-6: Left, Middle, and Right mouse buttons (toggle pressed state)\n - Slot 7: Toggle always move mouse\n - Slot 8: Move mouse to current position (even if always move mouse is disabled)\n - Slot 9: Do nothing\n - /makikey: Press a key or multiple keys.\n - /makitype: Type a string of text.");
                    }
                }
                return true;
            }
            if (!sender.isOp()) {
                sender.sendMessage("Error: You don't have permission!");
                return true;
            }

            switch(args[0]){
                case "audio":
                    if(args.length==1){
                        ConfigFile.setVal("audio","");
                        sender.sendMessage("Disabled audio!");
                    }else{
                        ConfigFile.setVal("audio",args[1]);
                        sender.sendMessage("Audio URL is now: "+args[1]);
                    }
                    break;
                case "give":
                    if(sender instanceof ConsoleCommandSender) {
                        sender.sendMessage("Error: This command cannot be run from console!");
                    }else{
                        Player player = (Player) sender;
                        for (int i=0; i<ConfigFile.getMapSize(); i++) {
                            MapView mapView = getServer().createMap(player.getWorld());
                            mapView.setScale(MapView.Scale.CLOSEST);
                            mapView.setUnlimitedTracking(true);
                            for (MapRenderer renderer : mapView.getRenderers()) {
                                mapView.removeRenderer(renderer);
                            }

                            ItemStack itemStack = new ItemStack(Material.FILLED_MAP);

                            MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
                            mapMeta.setMapView(mapView);

                            itemStack.setItemMeta(mapMeta);
                            player.getInventory().addItem(itemStack);
                            screens.add(new ScreenPart(mapView.getId(), i));
                            ImageManager manager = ImageManager.getInstance();
                            manager.saveImage(mapView.getId(), i);
                        }
                    }
                    break;
                case "clear":
                    try{
                        new PrintWriter(new File(this.getDataFolder(), "data.yml")).close();
                        sender.sendMessage("All maps have been reset!");
                        sender.sendMessage("§lNote: The current maps will not change until the server is restarted, and using the give command will restore map data!");
                    } catch (FileNotFoundException e) {
                        sender.sendMessage("Error: No data file was found, so nothing happened.");
                    }
                    break;
                case "toggle":
                    paused=!paused;
                    sender.sendMessage("MakiDesktop is now "+(paused?"":"un")+"paused.");
                    break;
                case "loc":
                    if(sender instanceof ConsoleCommandSender) {
                        sender.sendMessage("Error: This command cannot be run from console!");
                    }else {
                        Player player = (Player) sender;
                        Block tblock=player.getTargetBlock(5);
                        Location tmploc=tblock.getLocation();
                        Vector dir=new Vector(0,0,0);
                        tmploc.setYaw(player.getLocation().getYaw());
                        float yaw=tmploc.getYaw();
                        while(yaw < 0){yaw+=360;}
                        yaw = yaw % 360;
                        if(yaw < 45 || yaw >= 315){
                            //south
                            dir=new Vector(0,0,1);
                            locDir=0;
                        }else if(yaw < 135){
                            //west
                            dir=new Vector(-1,0,0);
                            locDir=1;
                        }else if(yaw < 225){
                            //north
                            dir=new Vector(0,0,-1);
                            locDir=2;
                        }else if(yaw < 315){
                            //east
                            dir=new Vector(1,0,0);
                            locDir=3;
                        }
                        loc=tmploc;
                        locEnd=loc.clone().add(dir.rotateAroundY(-Math.PI/2.0).multiply(ConfigFile.getMapWidth()-1).add(new Vector(0,1-ConfigFile.getMapSize()/ConfigFile.getMapWidth(),0)));
                        ConfigFile.setVal("loc",new double[]{loc.getX(),loc.getY(),loc.getZ(),loc.getYaw()});
                        ConfigFile.setVal("locworld",loc.getWorld().getName());
                        sender.sendMessage("Set screen location to "+loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ()+","+loc.getYaw());
                    }
                    break;
                case "size":
                    if(args.length==1) {
                        sender.sendMessage("Current size value is: " + ConfigFile.getSize());
                    }else{
                        try{
                            int size=Integer.parseInt(args[1]);
                            size=Math.min(3,Math.max(1,size));
                            ConfigFile.setVal("size",size);
                            sender.sendMessage("Size value now set to: "+size);
                            sender.sendMessage("§lNote: The size will not change until the server is restarted!");
                        }catch(NumberFormatException e){
                            sender.sendMessage("Error: Invalid command arguments!");
                        }
                    }
                    break;
                case "ip":
                    if(args.length==1) {
                        sender.sendMessage("Current IP:PORT is: " + ConfigFile.getIp());
                    }else{
                        ConfigFile.setVal("ip",args[1]);
                        sender.sendMessage("IP:PORT is now: "+args[1]);
                    }
                    break;
                case "delay":
                    if(args.length==1) {
                        sender.sendMessage("Current delay value is: " + ConfigFile.getDelay());
                    }else{
                        try{
                            int delay=Integer.parseInt(args[1]);
                            delay=Math.max(0,delay);
                            ConfigFile.setVal("delay",delay);
                            sender.sendMessage("Delay value now set to: "+delay);
                        }catch(NumberFormatException e){
                            sender.sendMessage("Error: Invalid command arguments!");
                        }
                    }
                    break;
                default:
                    sender.sendMessage("Error: Invalid command arguments!");
            }
        }

        return true;
    }

    public static void clickMouse(double x, double y,int doClick,boolean drag){
        videoCapture.clickMouse(x,y,doClick,drag);
    }

    public void pressKey(String key){
        videoCapture.pressKey(key);
    }

    public void typeText(String text){
        videoCapture.typeText(text);
    }
}
