package com.github.giji34.t;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

class TeleportLandmarkTabCompleter implements TabCompleter {
    final int argIndex;

    TeleportLandmarkTabCompleter(int argIndex) {
        this.argIndex = argIndex;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
        if (!(sender instanceof Player)) {
            return null;
        }
        Player player = (Player)sender;
        if (args.length <= this.argIndex) {
            return null;
        }
        final String arg = args[this.argIndex];
        ArrayList<Landmark> candidate = pickupCandidates(player, arg);
        ArrayList<String> names = new ArrayList<>();
        for (Landmark l : candidate) {
            names.add(l.name);
        }
        ArrayList<String> uniqNames = makeUnique(names);
        Collections.sort(uniqNames);
        return uniqNames;
    }

    static ArrayList<Landmark> pickupCandidates(Player player, String arg) {
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

    static Landmark findLandmark(Player player, String landmarkName) {
        UUID uid = player.getWorld().getUID();
        HashMap<String, Landmark> knownLandmarks = Main.ensureKnownLandmarks(uid);
        Landmark landmark = null;
        if (knownLandmarks.containsKey(landmarkName)) {
            landmark = knownLandmarks.get(landmarkName);
        } else {
            ArrayList<Landmark> candidate = TeleportLandmarkTabCompleter.pickupCandidates(player, landmarkName);
            HashSet<String> uniq = new HashSet<>();
            for (Landmark l : candidate) {
                uniq.add(l.name);
            }
            if (uniq.size() == 1) {
                landmark = candidate.get(0);
            } else {
                player.sendMessage(ChatColor.RED + "\"" + landmarkName + "\"に合致する建物が見つかりません");
            }
        }
        if (landmark == null) {
            player.sendMessage(ChatColor.RED + "\"" + landmarkName + "\"に合致する建物が見つかりません");
            return null;
        }
        if (landmark.worldUID.equals(uid)) {
            return landmark;
        } else {
            player.sendMessage(ChatColor.RED + "建物 \"" + landmarkName + "\" は現在居るディメンジョンには存在しません");
            return null;
        }
    }

    static ArrayList<String> makeUnique(ArrayList<String> src) {
        HashSet<String> strings = new HashSet<>();
        for (String s : src) {
            strings.add(s);
        }
        return new ArrayList<>(strings);
    }
}
