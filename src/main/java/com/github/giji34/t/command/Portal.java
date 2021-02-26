package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.util.*;

public abstract class Portal {
    public final String name;
    public final HashSet<Loc> blocks;
    public final UUID worldUuid;

    Portal(String name, UUID worldUuid, List<Loc> blocks) {
        this.name = name;
        this.worldUuid = worldUuid;
        this.blocks = new HashSet<>();
        blocks.forEach((loc) -> {
            this.blocks.add(loc);
        });
    }

    public static Portal Load(String name, ConfigurationSection section) throws Exception {
        // blocks
        final Object blocksObj = section.get("blocks");
        if (!(blocksObj instanceof ArrayList)) {
            throw new Exception("\"blocks\" section does not exist");
        }
        final ArrayList<Loc> blocks = new ArrayList<>();
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

        // world_uuid
        final Object worldUUIDObj = section.get("world_uuid");
        if (!(worldUUIDObj instanceof String)) {
            throw new Exception("\"world_uuid\" section does not exist");
        }
        final String worldUUIDString = (String)worldUUIDObj;
        final UUID worldUUID = UUID.fromString(worldUUIDString);

        // type
        final Object typeObj = section.get("type");
        if (!(typeObj instanceof String)) {
            throw new Exception("\"type\" section does not exist");
        }
        final String type = (String)typeObj;
        if (type.equals("inter")) {
            return new InterServerPortal(name, worldUUID, blocks, section);
        } else if (type.equals("intra")) {
            return new IntraServerPortal(name, worldUUID, blocks, section);
        } else {
            throw new Exception("unknown portal type: " + type);
        }
    }

    public void save(BufferedWriter br) throws Exception {
        br.write(name + ":");
        br.newLine();
        br.write("  world_uuid: \"" + this.worldUuid.toString() + "\"");
        br.newLine();
        br.write("  blocks:");
        br.newLine();
        for (Loc loc : this.blocks) {
            br.write("    - x: " + loc.x);
            br.newLine();
            br.write("      y: " + loc.y);
            br.newLine();
            br.write("      z: " + loc.z);
            br.newLine();
        }
        br.newLine();
    }

    public boolean register(HashMap<UUID, HashMap<Loc, Portal>> storage, Player logger) {
        if (storage.containsKey(this.worldUuid)) {
            HashMap<Loc, Portal> portals = storage.get(this.worldUuid);
            for (Loc loc : portals.keySet()) {
                Portal portal = portals.get(loc);
                if (portal.name.equals(name)) {
                    logger.sendMessage(ChatColor.RED + "すでに \"" + name + "\" という名前のポータルが設置済みです. delete_portal " + name + " で削除してから設置しなおして下さい");
                    return false;
                }
                if (this.blocks.contains(loc)) {
                    logger.sendMessage(ChatColor.RED + "(" + loc.x + ", " + loc.y + ", " + loc.z + ") には既に \"" + portal.name + "\" という名前のポータルが設置済みです");
                    return false;
                }
            }
        } else {
            storage.put(this.worldUuid, new HashMap<>());
        }
        final HashMap<Loc, Portal> target = storage.get(this.worldUuid);
        this.blocks.forEach((loc) -> {
            target.put(loc, this);
        });
        return true;
    }

    public abstract void apply(Player player, JavaPlugin source);
}
