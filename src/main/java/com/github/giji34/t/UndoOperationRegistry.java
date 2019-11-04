package com.github.giji34.t;

import org.bukkit.entity.Player;

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

    ReplaceOperation pop(Player player) {
        if (player == null) {
            return null;
        }
        String key = player.getName();
        if (!registry.containsKey(key)) {
            return null;
        }
        ReplaceOperation operation = registry.get(key);
        registry.remove(key);
        return operation;
    }
}