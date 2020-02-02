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
        HashMap<String, Landmark> landmarks = Main.ensureKnownLandmarks(uid);
        ArrayList<String> availableLandmarks = new ArrayList<String>();
        final String arg = args.length > 0 ? args[0] : "";
        landmarks.forEach((yomi, landmark) -> {
            if (!landmark.worldUID.equals(uid)) {
                return;
            }
            if (arg.length() == 0) {
                availableLandmarks.add(landmark.name);
            } else if (yomi.startsWith(arg)) {
                availableLandmarks.add(landmark.name);
            }
        });
        Collections.sort(availableLandmarks);
        return availableLandmarks;
    }
}
