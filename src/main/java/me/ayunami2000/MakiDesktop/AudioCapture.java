package me.ayunami2000.MakiDesktop;

import me.ayunami2000.MakiDesktop.AudioParser.Analysis.Analysis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;

public class AudioCapture {
    private static Sound[] sounds=new Sound[]{Sound.BLOCK_NOTE_BLOCK_HARP,Sound.BLOCK_NOTE_BLOCK_BASEDRUM,Sound.BLOCK_NOTE_BLOCK_SNARE,Sound.BLOCK_NOTE_BLOCK_HAT,Sound.BLOCK_NOTE_BLOCK_BASS,Sound.BLOCK_NOTE_BLOCK_FLUTE,Sound.BLOCK_NOTE_BLOCK_BELL,Sound.BLOCK_NOTE_BLOCK_GUITAR,Sound.BLOCK_NOTE_BLOCK_CHIME,Sound.BLOCK_NOTE_BLOCK_XYLOPHONE,Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE,Sound.BLOCK_NOTE_BLOCK_COW_BELL,Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO,Sound.BLOCK_NOTE_BLOCK_BIT,Sound.BLOCK_NOTE_BLOCK_BANJO,Sound.BLOCK_NOTE_BLOCK_PLING};

    public static void pitchDetect(Analysis analysis){
        if(analysis.maximum<1)return;
        double volumeToUsable = 12.0 * (Math.log(analysis.maximum / 440.0f) / Math.log(2)) + 69.0;
        for (Double f0 : analysis.klapuri.f0s) {
            //440 Hz as the pitch of A4
            double pitchToMidi = 12.0 * (Math.log(f0 / 440.0f) / Math.log(2)) + 69.0;
            int[] midiToNote = MidiConverter.noteConv(0,(int)pitchToMidi);
            int noteToGame = (midiToNote[1]-MidiConverter.instrument_offsets[midiToNote[0]]) + midiToNote[0]*25;
            World scrWorld=Bukkit.getWorld(ConfigFile.getLocWorld());
            scrWorld.playSound(new Location(scrWorld,(MakiDesktop.loc.getX()+MakiDesktop.locEnd.getX())/2.0,(MakiDesktop.loc.getY()+MakiDesktop.locEnd.getY())/2.0,(MakiDesktop.loc.getZ()+MakiDesktop.locEnd.getZ())/2.0),sounds[(int)Math.floor(noteToGame / 25)], (float) (volumeToUsable/127.0), (float) (.5*(Math.pow(2,((double)(noteToGame%25))/12.0))));
        }
    }
}
