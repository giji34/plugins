package com.github.giji34.t;

import com.github.giji34.t.command.EditCommand;
import com.github.giji34.t.command.PortalCommand;
import com.github.giji34.t.command.TeleportCommand;
import com.github.giji34.t.command.ToggleGameModeCommand;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class Main extends JavaPlugin implements Listener {
    private final ToggleGameModeCommand toggleGameModeCommand = new ToggleGameModeCommand();
    private final TeleportCommand teleportCommand = new TeleportCommand(this);
    private final EditCommand editCommand = new EditCommand(this);
    private final PortalCommand portalCommand = new PortalCommand(this);
    private Permission permission;

    public Main() {
    }

    @Override
    public void onLoad() {
        try {
            File jar = getFile();
            File pluginDirectory = new File(jar.getParent(), "giji34");
            this.permission = new Permission(new File(pluginDirectory, "permission.yml"));
            this.teleportCommand.init(pluginDirectory);
            this.editCommand.init(pluginDirectory);
            this.portalCommand.init(pluginDirectory);
        } catch (Exception e) {
            getLogger().warning("error: " + e);
        }
    }

    @Override
    public void onEnable() {
        PluginCommand tpb = getCommand("tpb");
        if (tpb != null) {
            tpb.setTabCompleter(new TeleportLandmarkTabCompleter(teleportCommand, 0));
        }
        PluginCommand gfill = getCommand("gfill");
        if (gfill != null) {
            gfill.setTabCompleter(new BlockNameTabCompleter());
        }
        PluginCommand greplace = getCommand("greplace");
        if (greplace != null) {
            greplace.setTabCompleter(new BlockNameTabCompleter());
        }
        PluginCommand gtree = getCommand("gtree");
        if (gtree != null) {
            gtree.setTabCompleter(new TreeTypeTabCompleter());
        }
        PluginCommand guide = getCommand("guide");
        if (guide != null) {
            guide.setTabCompleter(new TeleportLandmarkTabCompleter(teleportCommand,1));
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player)sender;
        if (invalidGameMode(player)) {
            return false;
        }
        if (!this.permission.hasPermission(player, label)) {
            player.sendMessage(ChatColor.RED + label + "コマンドを実行する権限がありません");
            return true;
        }
        switch (label) {
            case "tpl":
                return teleportCommand.teleport(player, args);
            case "tpb":
                return teleportCommand.teleportToLandmark(player, args);
            case "gm":
                toggleGameModeCommand.toggle(player);
                return true;
            case "gfill":
                return editCommand.fill(player, args);
            case "greplace":
                return editCommand.replace(player, args);
            case "gundo":
                return editCommand.undo(player);
            case "gregenerate":
                return editCommand.regenerate(player, args);
            case "gtree":
                return editCommand.tree(player, args);
            case "guide":
                return teleportCommand.guide(player, args);
            case "follow":
                return teleportCommand.follow(player, args);
            case "create_portal":
                return portalCommand.create(player, args, editCommand);
            case "delete_portal":
                return portalCommand.delete(player, args);
            default:
                return false;
        }
    }

    private boolean invalidGameMode(Player player) {
        GameMode current = player.getGameMode();
        return current != GameMode.CREATIVE && current != GameMode.SPECTATOR;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (invalidGameMode(player)) {
            return;
        }
        if (!e.hasItem()) {
            return;
        }
        ItemStack tool = e.getItem();
        if (tool == null) {
            return;
        }
        if (tool.getType() != Material.WOODEN_AXE) {
            return;
        }
        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }
        EquipmentSlot hand = e.getHand();
        if (hand != EquipmentSlot.HAND) {
            return;
        }
        Loc loc = Loc.fromVectorFloored(block.getLocation().toVector());
        Action action = e.getAction();
        if (action == Action.LEFT_CLICK_BLOCK) {
            editCommand.setSelectionStartBlock(player, loc);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            editCommand.setSelectionEndBlock(player, loc);
        } else {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        if (invalidGameMode(player)) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack tool = inventory.getItemInMainHand();
        if (tool.getType() != Material.WOODEN_AXE) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent ese) {
        if (!(ese instanceof CreatureSpawnEvent)) {
            return;
        }
        CreatureSpawnEvent e = (CreatureSpawnEvent)ese;
        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
        EntityType entityType = e.getEntity().getType();
        if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            switch (entityType) {
                case WITHER:
                case ENDER_DRAGON:
                    e.setCancelled(true);
                    break;
            }
            return;
        }
        if (reason == CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION) {
            e.setCancelled(true);
            getLogger().info("村の襲撃: " + entityType + " のスポーンをキャンセルしました");
            return;
        }
        if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }
        if (reason == CreatureSpawnEvent.SpawnReason.NATURAL) {
            e.setCancelled(true);
            return;
        }
    }
}
