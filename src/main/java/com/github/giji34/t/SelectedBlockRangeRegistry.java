package com.github.giji34.t;

import org.bukkit.entity.Player;

import java.util.HashMap;

class SelectedBlockRangeRegistry {
    private HashMap<String, MutableSelectedBlockRange> storage;

    SelectedBlockRangeRegistry() {
        this.storage = new HashMap<String, MutableSelectedBlockRange>();
    }

    /*nullable*/ SelectedBlockRange setStart(Player player, Loc loc) {
        MutableSelectedBlockRange current = ensureStorage(player);
        current.setStart(loc);
        return current.isolate();
    }

    /*nullable*/ SelectedBlockRange setEnd(Player player, Loc loc) {
        MutableSelectedBlockRange current = ensureStorage(player);
        current.setEnd(loc);
        return current.isolate();
    }

    boolean isReady(Player player) {
        MutableSelectedBlockRange current = ensureStorage(player);
        return current.isReady();
    }

    /*nullable*/ SelectedBlockRange current(Player player) {
        MutableSelectedBlockRange current = ensureStorage(player);
        return current.isolate();
    }

    private String key(Player player) {
        return player.getName();
    }

    private MutableSelectedBlockRange ensureStorage(Player player) {
        String name = key(player);
        if (!this.storage.containsKey(name)) {
            this.storage.put(name, new MutableSelectedBlockRange());
        }
        return this.storage.get(name);
    }
}
