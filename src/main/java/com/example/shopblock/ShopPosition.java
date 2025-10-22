package com.example.shopblock;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;
import java.util.UUID;

public final class ShopPosition {
    private final UUID worldId;
    private final int x;
    private final int y;
    private final int z;

    public ShopPosition(UUID worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static ShopPosition fromBlock(Block block) {
        World world = block.getWorld();
        return new ShopPosition(world.getUID(), block.getX(), block.getY(), block.getZ());
    }

    public UUID getWorldId() {
        return worldId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShopPosition that = (ShopPosition) o;
        return x == that.x && y == that.y && z == that.z && worldId.equals(that.worldId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldId, x, y, z);
    }

    public String toKey() {
        return x + "," + y + "," + z;
    }
}
