package com.example.shopblock.storage;

import com.example.shopblock.ShopData;
import com.example.shopblock.ShopPosition;
import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ShopStorage {
    private final Path file;

    public ShopStorage(Path file) {
        this.file = file;
    }

    public Map<ShopPosition, ShopData> load(Server server) throws IOException, InvalidConfigurationException {
        Map<ShopPosition, ShopData> shops = new HashMap<>();
        if (!Files.exists(file)) {
            return shops;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.load(file.toFile());
        if (!configuration.isConfigurationSection("shops")) {
            return shops;
        }
        for (String worldKey : configuration.getConfigurationSection("shops").getKeys(false)) {
            UUID worldId = UUID.fromString(worldKey);
            if (server.getWorld(worldId) == null) {
                continue;
            }
            for (String positionKey : configuration.getConfigurationSection("shops." + worldKey).getKeys(false)) {
                String[] parts = positionKey.split(",");
                if (parts.length != 3) {
                    continue;
                }
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                ShopPosition position = new ShopPosition(worldId, x, y, z);
                String ownerId = configuration.getString("shops." + worldKey + "." + positionKey + ".owner");
                if (ownerId == null) {
                    continue;
                }
                ShopData data = new ShopData(UUID.fromString(ownerId), position);
                ItemStack product = configuration.getItemStack("shops." + worldKey + "." + positionKey + ".product");
                ItemStack price = configuration.getItemStack("shops." + worldKey + "." + positionKey + ".price");
                int stock = configuration.getInt("shops." + worldKey + "." + positionKey + ".stock", 0);
                data.setProduct(product);
                data.setPrice(price);
                data.setStock(stock);
                List<?> earnings = configuration.getList("shops." + worldKey + "." + positionKey + ".earnings");
                if (earnings != null) {
                    for (Object earning : earnings) {
                        if (earning instanceof ItemStack item) {
                            data.addEarning(item);
                        }
                    }
                }
                shops.put(position, data);
            }
        }
        return shops;
    }

    public void save(Map<ShopPosition, ShopData> shops) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<ShopPosition, ShopData> entry : shops.entrySet()) {
            ShopPosition position = entry.getKey();
            ShopData data = entry.getValue();
            String basePath = "shops." + position.getWorldId() + "." + position.toKey();
            configuration.set(basePath + ".owner", data.getOwner().toString());
            configuration.set(basePath + ".product", data.getProduct());
            configuration.set(basePath + ".price", data.getPrice());
            configuration.set(basePath + ".stock", data.getStock());
            configuration.set(basePath + ".earnings", data.getEarningsView());
        }
        Files.createDirectories(file.getParent());
        configuration.save(file.toFile());
    }
}
