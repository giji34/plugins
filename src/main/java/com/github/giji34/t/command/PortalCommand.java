package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

class PortalCooldown {
    static final long kCooldownMilliSeconds = 5000;

    final Portal portal;
    final long timestamp;

    PortalCooldown(Portal portal, long timestamp) {
        this.portal = portal;
        this.timestamp = timestamp;
    }
}

public class PortalCommand {
    final HashMap<UUID, HashMap<Loc, Portal>> storege = new HashMap<>();
    final JavaPlugin owner;
    File pluginDirectory;

    public PortalCommand(JavaPlugin owner) {
        this.owner = owner;
    }

    public void init(File pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
        this.load();
    }

    File getConfigFile() {
        return new File(pluginDirectory, "portals.yml");
    }

    public boolean create(Player player, String[] args, EditCommand editCommand) {
        BlockRange selection = editCommand.getCurrentSelection(player);
        if (selection == null) {
            player.sendMessage(ChatColor.RED + "ポータルにする範囲を木の斧で選択して下さい");
            return true;
        }
        if (args.length != 2 && args.length != 5) {
            return false;
        }
        String name = args[0];
        String destination = args[1];
        boolean enableReturnLoc = args.length == 5;
        Loc returnLoc = null;
        if (enableReturnLoc) {
            int x, y, z;
            try {
                x = Integer.parseInt(args[2]);
                y = Integer.parseInt(args[3]);
                z = Integer.parseInt(args[4]);
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "帰還地点の座標の書式が不正です");
                return true;
            }
            returnLoc = new Loc(x, y, z);
        }
        UUID worldUUID = player.getWorld().getUID();
        if (storege.containsKey(worldUUID)) {
            HashMap<Loc, Portal> portals = storege.get(worldUUID);
            for (Loc loc : portals.keySet()) {
                Portal portal = portals.get(loc);
                if (portal.name.equals(name)) {
                    player.sendMessage(ChatColor.RED + "すでに \"" + name + "\" という名前のポータルが設置済みです. delete_portal " + name + " で削除してから設置しなおして下さい");
                    return true;
                }
                if (selection.contains(loc.x, loc.y, loc.z)) {
                    player.sendMessage(ChatColor.RED + "(" + loc.x + ", " + loc.y + ", " + loc.z + ") には既に \"" + portal.name + "\" という名前のポータルが設置済みです");
                    return true;
                }
            }
        } else {
            storege.put(worldUUID, new HashMap<>());
        }
        final HashMap<Loc, Portal> target = storege.get(worldUUID);
        final Portal portal = new Portal(name, returnLoc, destination);
        selection.forEach((Loc loc) -> {
            target.put(loc, portal);
            return Boolean.TRUE;
        });
        try {
            this.save();
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "設定ファイル portals.yml の書き込みに失敗しました");
            return true;
        }
        player.sendMessage("ポータル \"" + name + "\" を設置しました");
        return true;
    }

    public boolean delete(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "削除するポータルの名前を指定して下さい");
            return true;
        }
        String name = args[0];
        UUID worldUUID = player.getWorld().getUID();
        if (!storege.containsKey(worldUUID)) {
            player.sendMessage(ChatColor.RED + "現在居るワールドには \"" + name + "\" という名前のポータルはありません");
            return true;
        }
        HashMap<Loc, Portal> portals = storege.get(worldUUID);
        int cnt = 0;
        for (Iterator<Loc> it = portals.keySet().iterator(); it.hasNext(); ) {
            Loc loc = it.next();
            Portal portal = portals.get(loc);
            if (portal.name.equals(name)) {
                it.remove();
                cnt++;
            }
        }
        if (cnt == 0) {
            player.sendMessage(ChatColor.RED + "現在居るワールドには \"" + name + "\" という名前のポータルはありません");
            return true;
        }
        try {
            save();
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "設定ファイル portals.yml の書き込みに失敗しました");
            return true;
        }
        player.sendMessage("ポータル \"" + name + "\" を削除しました");
        return true;
    }

    @Nullable
    public Portal findPortal(Player player) {
        UUID worldUUID = player.getWorld().getUID();
        HashMap<Loc, Portal> portals = storege.get(worldUUID);
        if (portals == null) {
            return null;
        }
        Location loc = player.getLocation();
        Loc l = new Loc(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return portals.get(l);
    }

    final HashMap<String, Location> portalReturnLocation = new HashMap<>();
    public void setPortalReturnLocation(Player player, @Nullable Location loc) {
        //TODO(kbinani): 永続化する
        portalReturnLocation.put(player.getName(), loc);
    }

    @Nullable
    public Location getPortalReturnLocation(Player player) {
        Location returnLocation = portalReturnLocation.get(player.getName());
        if (returnLocation == null) {
            return null;
        }
        if (!returnLocation.getWorld().getUID().equals(player.getWorld().getUID())) {
            return null;
        }
        return returnLocation;
    }

    final HashMap<String, PortalCooldown> portalCooldown = new HashMap<>();
    final HashMap<String, Long> anyPortalCooldown = new HashMap<>();

    @Nullable
    public Portal filterPortalByCooldown(Player player, @Nullable Portal portal) {
        if (portal == null) {
            return null;
        }
        Portal coolingdown = getCoolingdownPortal(player);
        if (coolingdown != null) {
            portal = null;
        }
        portalCooldown.remove(player.getName());
        Long anyCooldown = anyPortalCooldown.get(player.getName());
        if (anyCooldown != null) {
            long now = System.currentTimeMillis();
            if (now < anyCooldown + PortalCooldown.kCooldownMilliSeconds) {
                portal = null;
            } else {
                anyPortalCooldown.remove(player.getName());
            }
        }
        return portal;
    }

    public void markPortalUsed(Player player, Portal portal) {
        long now = System.currentTimeMillis();
        portalCooldown.put(player.getName(), new PortalCooldown(portal, now));
    }

    public void setAnyPortalCooldown(Player player) {
        long now = System.currentTimeMillis();
        anyPortalCooldown.put(player.getName(), now);
    }

    @Nullable
    public Portal getCoolingdownPortal(Player player) {
        PortalCooldown cooldown = portalCooldown.get(player.getName());
        if (cooldown == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now < cooldown.timestamp + PortalCooldown.kCooldownMilliSeconds) {
            return cooldown.portal;
        }
        return null;
    }

    private void load() {
        /*
        portal1:
          destination: "server_name"
          world_uuid: "001E1288-969F-4239-80F3-559384597246"
          blocks:
            - x: 1
              y: 2
              z: 3
            - x: 1
              y: 2
              z: 4
          return_loc:
            x: 5
            y: 6
            z: 7
        */
        File configFile = getConfigFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        for (String name : config.getKeys(false)) {
            Object sectionObj = config.get(name);
            if (!(sectionObj instanceof ConfigurationSection)) {
                continue;
            }
            ConfigurationSection section = (ConfigurationSection) sectionObj;

            Object destinationObj = section.get("destination");
            if (!(destinationObj instanceof String)) {
                continue;
            }
            String destination = (String) destinationObj;

            Object blocksObj = section.get("blocks");
            if (!(blocksObj instanceof ArrayList)) {
                continue;
            }
            ArrayList<Loc> blocks = new ArrayList<>();
            for (Object locObj : (ArrayList) blocksObj) {
                if (!(locObj instanceof HashMap)) {
                    continue;
                }
                HashMap<String, Integer> loc = (HashMap) locObj;
                int x = loc.get("x");
                int y = loc.get("y");
                int z = loc.get("z");
                blocks.add(new Loc(x, y, z));
            }

            Loc returnLoc = null;
            Object returnLocObj = section.get("return_loc");
            if (returnLocObj instanceof ConfigurationSection) {
                ConfigurationSection returnLocSection = (ConfigurationSection) returnLocObj;
                int x = (Integer) returnLocSection.get("x");
                int y = (Integer) returnLocSection.get("y");
                int z = (Integer) returnLocSection.get("z");
                returnLoc = new Loc(x, y, z);
            }

            Object worldUUIDObj = section.get("world_uuid");
            if (!(worldUUIDObj instanceof String)) {
                continue;
            }
            String worldUUIDString = (String)worldUUIDObj;
            UUID worldUUID = UUID.fromString(worldUUIDString);
            if (!storege.containsKey(worldUUID)) {
                storege.put(worldUUID, new HashMap<>());
            }
            Portal portal = new Portal(name, returnLoc, destination);
            HashMap<Loc, Portal> target = storege.get(worldUUID);
            for (Loc block : blocks) {
                target.put(block, portal);
            }
        }
    }

    private void save() throws Exception {
        File configFile = getConfigFile();
        FileOutputStream fos = new FileOutputStream(configFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        final OutputStreamWriter osw = new OutputStreamWriter(bos);
        for (UUID worldUUID : storege.keySet()) {
            HashMap<Loc, Portal> portals = storege.get(worldUUID);
            HashSet<String> names = new HashSet<>();
            for (Portal portal : portals.values()) {
                names.add(portal.name);
            }
            for (String name : names) {
                osw.write(name + ":\n");
                osw.write("  world_uuid: \"" + worldUUID.toString() + "\"\n");
                osw.write("  blocks:\n");
                Portal p = null;
                for (Loc loc : portals.keySet()) {
                    Portal portal = portals.get(loc);
                    if (!portal.name.equals(name)) {
                        continue;
                    }
                    p = portal;
                    osw.write("    - x: " + loc.x + "\n");
                    osw.write("      y: " + loc.y + "\n");
                    osw.write("      z: " + loc.z + "\n");
                }
                if (p.returnLoc != null) {
                    osw.write("  return_loc:\n");
                    osw.write("    x: " + p.returnLoc.x + "\n");
                    osw.write("    y: " + p.returnLoc.y + "\n");
                    osw.write("    z: " + p.returnLoc.z + "\n");
                }
                osw.write("  destination: \"" + p.destination + "\"\n");
            }
        }
        osw.close();
    }
}