package com.github.giji34.t;

import com.github.giji34.t.command.*;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;

public class Main extends JavaPlugin implements Listener {
    private final ToggleGameModeCommand toggleGameModeCommand = new ToggleGameModeCommand();
    private final TeleportCommand teleportCommand = new TeleportCommand(this);
    private final EditCommand editCommand = new EditCommand(this);
    private final PortalCommand portalCommand = new PortalCommand(this);
    private Permission permission;
    private MobSpawnProhibiter mobSpawnProhibiter;

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
            this.mobSpawnProhibiter = new MobSpawnProhibiter(new File(pluginDirectory, "mob_spawn_allowed_regions.yml"), this);
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
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
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
            case "fell_trees":
                return editCommand.fellTrees(player);
            case "/chunk":
                return editCommand.chunk(player);
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
        if (this.permission.hasRole(player, "member")) {
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
        } else {
            if (this.shouldRejectInteraction(e)) {
                e.setCancelled(true);
            }
        }
    }

    private boolean shouldRejectInteraction(PlayerInteractEvent e) {
        Action action = e.getAction();
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        switch (action) {
            case PHYSICAL: {
                if (block == null) {
                    return true;
                }
                Material material = block.getType();
                if (material.isInteractable()) {
                    return false;
                }
                switch (material) {
                    case FARMLAND:
                        return true;
                    default:
                        return false;
                }
            }
            case RIGHT_CLICK_BLOCK: {
                if (block == null) {
                    return true;
                }
                if (player.isSneaking()) {
                    return true;
                }
                Material material = block.getType();
                switch (material) {
                    case CAKE:
                        return true;
                    case RAIL:
                    case POWERED_RAIL:
                    case ACTIVATOR_RAIL:
                    case DETECTOR_RAIL:
                        if (!e.hasItem()) {
                            return true;
                        }
                        ItemStack itemStack = e.getItem();
                        if (itemStack.getType() == Material.MINECART) {
                            return false;
                        }
                        return true;
                }
                if (MaterialHelper.isStorage(material)) {
                    return true;
                }
                if (MaterialHelper.isBoat(e.getMaterial()) && e.getHand() == EquipmentSlot.HAND) {
                    return false;
                }
                if (material.isInteractable()) {
                    return false;
                }
                return true;
            }
            default:
                return true;
        }
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
    public void onCreatureSpawnSpawn(CreatureSpawnEvent e) {
        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
        LivingEntity entity = e.getEntity();
        EntityType entityType = entity.getType();
        switch (reason) {
            case SPAWNER_EGG:
                switch (entityType) {
                    case WITHER:
                    case ENDER_DRAGON:
                        e.setCancelled(true);
                        break;
                }
                return;
            case VILLAGE_INVASION:
                e.setCancelled(true);
                getLogger().info("村の襲撃: " + entityType + " のスポーンをキャンセルしました");
                return;
            case NATURAL:
                if (this.mobSpawnProhibiter.isMobSpawnAllowed(e.getLocation())) {
                    switch (entityType) {
                        case WANDERING_TRADER:
                        case TRADER_LLAMA:
                            // 作業の邪魔なのでスポーンを阻止する
                            getLogger().info("行商人: " + entityType + " のスポーンをキャンセルしました");
                            e.setCancelled(true);
                            break;
                    }
                } else {
                    e.setCancelled(true);
                }
                return;
            case BUILD_WITHER:
                e.setCancelled(true);
                return;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Portal portal = portalCommand.filterPortalByCooldown(player, portalCommand.findPortal(player));
        if (portal == null) {
            return;
        }
        portalCommand.markPortalUsed(player, portal);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF("Connect");
            dos.writeUTF(portal.destination);
            player.sendPluginMessage(this, "BungeeCord", baos.toByteArray());
            baos.close();
            dos.close();
        } catch (Exception e) {
            getLogger().warning("onPlayerMove; io error: e=" + e);
        }
    }


    @EventHandler
    public void onPlayerJoined(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        addPotionEffects(player);
        portalCommand.setAnyPortalCooldown(player);
        Location loc = portalCommand.getPortalReturnLocation(player);
        if (loc == null) {
            return;
        }
        player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    private void addPotionEffects(Player player) {
        ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
        int duration = 7 * 24 * 60 * 60 * 20;
        int amplifier = 0;
        boolean ambient = false;
        boolean particles = false;
        PotionEffect nightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, duration, amplifier, ambient, particles);
        PotionEffect saturation = new PotionEffect(PotionEffectType.SATURATION, duration, amplifier, ambient, particles);
        PotionEffect instantHealth = new PotionEffect(PotionEffectType.HEAL, duration, amplifier, ambient, particles);
        effects.add(nightVision);
        effects.add(saturation);
        effects.add(instantHealth);
        if (!player.addPotionEffects(effects)) {
            getLogger().warning("failed adding portion effects for player: " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Portal portal = portalCommand.getCoolingdownPortal(player);
        if (portal == null) {
            portalCommand.setPortalReturnLocation(player, null);
        } else if (portal.returnLoc != null) {
            Location loc = player.getLocation();
            loc.setX(portal.returnLoc.getX());
            loc.setY(portal.returnLoc.getY());
            loc.setZ(portal.returnLoc.getZ());
            loc.setYaw(portal.returnLoc.getYaw());
            portalCommand.setPortalReturnLocation(player, loc);
        }
    }
}

class MaterialHelper {
    private MaterialHelper() {}

    static boolean isBoat(Material m) {
        switch (m) {
            case OAK_BOAT:
            case SPRUCE_BOAT:
            case BIRCH_BOAT:
            case JUNGLE_BOAT:
            case ACACIA_BOAT:
            case DARK_OAK_BOAT:
                return true;
            default:
                return false;
        }
    }

    static boolean isStorage(Material m) {
        switch (m) {
            case CHEST:
            case CHEST_MINECART:
            //case TRAPPED_CHEST: NOTE: 本家サーバーでトラップ建築の素材に使われることがある. 再現したものを稼働させる目的でこれだけは許可する.
            case HOPPER:
            case HOPPER_MINECART:
            case FURNACE:
            case FURNACE_MINECART:
            case BARREL:
            case DROPPER:
            case DISPENSER:
            case ENDER_CHEST:
            case SHULKER_BOX:
                return true;
            default:
                return false;
        }
    }
}
