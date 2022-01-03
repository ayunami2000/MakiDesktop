package me.ayunami2000.MakiDesktop;

import org.bukkit.Location;
import org.bukkit.Utility;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public final class IntersectionUtils {
    private IntersectionUtils() {

    }

    @Utility
    public static Vector getIntersection(Location lineLocation, Block planeBlock, BlockFace planeBlockFace, double addToFace) {
        if (lineLocation == null) {
            throw new IllegalArgumentException("lineLocation cannot be null");
        }

        return getIntersection(lineLocation.toVector(), lineLocation.getDirection(), planeBlock, planeBlockFace, addToFace);
    }

    @Utility
    private static Vector getIntersection(Vector lineP, Vector lineU, Block planeBlock, BlockFace planeBlockFace, double addToFace) {
        if (planeBlock == null) {
            throw new IllegalArgumentException("planeBlock cannot be null");
        }

        Vector planeVector = planeBlock.getLocation().toVector();

        if (planeBlockFace == BlockFace.EAST) {
            planeVector.setX(planeVector.getX() + 1 + addToFace);
        } else if (planeBlockFace == BlockFace.UP) {
            planeVector.setY(planeVector.getY() + 1 + addToFace);
        } else if (planeBlockFace == BlockFace.SOUTH) {
            planeVector.setZ(planeVector.getZ() + 1 + addToFace);
        } else if (planeBlockFace == BlockFace.WEST) {
            planeVector.setX(planeVector.getX() - addToFace);
        } else if (planeBlockFace == BlockFace.DOWN) {
            planeVector.setY(planeVector.getY() - addToFace);
        } else if (planeBlockFace == BlockFace.NORTH) {
            planeVector.setZ(planeVector.getZ() - addToFace);
        }

        return getIntersection(lineP, lineU, planeVector, planeBlockFace);
    }

    @Utility
    private static Vector getIntersection(Vector lineP, Vector lineU, Vector planeVector, BlockFace planeBlockFace) {
        if (planeVector == null) {
            throw new IllegalArgumentException("planeVector cannot be null");
        }

        if (planeBlockFace == null) {
            throw new IllegalArgumentException("planeBlockFace cannot be null");
        }

        if (planeBlockFace != BlockFace.UP && planeBlockFace != BlockFace.DOWN && planeBlockFace != BlockFace.NORTH && planeBlockFace != BlockFace.SOUTH && planeBlockFace != BlockFace.WEST && planeBlockFace != BlockFace.EAST) {
            throw new IllegalArgumentException("planeBlockFace cannot be " + planeBlockFace.toString());
        }

        double planeA = 0;
        double planeB = 0;
        double planeC = 0;
        double planeD = 0;

        if (planeBlockFace == BlockFace.WEST || planeBlockFace == BlockFace.EAST) {
            planeA = 1;
            planeD = planeVector.getX();
        } else if (planeBlockFace == BlockFace.UP || planeBlockFace == BlockFace.DOWN) {
            planeB = 1;
            planeD = planeVector.getY();
        } else if (planeBlockFace == BlockFace.NORTH || planeBlockFace == BlockFace.SOUTH) {
            planeC = 1;
            planeD = planeVector.getZ();
        }

        return getIntersection(lineP, lineU, planeA, planeB, planeC, planeD);
    }

    @Utility
    private static Vector getIntersection(Vector lineP, Vector lineU, double planeA, double planeB, double planeC, double planeD) {
        if (lineP == null) {
            throw new IllegalArgumentException("lineP cannot be null");
        }

        if (lineU == null) {
            throw new IllegalArgumentException("lineU cannot be null");
        }

        double lineS = (planeD - planeA * lineP.getX() - planeB * lineP.getY() - planeC * lineP.getZ()) / (planeA * lineU.getX() + planeB * lineU.getY() + planeC * lineU.getZ());
        return new Vector(lineP.getX() + lineS * lineU.getX(), lineP.getY() + lineS * lineU.getY(), lineP.getZ() + lineS * lineU.getZ());
    }
}