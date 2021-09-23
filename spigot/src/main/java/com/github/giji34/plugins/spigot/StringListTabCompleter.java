package com.github.giji34.plugins.spigot;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class StringListTabCompleter implements TabCompleter {
  final String[] candidates;

  StringListTabCompleter(String[] candidates) {
    this.candidates = candidates;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender,
                                    Command command,
                                    String alias,
                                    String[] args) {
    if (args.length == 0) {
      return Arrays.asList(candidates.clone());
    } else if (args.length == 1) {
      String a = args[0];
      return Arrays.stream(candidates).filter((String c) -> c.startsWith(a)).collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }
}
