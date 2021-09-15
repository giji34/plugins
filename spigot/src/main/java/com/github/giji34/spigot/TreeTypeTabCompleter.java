package com.github.giji34.spigot;

import org.bukkit.TreeType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TreeTypeTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
        if (args.length == 0) {
            return Stream.of(TreeType.values()).map(Enum::toString).collect(Collectors.toList());
        } else {
            String a = args[0].toLowerCase();
            return Stream.of(TreeType.values()).filter(it -> {
                return it.toString().toLowerCase().startsWith(a);
            }).map(Enum::toString).collect(Collectors.toList());
        }
    }
}
