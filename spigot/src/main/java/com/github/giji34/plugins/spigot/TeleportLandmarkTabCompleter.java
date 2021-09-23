package com.github.giji34.plugins.spigot;

import com.github.giji34.plugins.spigot.command.TeleportCommand;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

class TeleportLandmarkTabCompleter implements TabCompleter {
  final int argIndex;
  final TeleportCommand teleportCommand;

  TeleportLandmarkTabCompleter(TeleportCommand teleport, int argIndex) {
    this.argIndex = argIndex;
    this.teleportCommand = teleport;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender,
                                    Command command,
                                    String alias,
                                    String[] args) {
    if (!(sender instanceof Player)) {
      return null;
    }
    Player player = (Player) sender;
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

  ArrayList<Landmark> pickupCandidates(Player player, String arg) {
    Environment dimension = player.getWorld().getEnvironment();
    HashMap<String, Landmark> landmarks = teleportCommand.ensureKnownLandmarks(dimension);
    ArrayList<Landmark> availableLandmarks = new ArrayList<>();
    landmarks.forEach((yomi, landmark) -> {
      if (landmark.dimension != dimension) {
        return;
      }
      if (arg.length() == 0) {
        availableLandmarks.add(landmark);
      } else if (yomi.startsWith(arg.toLowerCase()) || landmark.name.toLowerCase().startsWith(arg.toLowerCase())) {
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
