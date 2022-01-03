package me.ayunami2000.MakiDesktop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AudioPlayer implements Runnable {
    private static Sound[] sounds=new Sound[]{Sound.BLOCK_NOTE_BLOCK_HARP,Sound.BLOCK_NOTE_BLOCK_BASEDRUM,Sound.BLOCK_NOTE_BLOCK_SNARE,Sound.BLOCK_NOTE_BLOCK_HAT,Sound.BLOCK_NOTE_BLOCK_BASS,Sound.BLOCK_NOTE_BLOCK_FLUTE,Sound.BLOCK_NOTE_BLOCK_BELL,Sound.BLOCK_NOTE_BLOCK_GUITAR,Sound.BLOCK_NOTE_BLOCK_CHIME,Sound.BLOCK_NOTE_BLOCK_XYLOPHONE,Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE,Sound.BLOCK_NOTE_BLOCK_COW_BELL,Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO,Sound.BLOCK_NOTE_BLOCK_BIT,Sound.BLOCK_NOTE_BLOCK_BANJO,Sound.BLOCK_NOTE_BLOCK_PLING};
    private boolean enabled=false;
    public void run(){
        if(MakiDesktop.audioUrl.equals("")){
            Thread.currentThread().stop();
            return;
        }
        enabled=true;
        String currUrl=MakiDesktop.audioUrl;
        try {
            URL url = new URL(currUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line="";
            World scrWorld= Bukkit.getWorld(ConfigFile.getLocWorld());
            Location scrLoc=new Location(scrWorld,(MakiDesktop.loc.getX()+MakiDesktop.locEnd.getX())/2.0,(MakiDesktop.loc.getY()+MakiDesktop.locEnd.getY())/2.0,(MakiDesktop.loc.getZ()+MakiDesktop.locEnd.getZ())/2.0);
            while (currUrl.equals(MakiDesktop.audioUrl)&&enabled&&!MakiDesktop.paused&&(line = reader.readLine()) != null) {
                String[] audpartss=line.trim().split(";");
                for (String audpart : audpartss) {
                    String[] audparts=audpart.split(",");
                    if(audparts.length==3) {
                        scrWorld.playSound(scrLoc, sounds[Integer.parseInt(audparts[0])], Float.parseFloat(audparts[1]), Float.parseFloat(audparts[2]));
                    }
                }
            }
            urlConnection.disconnect();
            enabled=false;
        } catch (IOException e) {
            enabled=false;
        }
        Thread.currentThread().stop();
    }
    public void stopIt(){
        enabled=false;
    }
    public boolean isEnabled(){
        return enabled;
    }
}
