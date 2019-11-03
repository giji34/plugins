package com.github.giji34.t;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class TeleportBuildingTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender,
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
