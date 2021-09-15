package com.github.giji34.t;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Borders {
    private final ArrayList<Border> borders = new ArrayList<>();
    private final DefaultHashMap<UUID, HashMap<World.Environment, Point>> lastValidPosition = new DefaultHashMap<>(HashMap::new);
    private final HashSet<UUID> cautionAlreadySentPlayers = new HashSet<UUID>();

    Borders(File configFile) {
        YamlConfiguration root = YamlConfiguration.loadConfiguration(configFile);
        Object setsObject = root.get("sets");
        if (!(setsObject instanceof ConfigurationSection)) {
            return;
        }
        ConfigurationSection sets = (ConfigurationSection) setsObject;

        Object bordersObject = sets.get("borders");
        if (!(bordersObject instanceof ConfigurationSection)){
            return;
        }
        ConfigurationSection borders = (ConfigurationSection) bordersObject;

        Object areasObject = borders.get("areas");
        if (!(areasObject instanceof ConfigurationSection)) {
            return;
        }
        ConfigurationSection areas = (ConfigurationSection) areasObject;
        for (String name : areas.getKeys(false)) {
            Object data = areas.get(name);
            if (!(data instanceof ConfigurationSection)) {
                continue;
            }
            try {
                Border border = new Border((ConfigurationSection)data);
                this.borders.add(border);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public boolean contains(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        World.Environment dimension = world.getEnvironment();
        Point position = new Point(location.getBlockX(), location.getBlockZ());
        for (Border border : this.borders) {
            if (border.dimension != dimension) {
                continue;
            }
            if (border.contains(position)) {
                return true;
            }
        }
        return false;
    }

    public void correct(Player player) {
        if (borders.isEmpty()) {
            return;
        }
        UUID uid = player.getUniqueId();
        World world = player.getWorld();
        World.Environment dimension = world.getEnvironment();
        Location location = player.getLocation();
        Point position = new Point(location.getBlockX(), location.getBlockZ());
        boolean ok = false;
        for (Border border : this.borders) {
            if (border.dimension != dimension) {
                continue;
            }
            if (border.contains(position)) {
                ok = true;
                break;
            }
        }
        if (ok) {
            this.lastValidPosition.get(uid).put(dimension, position);
        } else {
            if (!this.cautionAlreadySentPlayers.contains(uid)) {
                player.sendMessage(ChatColor.RED + "You have reached the edge of the world");
                this.cautionAlreadySentPlayers.add(uid);
            }
            Point p = this.lastValidPosition.get(uid).get(dimension);
            if (p == null) {
                location = world.getSpawnLocation();
            } else {
                location.setX(p.x);
                location.setZ(p.y);
            }
            player.setVelocity(new Vector());
            player.teleport(location);
        }
    }

    public void forget(Player player) {
        UUID uid = player.getUniqueId();
        this.lastValidPosition.remove(uid);
        this.cautionAlreadySentPlayers.remove(uid);
    }
}
