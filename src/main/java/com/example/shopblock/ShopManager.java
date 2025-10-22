package com.example.shopblock;

import com.example.shopblock.storage.ShopStorage;
import org.bukkit.block.Block;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ShopManager {
    private final ShopBlockPlugin plugin;
    private final ShopStorage storage;
    private final Map<ShopPosition, ShopData> shops = new HashMap<>();

    public ShopManager(ShopBlockPlugin plugin, ShopStorage storage) throws IOException {
        this.plugin = plugin;
        this.storage = storage;
        this.shops.putAll(storage.load(plugin.getServer()));
    }

    public Optional<ShopData> getShop(Block block) {
        return Optional.ofNullable(shops.get(ShopPosition.fromBlock(block)));
    }

    public ShopData createShop(Block block, UUID ownerId) {
        ShopPosition position = ShopPosition.fromBlock(block);
        ShopData data = new ShopData(ownerId, position);
        shops.put(position, data);
        return data;
    }

    public void removeShop(Block block) {
        shops.remove(ShopPosition.fromBlock(block));
    }

    public void removeShop(ShopPosition position) {
        shops.remove(position);
    }

    public Map<ShopPosition, ShopData> getShops() {
        return shops;
    }

    public void save() throws IOException {
        storage.save(shops);
    }
}
