package com.github.giji34.t;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class Main extends JavaPlugin {
    public static HashMap<String, Vector> _knownBuildings;

    static {
        _knownBuildings = new HashMap<String, Vector>();
    }

    @Override
    public void onLoad() {
        try {
            loadBuildings();
        } catch (Exception e) {
            getLogger().info("error: loadBuildings");
        }
    }

    private synchronized void loadBuildings() throws Exception {
        File jar = getFile();
        File json = new File(new File(jar.getParent(), "giji34"), "buildings.tsv");
        if (json.exists()) {
            HashMap<String, Vector> buildings = new HashMap<String, Vector>();
            BufferedReader br = new BufferedReader(new FileReader(json));
            String line;
            int lineN = 0;
            while ((line = br.readLine()) != null) {
                lineN++;
                if (line.startsWith("#")) {
                    continue;
                }
                String[] tokens = line.split("\t");
                if (tokens.length < 4) {
                    continue;
                }
                String name = tokens[0];
                double x;
                double y;
                double z;
                try {
                    x = Double.parseDouble(tokens[1]);
                    y = Double.parseDouble(tokens[2]);
                    z = Double.parseDouble(tokens[3]);
                } catch (Exception e) {
                    getLogger().warning("line " + lineN + " parse error: \"" + line + "\"");
                    return;
                }
                getLogger().info(name + ": [" + x + ", " + y + ", " + z + "]");
                buildings.put(name, new Vector(x, y, z));
            }
            _knownBuildings = buildings;
        } else {
            BufferedWriter bw = new BufferedWriter(new FileWriter(json));
            bw.write("#建物名\tX\tY\tZ");
            bw.newLine();
            bw.flush();
            bw.close();
            return;
        }
    }

    public static synchronized HashMap<String, Vector> ensureKnownBuildings() {
        return new HashMap<String, Vector>(_knownBuildings);
    }

    @Override
    public void onEnable() {
        PluginCommand c = getCommand("tpb");
        if (c != null) {
            c.setTabCompleter(new TeleportBuildingTabCompleter());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player)sender;
        if ("tpl".equals(label)) {
            return this.onTeleportCommand(player, args);
        } else if ("tpb".equals(label)) {
            return this.onTeleportToBuilding(player, args);
        } else if ("gm".equals(label)) {
            return this.onToggleGameMode(player);
        } else {
            return false;
        }
    }

    private boolean onTeleportCommand(Player player, String[] args) {
        if (args.length != 3) {
            return false;
        }
        if (!assertGameMode(player)) {
            return false;
        }
        Location loc = player.getLocation().clone();
        try {
            loc.setX(parseCoordinate(args[0], loc.getX()));
            loc.setY(parseCoordinate(args[1], loc.getY()));
            loc.setZ(parseCoordinate(args[2], loc.getZ()));
        } catch (Exception e) {
            return false;
        }

        player.teleport(loc);
        return true;
    }

    private boolean onTeleportToBuilding(Player player, String[] args) {
        if (args.length != 1) {
            return false;
        }
        if (!assertGameMode(player)) {
            return false;
        }
        Location loc = player.getLocation().clone();
        String name = args[0];
        HashMap<String, Vector> knownBuildings = ensureKnownBuildings();
        if (!knownBuildings.containsKey(name)) {
            return false;
        }
        Vector p = knownBuildings.get(name);
        loc.setX(p.getX());
        loc.setY(p.getY());
        loc.setZ(p.getZ());
        player.teleport(loc);
        return true;
    }

    private boolean onToggleGameMode(Player player) {
        if (!assertGameMode(player)) {
            return false;
        }
        GameMode current = player.getGameMode();
        if (current == GameMode.CREATIVE) {
            player.setGameMode(GameMode.SPECTATOR);
        } else if (current == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.CREATIVE);
        }
        return true;
    }

    private boolean assertGameMode(Player player) {
        GameMode current = player.getGameMode();
        return current == GameMode.CREATIVE || current == GameMode.SPECTATOR;
    }

    private static double parseCoordinate(String s, double defaultValue) throws Exception {
        if ("~".equals(s)) {
            return defaultValue;
        }
        return Double.parseDouble(s);
    }
}


class TeleportBuildingTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete​(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
        ArrayList<String> knownBuildings = new ArrayList<String>(Main.ensureKnownBuildings().keySet());
        Collections.sort(knownBuildings);
        if (args.length == 0) {
            return knownBuildings;
        }
        String name = args[0];
        if ("".equals(name)) {
            return knownBuildings;
        }
        knownBuildings.removeIf(it -> { return !it.startsWith(name); });
        return knownBuildings;
    }
}
