package com.example.shopblock.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

public final class InventoryUtils {
    private InventoryUtils() {
    }

    public static boolean hasRequiredItems(Player player, ItemStack template) {
        if (template == null) {
            return false;
        }
        int required = template.getAmount();
        PlayerInventory inventory = player.getInventory();
        int remaining = required;
        for (ItemStack item : inventory.getContents()) {
            if (item == null) {
                continue;
            }
            if (item.isSimilar(template)) {
                remaining -= item.getAmount();
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return remaining <= 0;
    }

    public static void removeItems(Player player, ItemStack template) {
        int toRemove = template.getAmount();
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null) {
                continue;
            }
            if (!item.isSimilar(template)) {
                continue;
            }
            if (item.getAmount() > toRemove) {
                item.setAmount(item.getAmount() - toRemove);
                inventory.setItem(i, item);
                return;
            } else {
                toRemove -= item.getAmount();
                inventory.setItem(i, null);
                if (toRemove <= 0) {
                    return;
                }
            }
        }
    }

    public static void giveOrDrop(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        if (!leftovers.isEmpty()) {
            Location location = player.getLocation();
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(location, item));
        }
    }
}
