package me.ayunami2000.MakiDesktop;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ClickOnScreen {
    static BlockFace[] numberToBlockFace=new BlockFace[]{BlockFace.SOUTH,BlockFace.WEST,BlockFace.NORTH,BlockFace.EAST};
    private static boolean numBetween(double num, double val1, double val2){
        double maxVal=Math.max(val1,val2);
        double minVal=Math.min(val1,val2);
        return num>=minVal&&num<=maxVal;
    }
    public static boolean clickedOnBlock(Block block, Player player, boolean doClick){
        if (numBetween(block.getX(),MakiDesktop.loc.getX(),MakiDesktop.locEnd.getX())&&numBetween(block.getY(),MakiDesktop.loc.getY(),MakiDesktop.locEnd.getY())&&numBetween(block.getZ(),MakiDesktop.loc.getZ(),MakiDesktop.locEnd.getZ())) {
            Location plyrloc=player.getLocation();
            float yaw=plyrloc.getYaw();
            while(yaw < 0){yaw+=360;}
            yaw = yaw % 360;
            if((MakiDesktop.locDir==0&&(yaw < 90 || yaw >= 270))||(MakiDesktop.locDir==1&&(yaw >= 0 && yaw < 180))||(MakiDesktop.locDir==2&&(yaw >= 90 && yaw < 270))||(MakiDesktop.locDir==3&&(yaw >= 180 && yaw < 360))){
                //looking at screen from the correct angle
                BlockFace blockFace = player.getTargetBlockFace(5);
                if(blockFace==numberToBlockFace[(MakiDesktop.locDir+2)%4]) {
                    //correct block face
                    Vector exactLoc = IntersectionUtils.getIntersection(player.getEyeLocation(), block, blockFace, 0);
                    double y = 1.0-(exactLoc.getY() - MakiDesktop.locEnd.getY()) / ((double) (ConfigFile.getMapSize() / ConfigFile.getMapWidth()));
                    double x = 0;
                    if (MakiDesktop.locDir == 0) {
                        //south -
                        x = exactLoc.getX() - MakiDesktop.locEnd.getX();
                    } else if (MakiDesktop.locDir == 1) {
                        //west -
                        x = exactLoc.getZ() - MakiDesktop.locEnd.getZ();
                    } else if (MakiDesktop.locDir == 2) {
                        //north +
                        x = exactLoc.getX() - MakiDesktop.loc.getX();
                    } else if (MakiDesktop.locDir == 3) {
                        //east +
                        x = exactLoc.getZ() - MakiDesktop.loc.getZ();
                    }
                    x = x / (double) ConfigFile.getMapWidth();
                    if (MakiDesktop.locDir == 0 || MakiDesktop.locDir == 1) {
                        x = 1.0-x;
                    }

                    y = Math.max(0, Math.min(1, y));
                    x = Math.max(0, Math.min(1, x));
                    int slot=player.getInventory().getHeldItemSlot();
                    if(!doClick){
                        MakiDesktop.clickMouse(x, y, 0, false);
                    }else if(slot==6) {
                        MakiDesktop.alwaysMoveMouse = !MakiDesktop.alwaysMoveMouse;
                        player.sendMessage("No" + (MakiDesktop.alwaysMoveMouse ? "w" : " longer") + " controlling mouse.");
                    }else if(slot==7) {
                        MakiDesktop.clickMouse(x, y, 0, false);
                    }else if(slot==8){
                        //do nothing
                    }else {
                        MakiDesktop.clickMouse(x, y, (slot % 3) + 1, slot==3||slot==4||slot==5);
                    }
                    //player.sendMessage(x+","+y);
                    return true;
                }
            }
        }
        return false;
    }
}
