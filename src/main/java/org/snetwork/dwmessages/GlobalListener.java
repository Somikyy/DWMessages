package org.snetwork.dwmessages;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GlobalListener implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.deathMessage((Component)null);
        Player player = event.getEntity();
        Location location = player.getLocation();
        String coordinate = location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
        String killerName = player.getKiller() != null ? player.getKiller().getName() : "§cНеизвестен";
        Economy econ = Main.getInstance().getEconomy();
        FileConfiguration config = Main.getInstance().getConfig();
        double balance = econ.getBalance(player);
        double dropPercentage = config.getDouble("drop-percentage") / 100.0D;
        double moneyToDrop = balance * dropPercentage;
        econ.withdrawPlayer(player, moneyToDrop);
        List messages;
        if (player.getKiller() != null) {
            messages = config.getStringList("death-message.from-player");
        } else {
            messages = config.getStringList("death-message.from-other");
        }

        Iterator var15 = messages.iterator();

        while(var15.hasNext()) {
            String message = (String)var15.next();
            message = message.replace("%coordinate%", coordinate).replace("%killer%", killerName).replace("%money%", String.format(Locale.FRANCE, "%.2f", moneyToDrop));
            player.sendMessage(ColorUtil.format(message));
        }

        this.dropMoneyItem(location, moneyToDrop);
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.getDisplayName() != null) {
                FileConfiguration config = Main.getInstance().getConfig();
                String displayName = ChatColor.stripColor(meta.getDisplayName());
                String symbol = config.getString("item-settings.symbol");
                if (displayName.startsWith(symbol)) {
                    Player player = event.getPlayer();
                    String moneyStr = displayName.substring(symbol.length());

                    double money;
                    try {
                        NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
                        money = format.parse(moneyStr).doubleValue();
                    } catch (ParseException var18) {
                        var18.printStackTrace();
                        return;
                    }

                    Economy econ = Main.getInstance().getEconomy();
                    econ.depositPlayer(player, money);
                    String pickupMessageTemplate = config.getString("pickup.message.msg");
                    String pickupActionbarTemplate = config.getString("pickup.actionbar.msg");
                    String pickupMessage = pickupMessageTemplate.replace("&", "§").replace("%money%", String.format(Locale.FRANCE, "%.2f", money));
                    String pickupActionbar = pickupActionbarTemplate.replace("&", "§").replace("%money%", String.format(Locale.FRANCE, "%.2f", money));
                    boolean message = config.getBoolean("pickup.message.enabled");
                    boolean actionbar = config.getBoolean("pickup.actionbar.enabled");
                    if (message) {
                        player.sendMessage(ColorUtil.format(pickupMessage));
                    }

                    if (actionbar) {
                        player.sendActionBar(ColorUtil.format(pickupActionbar));
                    }

                    event.getItem().remove();
                    event.setCancelled(true);
                }

            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (!player.isDead()) {
            return;
        }

        FileConfiguration config = Main.getInstance().getConfig();
        String title = ChatColor.translateAlternateColorCodes('&', config.getString("respawn-title.title"));
        String subtitle = ChatColor.translateAlternateColorCodes('&', config.getString("respawn-title.subtitle"));
        boolean soundEnabled = config.getBoolean("respawn-title.sound.enabled");
        Sound sound = Sound.valueOf(config.getString("respawn-title.sound.type"));
        float volume = (float)config.getDouble("respawn-title.sound.volume");
        float pitch = (float)config.getDouble("respawn-title.sound.pitch");
        player.sendTitle(title, subtitle, 10, 20, 10);
        if (soundEnabled) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }

    }

    private void dropMoneyItem(Location location, double amount) {
        FileConfiguration config = Main.getInstance().getConfig();
        String itemTypeString = config.getString("item-settings.type");
        Material itemType = Material.getMaterial(itemTypeString);
        if (itemType == null) {
            itemType = Material.EMERALD;
        }

        String itemNameTemplate = config.getString("item-settings.name");
        String moneyStr = String.format(Locale.FRANCE, "%.2f", amount);
        String itemName = itemNameTemplate.replace("%money%", moneyStr);
        ItemStack moneyItem = new ItemStack(itemType);
        ItemMeta meta = moneyItem.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));
        moneyItem.setItemMeta(meta);
        Item droppedItem = location.getWorld().dropItemNaturally(location, moneyItem);
        droppedItem.setCustomName(meta.getDisplayName());
        droppedItem.setCustomNameVisible(true);
    }
}