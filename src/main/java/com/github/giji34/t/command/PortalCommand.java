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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    final HashMap<UUID, HashMap<Loc, Portal>> storage = new HashMap<>();
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

    public void reload() {
        this.loadConfig();
    }

    File getConfigFile() {
        return new File(pluginDirectory, "portals.yml");
    }

    File getStatusFile() {
        return new File(pluginDirectory, "user_status.yml");
    }

    public boolean createInterServerPortal(Player player, String[] args, EditCommand editCommand) {
        BlockRange selection = editCommand.getCurrentSelection(player);
        if (selection == null) {
            player.sendMessage(ChatColor.RED + "ポータルにする範囲を木の斧で選択して下さい");
            return true;
        }
        if (args.length != 2 && args.length != 6) {
            return false;
        }
        String name = args[0];
        String destination = args[1];
        boolean enableReturnLoc = args.length == 6;
        Location returnLoc = null;
        if (enableReturnLoc) {
            String xStr = args[2];
            String yStr = args[3];
            String zStr = args[4];
            String yawStr = args[5];
            double x, y, z;
            try {
                x = Double.parseDouble(xStr);
                y = Double.parseDouble(yStr);
                z = Double.parseDouble(zStr);
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "帰還地点の座標の書式が不正です");
                return true;
            }
            try {
                Integer.parseInt(xStr);
                x += 0.5;
            } catch (Exception e) {}
            try {
                Integer.parseInt(zStr);
                z += 0.5;
            } catch (Exception e) {}
            float yaw;
            try {
                yaw = Float.parseFloat(yawStr);
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "帰還地点の座標の書式が不正です");
                return true;
            }
            returnLoc = new Location(null, x, y, z, yaw, 0);
        }
        UUID worldUUID = player.getWorld().getUID();
        final ArrayList<Loc> blocks = new ArrayList<>();
        selection.forEach((Loc loc) -> {
            blocks.add(loc);
            return Optional.empty();
        });
        final InterServerPortal portal = new InterServerPortal(name, worldUUID, blocks, returnLoc, destination);
        if (!portal.register(storage, player)) {
            return true;
        }
        try {
            this.saveConfig();
        } catch (Exception e) {
            owner.getLogger().warning(e.getMessage());
            player.sendMessage(ChatColor.RED + "設定ファイル portals.yml の書き込みに失敗しました");
            return true;
        }
        player.sendMessage("ポータル \"" + name + "\" を設置しました");
        return true;
    }

    // /create_intra_server_portal <name> <destX> <destY> <destZ> <yaw>
    public boolean createIntraServerPortal(Player player, String[] args, EditCommand editCommand) {
        BlockRange selection = editCommand.getCurrentSelection(player);
        if (selection == null) {
            player.sendMessage(ChatColor.RED + "ポータルにする範囲を木の斧で選択して下さい");
            return true;
        }
        if (args.length != 5) {
            return false;
        }
        String name = args[0];
        String xStr = args[1];
        String yStr = args[2];
        String zStr = args[3];
        String yawStr = args[4];
        double x, y, z;
        try {
            x = Double.parseDouble(xStr);
            y = Double.parseDouble(yStr);
            z = Double.parseDouble(zStr);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "帰還地点の座標の書式が不正です");
            return true;
        }
        try {
            Integer.parseInt(xStr);
            x += 0.5;
        } catch (Exception e) {}
        try {
            Integer.parseInt(zStr);
            z += 0.5;
        } catch (Exception e) {}
        float yaw;
        try {
            yaw = Float.parseFloat(yawStr);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "帰還地点の座標の書式が不正です");
            return true;
        }
        UUID worldUUID = player.getWorld().getUID();
        final ArrayList<Loc> blocks = new ArrayList<>();
        selection.forEach((Loc loc) -> {
            blocks.add(loc);
            return Optional.empty();
        });
        final IntraServerPortal portal = new IntraServerPortal(name, worldUUID, blocks, x, y, z, yaw);
        if (!portal.register(storage, player)) {
            return true;
        }
        try {
            this.saveConfig();
        } catch (Exception e) {
            e.printStackTrace();
            owner.getLogger().warning(e.getMessage());
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
        if (!storage.containsKey(worldUUID)) {
            player.sendMessage(ChatColor.RED + "現在居るワールドには \"" + name + "\" という名前のポータルはありません");
            return true;
        }
        HashMap<Loc, Portal> portals = storage.get(worldUUID);
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
            owner.getLogger().warning(e.getMessage());
            player.sendMessage(ChatColor.RED + "設定ファイル portals.yml の書き込みに失敗しました");
            return true;
        }
        player.sendMessage("ポータル \"" + name + "\" を削除しました");
        return true;
    }

    @Nullable
    public Portal findPortal(Player player) {
        UUID worldUUID = player.getWorld().getUID();
        HashMap<Loc, Portal> portals = storage.get(worldUUID);
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
          type: "inter"
          blocks:
            - x: 1
              y: 2
              z: 3
            - x: 1
              y: 2
              z: 4
          world_uuid: "001E1288-969F-4239-80F3-559384597246"
          data:
            destination: "server_name"
            return_loc:
              x: 5.5
              y: 6
              z: 7.5
              yaw: 180.0
        portal2:
          type: "intra"
          world_uuid: "001E1288-969F-4239-80F3-559384597246"
          blocks:
            - x: 1
              y: 2
              z: 3
            - x: 1
              y: 2
              z: 4
          data:
            destination:
              x: 1
              y: 2
              z: 3
              yaw: 0.0
        */
        File configFile = getConfigFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        for (String name : config.getKeys(false)) {
            Object sectionObj = config.get(name);
            if (!(sectionObj instanceof ConfigurationSection)) {
                continue;
            }
            ConfigurationSection section = (ConfigurationSection) sectionObj;

            Portal portal;
            try {
                portal = Portal.Load(name, section);
            } catch (Exception e) {
                owner.getLogger().warning("Cannot parse portal data with name \"" + name + "\"");
                owner.getLogger().warning(e.getMessage());
                continue;
            }

            if (!storage.containsKey(portal.worldUuid)) {
                storage.put(portal.worldUuid, new HashMap<>());
            }
            HashMap<Loc, Portal> target = storage.get(portal.worldUuid);
            for (Loc block : portal.blocks) {
                target.put(block, portal);
            }
        }
    }

    private void saveConfig() throws Exception {
        File tmp;
        try {
            tmp = File.createTempFile("tmp", null);
            FileOutputStream fos = new FileOutputStream(tmp.getAbsolutePath());
            OutputStreamWriter osw_ = new OutputStreamWriter(fos);
            final BufferedWriter br = new BufferedWriter(osw_);
            for (UUID worldUUID : storage.keySet()) {
                HashMap<Loc, Portal> portals = storage.get(worldUUID);
                HashMap<String, Portal> names = new HashMap<>();
                for (Portal portal : portals.values()) {
                    names.put(portal.name, portal);
                }
                for (String name : names.keySet()) {
                    Portal portal = names.get(name);
                    portal.save(br);
                }
            }
            br.close();
        } catch (Exception e) {
            throw e;
        }

        File configFile = getConfigFile();
        Files.move(tmp.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void loadUserStatus() {
        /*
        user1:
          world_uuid: "6125BB4C-C988-4EEC-A9F4-68EE713478E1"
          location:
            x: 1.5
            y: 2
            z: 3.5
            yaw: 180.0
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
            Object yawObj = locationSec.get("yaw");

            if (!(xObj instanceof Number) || !(yObj instanceof Number) || !(zObj instanceof Number)) {
                continue;
            }
            double x = ((Number)xObj).doubleValue();
            double y = ((Number)yObj).doubleValue();
            double z = ((Number)zObj).doubleValue();

            float yaw = 0;
            if (yawObj instanceof Number) {
                yaw = ((Number)yawObj).floatValue();
            }

            World world = owner.getServer().getWorld(worldUUID);
            if (world == null) {
                continue;
            }
            Location location = new Location(world, x, y, z, yaw, 0);
            portalReturnLocation.put(name, location);
            owner.getLogger().info("loaded: portalReturnLocation[" + name + "] = (" + worldUUID.toString() + ", " + x + ", " + y + ", " + z + ", " + yaw + ", " + "0)");
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
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            float yaw = location.getYaw();
            br.write("    x: " + x);
            br.newLine();
            br.write("    y: " + y);
            br.newLine();
            br.write("    z: " + z);
            br.newLine();
            br.write("    yaw: " + yaw);
            br.newLine();
            owner.getLogger().info("saved: portalReturnLocation[" + name + "] = (" + worldUUID.toString() + ", " + x + ", " + y + ", " + z + ", " + yaw + ", 0)");
        }
        br.close();
    }
}