package com.example.shopblock.gui;

import com.example.shopblock.ShopBlockPlugin;
import com.example.shopblock.ShopData;
import com.example.shopblock.util.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.List;

public final class ShopBuyMenu implements InventoryHolder {
    private static final int PRODUCT_SLOT = 11;
    private static final int PRICE_SLOT = 15;
    private static final int INFO_SLOT = 13;
    private static final int CONFIRM_SLOT = 22;
    private static final int CANCEL_SLOT = 24;

    private final ShopBlockPlugin plugin;
    private final ShopData shop;
    private final Inventory inventory;

    public ShopBuyMenu(ShopBlockPlugin plugin, ShopData shop) {
        this.plugin = plugin;
        this.shop = shop;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_AQUA + "Shop Offer");
        refresh();
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void refresh() {
        inventory.clear();
        ItemStack pane = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, pane.clone());
        }
        inventory.setItem(PRODUCT_SLOT, createDisplayItem(shop.getProduct(), ChatColor.GREEN + "Product"));
        inventory.setItem(PRICE_SLOT, createDisplayItem(shop.getPrice(), ChatColor.GOLD + "Price"));
        inventory.setItem(INFO_SLOT, createInfoItem());
        inventory.setItem(CONFIRM_SLOT, createConfirmButton());
        inventory.setItem(CANCEL_SLOT, createCancelButton());
    }

    private ItemStack createDisplayItem(ItemStack template, String title) {
        if (template == null) {
            ItemStack barrier = new ItemStack(Material.BARRIER);
            ItemMeta meta = barrier.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "Not available");
                barrier.setItemMeta(meta);
            }
            return barrier;
        }
        ItemStack display = template.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(title);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack createInfoItem() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Details");
            meta.setLore(List.of(
                    ChatColor.YELLOW + "Stock left: " + shop.getStock(),
                    ChatColor.GRAY + "Shift-click items won't work here.",
                    ChatColor.GRAY + "Click green concrete to confirm."
            ));
            paper.setItemMeta(meta);
        }
        return paper;
    }

    private ItemStack createConfirmButton() {
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = confirm.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Confirm Purchase");
            confirm.setItemMeta(meta);
        }
        return confirm;
    }

    private ItemStack createCancelButton() {
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta meta = cancel.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Cancel");
            cancel.setItemMeta(meta);
        }
        return cancel;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!event.getView().getTopInventory().equals(inventory)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot >= inventory.getSize()) {
            return;
        }
        if (rawSlot == CONFIRM_SLOT) {
            attemptPurchase(player);
        } else if (rawSlot == CANCEL_SLOT) {
            player.closeInventory();
        }
    }

    private void attemptPurchase(Player player) {
        if (!shop.isConfigured()) {
            player.sendMessage(ChatColor.RED + "This shop is not configured.");
            player.closeInventory();
            return;
        }
        if (shop.getStock() <= 0) {
            player.sendMessage(ChatColor.RED + "This shop is out of stock.");
            player.closeInventory();
            return;
        }
        ItemStack price = shop.getPrice();
        if (!InventoryUtils.hasRequiredItems(player, price)) {
            player.sendMessage(ChatColor.RED + "You do not have the required items.");
            return;
        }
        InventoryUtils.removeItems(player, price);
        ItemStack product = shop.getProduct();
        InventoryUtils.giveOrDrop(player, product.clone());
        shop.decrementStock();
        shop.addEarning(price.clone());
        try {
            plugin.getShopManager().save();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save shop purchase: " + e.getMessage());
        }
        player.sendMessage(ChatColor.GREEN + "Purchase successful!");
        if (shop.getStock() <= 0) {
            player.sendMessage(ChatColor.RED + "The shop is now out of stock.");
            player.closeInventory();
        } else {
            refresh();
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        // no-op, but method kept for interface symmetry
    }
}
