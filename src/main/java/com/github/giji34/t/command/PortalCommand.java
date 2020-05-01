package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

class Portal {
    final String name;
    final Loc returnLoc;

    Portal(String name, Loc returnLoc) {
        this.name = name;
        this.returnLoc = returnLoc;
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
        if (args.length != 4) {
            return false;
        }
        String name = args[0];
        int x, y, z;
        try {
            x = Integer.parseInt(args[1]);
            y = Integer.parseInt(args[2]);
            z = Integer.parseInt(args[3]);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "帰還地点の座標の書式が不正です");
            return true;
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
        final HashMap<Loc, Portal> destination = storege.get(worldUUID);
        Loc returnLoc = new Loc(x, y, z);
        final Portal portal = new Portal(name, returnLoc);
        selection.forEach((Loc loc) -> {
            destination.put(loc, portal);
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

    private void load() {
        /*
        portal1:
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
            Object blocksObj = ((ConfigurationSection)sectionObj).get("blocks");
            if (!(blocksObj instanceof ArrayList)) {
                continue;
            }
            ArrayList<Loc> blocks = new ArrayList<>();
            for (Object locObj : (ArrayList)blocksObj) {
                if (!(locObj instanceof HashMap)) {
                    continue;
                }
                HashMap<String, Integer> loc = (HashMap)locObj;
                int x = loc.get("x");
                int y = loc.get("y");
                int z = loc.get("z");
                blocks.add(new Loc(x, y, z));
            }
            Object returnLocObj = ((ConfigurationSection)sectionObj).get("return_loc");
            if (!(returnLocObj instanceof ConfigurationSection)) {
                continue;
            }
            ConfigurationSection returnLoc = (ConfigurationSection)returnLocObj;
            int x = (Integer)returnLoc.get("x");
            int y = (Integer)returnLoc.get("y");
            int z = (Integer)returnLoc.get("z");
            Loc r = new Loc(x, y, z);
            Object worldUUIDObj = ((ConfigurationSection)sectionObj).get("world_uuid");
            if (!(worldUUIDObj instanceof String)) {
                continue;
            }
            String worldUUIDString = (String)worldUUIDObj;
            UUID worldUUID = UUID.fromString(worldUUIDString);
            if (!storege.containsKey(worldUUID)) {
                storege.put(worldUUID, new HashMap<>());
            }
            Portal portal = new Portal(name, r);
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
                osw.write("  return_loc:\n");
                osw.write("    x: " + p.returnLoc.x + "\n");
                osw.write("    y: " + p.returnLoc.y + "\n");
                osw.write("    z: " + p.returnLoc.z + "\n");
            }
        }
        osw.close();
    }
}