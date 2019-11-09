package com.github.giji34.t;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

class TeleportLandmarkTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        Player player = (Player)sender;
        UUID uid = player.getWorld().getUID();
        HashMap<String, Landmark> landmarks = Main.ensureKnownLandmarks();
        ArrayList<String> availableLandmarks = new ArrayList<String>();
        landmarks.forEach((name, landmark) -> {
            if (landmark.worldUID.equals(uid)) {
                availableLandmarks.add(name);
            }
        });
        Collections.sort(availableLandmarks);
        if (args.length == 0) {
            return availableLandmarks;
        }
        String name = args[0];
        if ("".equals(name)) {
            return availableLandmarks;
        }
        availableLandmarks.removeIf(it -> !it.startsWith(name));
        return availableLandmarks;
    }
}
