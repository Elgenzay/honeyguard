package gg.elg.honeyGuard;

import org.bukkit.Location;

import java.util.Objects;

public class Coordinate {
    private final int x, y, z;

    private Coordinate(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    int getZ() {
        return z;
    }

    static Coordinate fromLocation(Location location){
        return new Coordinate(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinate that = (Coordinate) o;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    static int calculateFastDistance(Coordinate one, Coordinate two){
        int xDistance = Math.abs(one.getX() - two.getX());
        int zDistance = Math.abs(one.getZ() - two.getZ());
        return xDistance + zDistance;
    }
}