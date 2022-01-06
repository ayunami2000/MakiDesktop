package me.ayunami2000.MakiDesktop;

import org.bukkit.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AudioPlayer implements Runnable {
    private static Sound[] sounds=new Sound[]{Sound.BLOCK_NOTE_BLOCK_HARP,Sound.BLOCK_NOTE_BLOCK_BASEDRUM,Sound.BLOCK_NOTE_BLOCK_SNARE,Sound.BLOCK_NOTE_BLOCK_HAT,Sound.BLOCK_NOTE_BLOCK_BASS,Sound.BLOCK_NOTE_BLOCK_FLUTE,Sound.BLOCK_NOTE_BLOCK_BELL,Sound.BLOCK_NOTE_BLOCK_GUITAR,Sound.BLOCK_NOTE_BLOCK_CHIME,Sound.BLOCK_NOTE_BLOCK_XYLOPHONE,Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE,Sound.BLOCK_NOTE_BLOCK_COW_BELL,Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO,Sound.BLOCK_NOTE_BLOCK_BIT,Sound.BLOCK_NOTE_BLOCK_BANJO,Sound.BLOCK_NOTE_BLOCK_PLING};
    private boolean enabled=false;
    private int requests=0;
    public void run(){
        if(ConfigFile.getAudio().equals("")){
            Thread.currentThread().stop();
            return;
        }
        if(requests>=10){
            //too many requests, pausing!
            MakiDesktop.paused=true;
            requests=0;
            return;
        }
        requests++;
        enabled=true;
        String currUrl=ConfigFile.getAudio();
        try {
            URL url = new URL(currUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line="";
            World scrWorld= Bukkit.getWorld(ConfigFile.getLocWorld());
            Location scrLoc=new Location(scrWorld,(MakiDesktop.loc.getX()+MakiDesktop.locEnd.getX())/2.0,(MakiDesktop.loc.getY()+MakiDesktop.locEnd.getY())/2.0,(MakiDesktop.loc.getZ()+MakiDesktop.locEnd.getZ())/2.0);
            while (currUrl.equals(ConfigFile.getAudio())&&enabled&&!MakiDesktop.paused&&(line = reader.readLine()) != null) {
                String[] audparts=line.split(",");
                if(audparts.length==4) {
                    Location localLoc=scrLoc.clone();
                    float panning=Float.parseFloat(audparts[3]);
                    if (MakiDesktop.locDir == 0) {
                        //south
                        localLoc.add(panning,0,0);
                    } else if (MakiDesktop.locDir == 1) {
                        //west
                        localLoc.add(0,0,panning);
                    } else if (MakiDesktop.locDir == 2) {
                        //north
                        localLoc.add(-panning,0,0);
                    } else if (MakiDesktop.locDir == 3) {
                        //east
                        localLoc.add(0,0,-panning);
                    }
                    scrWorld.playSound(localLoc, sounds[Integer.parseInt(audparts[0])], Float.parseFloat(audparts[1]), Float.parseFloat(audparts[2]));
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
    public void resetReqs(){
        requests=0;
    }
}
