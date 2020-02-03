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
        final String arg = args.length > 0 ? args[0] : "";
        ArrayList<Landmark> candidate = pickup(player, arg);
        ArrayList<String> names = new ArrayList<>();
        for (Landmark l : candidate) {
            names.add(l.name);
        }
        ArrayList<String> uniqNames = makeUnique(names);
        Collections.sort(uniqNames);
        return uniqNames;
    }

    static ArrayList<Landmark> pickup(Player player, String arg) {
        UUID uid = player.getWorld().getUID();
        HashMap<String, Landmark> landmarks = Main.ensureKnownLandmarks(uid);
        ArrayList<Landmark> availableLandmarks = new ArrayList<>();
        landmarks.forEach((yomi, landmark) -> {
            if (!landmark.worldUID.equals(uid)) {
                return;
            }
            if (arg.length() == 0) {
                availableLandmarks.add(landmark);
            } else if (yomi.startsWith(arg)) {
                availableLandmarks.add(landmark);
            }
        });
        return availableLandmarks;
    }

    static ArrayList<String> makeUnique(ArrayList<String> src) {
        HashSet<String> strings = new HashSet<>();
        for (String s : src) {
            strings.add(s);
        }
        return new ArrayList<>(strings);
    }
}
