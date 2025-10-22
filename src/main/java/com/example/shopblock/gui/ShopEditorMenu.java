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

public final class ShopEditorMenu implements InventoryHolder {
    private static final int PRODUCT_SLOT = 10;
    private static final int STOCK_SLOT = 13;
    private static final int PRICE_SLOT = 16;
    private static final int INFO_SLOT = 4;
    private static final int STATUS_SLOT = 22;
    private static final int WITHDRAW_SLOT = 24;
    private static final int CLOSE_SLOT = 26;

    private final ShopBlockPlugin plugin;
    private final ShopData shop;
    private final Inventory inventory;

    public ShopEditorMenu(ShopBlockPlugin plugin, ShopData shop) {
        this.plugin = plugin;
        this.shop = shop;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GREEN + "Shop Editor");
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
        fillBackground();
        inventory.setItem(INFO_SLOT, createStatusItem());
        inventory.setItem(STATUS_SLOT, createConfirmItem());
        inventory.setItem(WITHDRAW_SLOT, createWithdrawItem());
        inventory.setItem(CLOSE_SLOT, createCloseItem());
    }

    private void fillBackground() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, pane.clone());
        }
        inventory.setItem(PRODUCT_SLOT, null);
        inventory.setItem(STOCK_SLOT, null);
        inventory.setItem(PRICE_SLOT, null);
    }

    private ItemStack createStatusItem() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Shop Status");
            String productLine = shop.getProduct() == null
                    ? ChatColor.RED + "Product: not set"
                    : ChatColor.GREEN + "Product: " + describeItem(shop.getProduct());
            String priceLine = shop.getPrice() == null
                    ? ChatColor.RED + "Price: not set"
                    : ChatColor.GOLD + "Price: " + describeItem(shop.getPrice());
            meta.setLore(List.of(
                    ChatColor.YELLOW + "Stock: " + shop.getStock(),
                    productLine,
                    priceLine,
                    ChatColor.GRAY + "Place sample items in the highlighted slots.",
                    ChatColor.GRAY + "Deposit product stacks to increase stock."
            ));
            paper.setItemMeta(meta);
        }
        return paper;
    }

    private ItemStack createConfirmItem() {
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = confirm.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Save Settings");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Place the item you are selling in the left slot.",
                    ChatColor.GRAY + "Put the payment stack in the right slot.",
                    ChatColor.GRAY + "Deposit extra product in the middle to add stock."
            ));
            confirm.setItemMeta(meta);
        }
        return confirm;
    }

    private ItemStack createWithdrawItem() {
        ItemStack withdraw = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = withdraw.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Withdraw Earnings");
            int totalItems = shop.getEarningsView().stream().mapToInt(ItemStack::getAmount).sum();
            meta.setLore(List.of(
                    totalItems == 0 ? ChatColor.GRAY + "No pending payments." : ChatColor.YELLOW + "Items waiting: " + totalItems,
                    ChatColor.GRAY + "Click to move earnings to your inventory."
            ));
            withdraw.setItemMeta(meta);
        }
        return withdraw;
    }

    private ItemStack createCloseItem() {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Close");
            barrier.setItemMeta(meta);
        }
        return barrier;
    }

    private String describeItem(ItemStack stack) {
        ItemStack clone = stack.clone();
        return clone.getAmount() + "x " + clone.getType().name().toLowerCase().replace('_', ' ');
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        if (!event.getView().getTopInventory().equals(inventory)) {
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot >= inventory.getSize()) {
            return;
        }
        if (rawSlot == PRODUCT_SLOT || rawSlot == PRICE_SLOT || rawSlot == STOCK_SLOT) {
            event.setCancelled(false);
            return;
        }
        event.setCancelled(true);
        if (rawSlot == STATUS_SLOT) {
            saveChanges(player);
        } else if (rawSlot == WITHDRAW_SLOT) {
            withdrawEarnings(player);
        } else if (rawSlot == CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    private void saveChanges(Player player) {
        ItemStack productInput = inventory.getItem(PRODUCT_SLOT);
        ItemStack priceInput = inventory.getItem(PRICE_SLOT);
        ItemStack deposit = inventory.getItem(STOCK_SLOT);

        ItemStack productTemplate = productInput != null ? productInput.clone() : shop.getProduct();
        ItemStack priceTemplate = priceInput != null ? priceInput.clone() : shop.getPrice();

        if (productTemplate == null || priceTemplate == null) {
            player.sendMessage(ChatColor.RED + "You must specify both product and price.");
            return;
        }

        if (productInput != null) {
            inventory.setItem(PRODUCT_SLOT, null);
        }
        if (priceInput != null) {
            inventory.setItem(PRICE_SLOT, null);
        }

        ItemStack previousProduct = shop.getProduct();
        int previousStock = shop.getStock();
        boolean productReplaced = productInput != null;
        boolean productChanged = productReplaced && (previousProduct == null
                || !previousProduct.isSimilar(productTemplate)
                || previousProduct.getAmount() != productTemplate.getAmount());

        if (productChanged && previousProduct != null && previousStock > 0) {
            ItemStack dropTemplate = previousProduct.clone();
            int totalItems = previousStock * dropTemplate.getAmount();
            int maxStack = dropTemplate.getMaxStackSize();
            while (totalItems > 0) {
                ItemStack refund = dropTemplate.clone();
                int give = Math.min(maxStack, totalItems);
                refund.setAmount(give);
                InventoryUtils.giveOrDrop(player, refund);
                totalItems -= give;
            }
            shop.setStock(0);
        }

        int addedStock = 0;
        if (deposit != null && deposit.getType() != Material.AIR) {
            if (!deposit.isSimilar(productTemplate)) {
                player.sendMessage(ChatColor.RED + "Stock items must match the product sample.");
                return;
            }
            int templateAmount = Math.max(1, productTemplate.getAmount());
            int depositAmount = deposit.getAmount();
            if (depositAmount < templateAmount) {
                player.sendMessage(ChatColor.RED + "Deposit at least " + templateAmount + " items for one sale.");
                return;
            }
            addedStock = depositAmount / templateAmount;
            int remainder = depositAmount % templateAmount;
            inventory.setItem(STOCK_SLOT, null);
            if (remainder > 0) {
                ItemStack leftover = deposit.clone();
                leftover.setAmount(remainder);
                InventoryUtils.giveOrDrop(player, leftover);
            }
        }

        if (productReplaced) {
            shop.setProduct(productTemplate);
        } else if (shop.getProduct() == null) {
            shop.setProduct(productTemplate);
        }

        if (priceInput != null) {
            shop.setPrice(priceTemplate);
        } else if (shop.getPrice() == null) {
            shop.setPrice(priceTemplate);
        }

        if (addedStock > 0) {
            shop.addStock(addedStock);
        }

        try {
            plugin.getShopManager().save();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save shop configuration: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while saving the shop.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Shop updated. Remaining stock: " + shop.getStock());
        refresh();
        player.closeInventory();
    }

    private void withdrawEarnings(Player player) {
        List<ItemStack> earnings = shop.drainEarnings();
        if (earnings.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "There are no earnings to withdraw.");
            refresh();
            return;
        }
        int totalItems = earnings.stream().mapToInt(ItemStack::getAmount).sum();
        for (ItemStack earning : earnings) {
            InventoryUtils.giveOrDrop(player, earning);
        }
        try {
            plugin.getShopManager().save();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save shop after withdrawal: " + e.getMessage());
        }
        player.sendMessage(ChatColor.GOLD + "Collected " + totalItems + " items of earnings.");
        refresh();
    }

    public void handleClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        returnSlotItem(player, PRODUCT_SLOT);
        returnSlotItem(player, PRICE_SLOT);
        returnSlotItem(player, STOCK_SLOT);
    }

    private void returnSlotItem(Player player, int slot) {
        ItemStack stack = inventory.getItem(slot);
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        inventory.setItem(slot, null);
        InventoryUtils.giveOrDrop(player, stack);
    }
}
