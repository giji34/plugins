package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
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
        this.loadConfig();
        this.loadUserStatus();
    }

    File getConfigFile() {
        return new File(pluginDirectory, "portals.yml");
    }

    File getStatusFile() {
        return new File(pluginDirectory, "user_status.yml");
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
            this.saveConfig();
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
            saveConfig();
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

    static String getLocationString(@Nullable Location loc) {
        if (loc == null) {
            return "null";
        }
        return "(" + loc.getWorld().getUID().toString() + ", " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    final HashMap<String, Location> portalReturnLocation = new HashMap<>();
    public void setPortalReturnLocation(Player player, @Nullable Location loc) {
        if (loc == null) {
            portalReturnLocation.remove(player.getName());
        } else {
            portalReturnLocation.put(player.getName(), loc);
        }
        try {
            this.saveUserStatus();
        } catch (Exception e) {
            owner.getLogger().warning("user_status.yml の保存に失敗しました: e=" + e);
        }
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
        if (coolingdown == null) {
            portalCooldown.remove(player.getName());
        } else {
            portal = null;
        }
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

    private void loadConfig() {
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
            Integer minX = null;
            Integer maxX = null;
            Integer minY = null;
            Integer maxY = null;
            Integer minZ = null;
            Integer maxZ = null;
            for (Object locObj : (ArrayList) blocksObj) {
                if (!(locObj instanceof HashMap)) {
                    continue;
                }
                HashMap<String, Integer> loc = (HashMap) locObj;
                int x = loc.get("x");
                int y = loc.get("y");
                int z = loc.get("z");
                minX = minX == null ? x : Math.min(minX, x);
                maxX = maxX == null ? x : Math.max(maxX, x);
                minY = minY == null ? y : Math.min(minY, y);
                maxY = maxY == null ? y : Math.max(maxY, y);
                minZ = minZ == null ? z : Math.min(minZ, z);
                maxZ = maxZ == null ? z : Math.max(maxZ, z);
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
            owner.getLogger().info("loadConfig: name=" + name + "; blocks=[" + minX + ", " + minY + ", " + minZ + "]-[" + maxX + ", " + maxY + ", " + maxZ + "]; return_loc=" + (returnLoc == null ? "null" : returnLoc.toString()) + "; world_uuid=" + worldUUIDString);
        }
    }

    private void saveConfig() throws Exception {
        File configFile = getConfigFile();
        FileOutputStream fos = new FileOutputStream(configFile);
        OutputStreamWriter osw_ = new OutputStreamWriter(fos);
        final BufferedWriter br = new BufferedWriter(osw_);
        for (UUID worldUUID : storege.keySet()) {
            HashMap<Loc, Portal> portals = storege.get(worldUUID);
            HashSet<String> names = new HashSet<>();
            for (Portal portal : portals.values()) {
                names.add(portal.name);
            }
            for (String name : names) {
                br.write(name + ":");
                br.newLine();
                br.write("  world_uuid: \"" + worldUUID.toString() + "\"");
                br.newLine();
                br.write("  blocks:");
                br.newLine();
                Portal p = null;
                for (Loc loc : portals.keySet()) {
                    Portal portal = portals.get(loc);
                    if (!portal.name.equals(name)) {
                        continue;
                    }
                    p = portal;
                    br.write("    - x: " + loc.x);
                    br.newLine();
                    br.write("      y: " + loc.y);
                    br.newLine();
                    br.write("      z: " + loc.z);
                    br.newLine();
                }
                if (p.returnLoc != null) {
                    br.write("  return_loc:");
                    br.newLine();
                    br.write("    x: " + p.returnLoc.x);
                    br.newLine();
                    br.write("    y: " + p.returnLoc.y);
                    br.newLine();
                    br.write("    z: " + p.returnLoc.z);
                    br.newLine();
                }
                br.write("  destination: \"" + p.destination + "\"");
                br.newLine();
            }
        }
        br.close();
    }

    private void loadUserStatus() {
        /*
        user1:
          world_uuid: "6125BB4C-C988-4EEC-A9F4-68EE713478E1"
          location:
            x: 1
            y: 2
            z: 3
        */
        File file = getStatusFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String name : config.getKeys(false)) {
            Object userObj = config.get(name);
            if (!(userObj instanceof ConfigurationSection)) {
                continue;
            }
            ConfigurationSection userSec = (ConfigurationSection)userObj;
            Object worldUUIDObj = userSec.get("world_uuid");
            if (!(worldUUIDObj instanceof String)) {
                continue;
            }
            UUID worldUUID = UUID.fromString((String)worldUUIDObj);
            Object locationObj = userSec.get("location");
            if (!(locationObj instanceof ConfigurationSection)) {
                continue;
            }
            ConfigurationSection locationSec = (ConfigurationSection)locationObj;
            Object xObj = locationSec.get("x");
            Object yObj = locationSec.get("y");
            Object zObj = locationSec.get("z");

            if (!(xObj instanceof Integer) || !(yObj instanceof Integer) || !(zObj instanceof  Integer)) {
                continue;
            }
            int x = (Integer)xObj;
            int y = (Integer)yObj;
            int z = (Integer)zObj;
            World world = owner.getServer().getWorld(worldUUID);
            if (world == null) {
                continue;
            }
            Location location = new Location(world, x, y, z);
            portalReturnLocation.put(name, location);
            owner.getLogger().info("loaded: portalReturnLocation[" + name + "] = (" + worldUUID.toString() + ", " + x + ", " + y + ", " + z + ")");
        }
    }

    private void saveUserStatus() throws Exception {
        File file = getStatusFile();
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        BufferedWriter br = new BufferedWriter(osw);
        for (String name : portalReturnLocation.keySet()) {
            Location location = portalReturnLocation.get(name);
            br.write(name + ":");
            br.newLine();
            UUID worldUUID = location.getWorld().getUID();
            br.write("  world_uuid: \"" + worldUUID.toString() + "\"");
            br.newLine();
            br.write("  location:");
            br.newLine();
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            br.write("    x: " + x);
            br.newLine();
            br.write("    y: " + y);
            br.newLine();
            br.write("    z: " + z);
            br.newLine();
            owner.getLogger().info("saved: portalReturnLocation[" + name + "] = (" + worldUUID.toString() + ", " + x + ", " + y + ", " + z + ")");
        }
        br.close();
    }
}