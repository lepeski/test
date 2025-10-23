package com.example.shopblock;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ShopData {
    private final UUID owner;
    private final ShopPosition position;
    private ItemStack product;
    private ItemStack price;
    private int stock;
    private final List<ItemStack> earnings = new ArrayList<>();

    public ShopData(UUID owner, ShopPosition position) {
        this.owner = owner;
        this.position = position;
    }

    public UUID getOwner() {
        return owner;
    }

    public ShopPosition getPosition() {
        return position;
    }

    public ItemStack getProduct() {
        return product == null ? null : product.clone();
    }

    public void setProduct(ItemStack product) {
        this.product = product == null ? null : product.clone();
    }

    public ItemStack getPrice() {
        return price == null ? null : price.clone();
    }

    public void setPrice(ItemStack price) {
        this.price = price == null ? null : price.clone();
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = Math.max(0, stock);
    }

    public void addStock(int additional) {
        if (additional <= 0) {
            return;
        }
        this.stock += additional;
    }

    public boolean isConfigured() {
        return product != null && price != null;
    }

    public void decrementStock() {
        if (stock > 0) {
            stock--;
        }
    }

    public void addEarning(ItemStack payment) {
        if (payment == null || payment.getAmount() <= 0) {
            return;
        }
        ItemStack remaining = payment.clone();
        for (ItemStack stored : earnings) {
            if (stored.isSimilar(remaining)) {
                int max = stored.getMaxStackSize();
                int total = stored.getAmount() + remaining.getAmount();
                int applied = Math.min(max, total);
                stored.setAmount(applied);
                remaining.setAmount(total - applied);
                if (remaining.getAmount() <= 0) {
                    return;
                }
            }
        }
        while (remaining.getAmount() > 0) {
            ItemStack extra = remaining.clone();
            int max = extra.getMaxStackSize();
            int take = Math.min(max, remaining.getAmount());
            extra.setAmount(take);
            earnings.add(extra);
            remaining.setAmount(remaining.getAmount() - take);
        }
    }

    public List<ItemStack> drainEarnings() {
        List<ItemStack> clone = new ArrayList<>();
        for (ItemStack item : earnings) {
            clone.add(item.clone());
        }
        earnings.clear();
        return clone;
    }

    public List<ItemStack> getEarningsView() {
        List<ItemStack> clone = new ArrayList<>();
        for (ItemStack item : earnings) {
            clone.add(item.clone());
        }
        return clone;
    }

    public void dropContents(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        if (product != null && stock > 0) {
            ItemStack template = product.clone();
            for (int i = 0; i < stock; i++) {
                world.dropItemNaturally(location, template.clone());
            }
        }
        for (ItemStack earning : earnings) {
            world.dropItemNaturally(location, earning.clone());
        }
        stock = 0;
        earnings.clear();
    }
}
