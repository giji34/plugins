package com.github.giji34.t;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class BlockNameTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
        ArrayList<String> blocks = new ArrayList<>(Arrays.asList(Main.allMaterials));
        Collections.sort(blocks);
        if (args.length == 0) {
            return blocks;
        }
        String name = args[args.length - 1];
        if ("".equals(name)) {
            return blocks;
        }
        blocks.removeIf(it -> !it.startsWith(name));
        return blocks;
    }
}
