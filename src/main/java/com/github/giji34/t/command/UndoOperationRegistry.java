package com.github.giji34.t.command;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

class UndoOperationRegistry {
    HashMap<String, ReplaceOperation> registry;

    UndoOperationRegistry() {
        registry = new HashMap<>();
    }

    void push(Player player, ReplaceOperation op) {
        if (op == null) {
            return;
        }
        registry.put(player.getName(), op);
    }

    @Nullable
    ReplaceOperation pop(Player player) {
        if (player == null) {
            return null;
        }
        String key = player.getName();
        if (!registry.containsKey(key)) {
            return null;
        }
        ReplaceOperation operation = registry.get(key);
        if (!operation.isIdenticalWorld(player.getWorld())) {
            player.sendMessage(ChatColor.RED + "undo 対象のワールドが現在居るワールドと異なります");
            return null;
        }
        registry.remove(key);
        return operation;
    }
}