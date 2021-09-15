package com.github.giji34.spigot;

import com.github.giji34.spigot.command.EditCommand;
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
        ArrayList<String> blocks = new ArrayList<>(Arrays.asList(EditCommand.allMaterials));
        Collections.sort(blocks);
        if (args.length == 0) {
            return blocks;
        }
        String name = args[args.length - 1];
        if ("".equals(name)) {
            return blocks;
        }
        blocks.removeIf(it -> !it.startsWith(name) && !it.replace("_", "").startsWith(name));
        return blocks;
    }
}
