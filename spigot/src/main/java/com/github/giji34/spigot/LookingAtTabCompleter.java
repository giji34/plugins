package com.github.giji34.spigot;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LookingAtTabCompleter implements TabCompleter {
  @Nullable
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
    if (!(sender instanceof Player)) {
      return null;
    }
    Player player = (Player) sender;
    Block lookingAt = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
    if (lookingAt == null) {
      return null;
    }
    Location location = lookingAt.getLocation();
    String candidate = location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    ArrayList<String> ret = new ArrayList<>();
    ret.add(candidate);
    return ret;
  }
}
