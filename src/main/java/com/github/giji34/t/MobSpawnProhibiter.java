package com.github.giji34.t;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Logger;

class MobSpawnProhibiter {
    final HashMap<UUID, HashSet<Loc>> storage;
    final JavaPlugin owner;

    MobSpawnProhibiter(File configFile, JavaPlugin owner) {
        this.owner = owner;
        this.storage = parseConfigFile(configFile, owner.getLogger());
    }

    boolean isMobSpawnAllowed(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        UUID uuid = world.getUID();
        if (!this.storage.containsKey(uuid)) {
            return false;
        }
        Loc loc = new Loc(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return this.storage.get(uuid).contains(loc);
    }

    private static HashMap<UUID, HashSet<Loc>> parseConfigFile(File configFile, Logger logger) {
        /*
        regions:
        - world_uuid: "6125BB4C-C988-4EEC-A9F4-68EE713478E1"
          min_x: 0
          max_x: 10
          min_y: 64
          max_y: 65
          min_z: 0
          max_z: 10
        */
        HashMap<UUID, HashSet<Loc>> storage = new HashMap<>();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        Object regionsObj = config.get("regions");
        if (!(regionsObj instanceof ArrayList)) {
            return storage;
        }
        for (Object regionObj : (ArrayList)regionsObj) {
            if (!(regionObj instanceof HashMap)) {
                continue;
            }
            HashMap<Object, Object> region = (HashMap<Object, Object>)regionObj;
            Object worldUUIDObj = region.get("world_uuid");
            if (!(worldUUIDObj instanceof String)) {
                continue;
            }
            Object minXObj = region.get("min_x");
            Object maxXObj = region.get("max_x");
            Object minYObj = region.get("min_y");
            Object maxYObj = region.get("max_y");
            Object minZObj = region.get("min_z");
            Object maxZObj = region.get("max_z");
            if (!(minXObj instanceof Integer) || !(maxXObj instanceof Integer)) {
                continue;
            }
            if (!(minYObj instanceof Integer) || !(maxYObj instanceof Integer)) {
                continue;
            }
            if (!(minZObj instanceof Integer) || !(maxZObj instanceof Integer)) {
                continue;
            }
            int minX = (Integer)minXObj;
            int maxX = (Integer)maxXObj;
            int minY = (Integer)minYObj;
            int maxY = (Integer)maxYObj;
            int minZ = (Integer)minZObj;
            int maxZ = (Integer)maxZObj;
            if (minX > maxX || minY > maxY || minZ > maxZ) {
                continue;
            }
            UUID uuid = null;
            try {
                uuid = UUID.fromString((String)worldUUIDObj);
            } catch (Exception e) {
                logger.warning("cannot parse world_uuid: " + worldUUIDObj);
                continue;
            }
            logger.info("mob spawn allowed area: " + uuid + ", [" + minX + ", " + minY + ", " + minZ + "]-[" + maxX + ", " + maxY + ", " + maxZ + "]");
            if (!storage.containsKey(uuid)) {
                storage.put(uuid, new HashSet<>());
            }
            final HashSet<Loc> s = storage.get(uuid);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Loc loc = new Loc(x, y, z);
                        s.add(loc);
                    }
                }
            }
        }
        return storage;
    }
}
