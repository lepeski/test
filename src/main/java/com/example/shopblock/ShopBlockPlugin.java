package com.example.shopblock;

import com.example.shopblock.gui.ShopBuyMenu;
import com.example.shopblock.gui.ShopEditorMenu;
import com.example.shopblock.storage.ShopStorage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ShopBlockPlugin extends JavaPlugin implements Listener {
    private NamespacedKey shopBlockKey;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Failed to create plugin data folder.");
        }
        this.shopBlockKey = new NamespacedKey(this, "shop-block");
        ShopStorage storage = new ShopStorage(getDataFolder().toPath().resolve("shops.yml"));
        this.shopManager = new ShopManager(this, storage);
        try {
            this.shopManager.load();
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().severe("Failed to load shop data: " + e.getMessage());
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (shopManager != null) {
            try {
                shopManager.save();
            } catch (IOException e) {
                getLogger().severe("Failed to save shop data: " + e.getMessage());
            }
        }
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public NamespacedKey getShopBlockKey() {
        return shopBlockKey;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"shopblock".equalsIgnoreCase(command.getName())) {
            return false;
        }

        if (!sender.hasPermission("shopblock.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /shopblock give [player]");
            return true;
        }

        if ("give".equalsIgnoreCase(args[0])) {
            Player target;
            if (args.length >= 2) {
                target = getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return true;
                }
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "You must specify a player when using this command from console.");
                    return true;
                }
                target = (Player) sender;
            }

            ItemStack shopItem = createShopItem();
            target.getInventory().addItem(shopItem);
            sender.sendMessage(ChatColor.GREEN + "Gave a shop block to " + target.getName() + ".");
            if (!Objects.equals(sender, target)) {
                target.sendMessage(ChatColor.GREEN + "You received a shop block. Place it to set up your trades.");
            }
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Unknown subcommand. Usage: /shopblock give [player]");
        return true;
    }

    private ItemStack createShopItem() {
        ItemStack item = new ItemStack(Material.CHISELED_BOOKSHELF);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Shop Block");
            meta.setLore(java.util.List.of(ChatColor.GRAY + "Place to create a player shop."));
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(shopBlockKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isShopItem(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte value = meta.getPersistentDataContainer().get(shopBlockKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isShopItem(event.getItemInHand())) {
            return;
        }

        Block block = event.getBlockPlaced();
        UUID ownerId = event.getPlayer().getUniqueId();
        if (shopManager.getShop(block).isPresent()) {
            event.getPlayer().sendMessage(ChatColor.RED + "This block is already registered as a shop.");
            return;
        }

        shopManager.createShop(block, ownerId);
        event.getPlayer().sendMessage(ChatColor.GREEN + "Shop created. Sneak-right-click to configure it.");
        getServer().getScheduler().runTaskLater(this, () -> {
            try {
                shopManager.save();
            } catch (IOException e) {
                getLogger().severe("Failed to save shop data after placement: " + e.getMessage());
            }
        }, 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Optional<ShopData> optionalShop = shopManager.getShop(block);
        if (optionalShop.isEmpty()) {
            return;
        }

        ShopData shop = optionalShop.get();
        Player player = event.getPlayer();
        boolean isOwner = shop.getOwner().equals(player.getUniqueId());
        if (!isOwner && !player.hasPermission("shopblock.admin.break")) {
            player.sendMessage(ChatColor.RED + "Only the owner can break this shop block.");
            event.setCancelled(true);
            return;
        }

        event.setDropItems(false);
        shopManager.removeShop(block);
        player.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), createShopItem());
        shop.dropContents(block.getLocation());
        try {
            shopManager.save();
        } catch (IOException e) {
            getLogger().severe("Failed to save shop data after removal: " + e.getMessage());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Optional<ShopData> optionalShop = shopManager.getShop(block);
        if (optionalShop.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        ShopData shop = optionalShop.get();
        if (shop.getOwner().equals(player.getUniqueId()) && player.isSneaking()) {
            new ShopEditorMenu(this, shop).open(player);
        } else {
            if (!shop.isConfigured()) {
                player.sendMessage(ChatColor.RED + "This shop is not ready yet.");
                return;
            }
            new ShopBuyMenu(this, shop).open(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof ShopEditorMenu editorMenu) {
            editorMenu.handleClick(event);
        } else if (inventory.getHolder() instanceof ShopBuyMenu buyMenu) {
            buyMenu.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof ShopEditorMenu editorMenu) {
            editorMenu.handleClose(event);
        } else if (inventory.getHolder() instanceof ShopBuyMenu buyMenu) {
            buyMenu.handleClose(event);
        }
    }
}
