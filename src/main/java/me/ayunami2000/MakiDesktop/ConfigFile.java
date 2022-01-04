package me.ayunami2000.MakiDesktop;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ConfigFile extends BukkitRunnable {
    private static int size=2;
    private static int delay=10000;
    private static String locstr="0,0,0,180";
    private static String locworld=Bukkit.getWorlds().get(0).getName();
    private static double[] locarr=new double[]{0,0,0,180};
    private static String ipport="127.0.0.1:5900";
    private static String audurl="";
    private static int mapSize=8;
    private static int mapWidth=4;
    private static int VCWidth=128*4;
    private static int VCHeight=128*2;
    private static FileConfiguration config;
    public Plugin plugin;

    //create config file if it doesn't exist
    public ConfigFile(@NotNull Plugin plugin) {
        this.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveDefaultConfig();
        }
        config = plugin.getConfig();
    }

    @Override
    public void run() {
        this.config = plugin.getConfig();
        if (config.contains("size")&&config.getInt("size") != 0&&config.getInt("size") <= 3) {
            ConfigSize(config.getInt("size"));
        } else {
            config.addDefault("size", 2);
        }
        if (config.contains("delay")&&config.getInt("delay") >= 0) {
            delay=config.getInt("delay");
        } else {
            config.addDefault("delay", 10000);
        }
        if (config.contains("ip")&&!config.getString("ip").isEmpty()) {
            ipport=config.getString("ip");
        } else {
            config.addDefault("ip", "127.0.0.1:5900");
        }
        if (config.contains("audio")) {
            audurl=config.getString("audio");
        } else {
            config.addDefault("audio", "");
        }
        if (config.contains("loc")&&!config.getString("loc").isEmpty()) {
            ConfigLoc(config.getString("loc"));
        } else {
            config.addDefault("loc", "0,0,0,180");
        }
        if (config.contains("locworld")&&!config.getString("locworld").isEmpty()) {
            locworld=config.getString("locworld");
        } else {
            config.addDefault("locworld",Bukkit.getWorlds().get(0).getName());
        }
    }

    private void ConfigSize(int sizee) {
        size=sizee;
        switch (sizee) {
            case 1 -> {
                mapSize = 2;
                mapWidth = 2;
                VCWidth = 128 * 2;
                VCHeight = 128;
            }
            case 2 -> {
                mapSize = 8;
                mapWidth = 4;
                VCWidth = 128 * 4;
                VCHeight = 128 * 2;
            }
            case 3 -> {
                mapSize = 32;
                mapWidth = 8;
                VCWidth = 128 * 8;
                VCHeight = 128 * 4;
            }
        }
    }

    private void ConfigLoc(String locstrr) {
        locstr=locstrr;
        String[] locspl=locstrr.split(",");
        if(locspl.length!=4)return;
        try{
            for (int i = 0; i < locspl.length; i++) {
                locarr[i]=Double.parseDouble(locspl[i]);
            }
        }catch(NumberFormatException e){
            locarr=new double[]{0,0,0,180};
        }
    }

    public static int getMapSize() {
        return mapSize;
    }

    public static int getMapWidth() {
        return mapWidth;
    }

    public static int getVCWidth() {
        return VCWidth;
    }

    public static int getVCHeight() {
        return VCHeight;
    }

    public static String getLocStr() {
        return locstr;
    }

    public static double[] getLocArr() {
        return locarr;
    }

    public static String getLocWorld() {
        return locworld;
    }

    public static String getIp() {
        return ipport;
    }

    public static String getAudio() {
        return audurl;
    }

    public static int getDelay() {
        return delay;
    }

    public static int getSize() {
        return size;
    }

    public static void setVal(String key, Object val){
        if(!key.equals("loc"))config.set(key,val);
        switch(key){
            case "size":
                size=(int)val;
                break;
            case "ip":
                ipport=(String)val;
                break;
            case "audio":
                audurl=(String)val;
                break;
            case "delay":
                delay=(int)val;
                break;
            case "loc":
                locarr=(double[])val;
                locstr="";
                for (double v : locarr) {
                    locstr+=v+",";
                }
                locstr=locstr.substring(0,locstr.length()-1);
                config.set(key,locstr);
                break;
            case "locworld":
                locworld=(String)val;
                break;
            default:
        }
    }
}