package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

class SelectedBlockRangeRegistry {
    private HashMap<String, MutableBlockRange> storage;

    SelectedBlockRangeRegistry() {
        this.storage = new HashMap<String, MutableBlockRange>();
    }

    @Nullable
    Loc getStart(Player player) {
        MutableBlockRange current = ensureStorage(player);
        return current.start.clone();
    }

    @Nullable
    BlockRange setStart(Player player, Loc loc) {
        MutableBlockRange current = ensureStorage(player);
        current.setStart(loc, player.getWorld());
        return current.isolate();
    }

    @Nullable
    BlockRange setEnd(Player player, Loc loc) {
        MutableBlockRange current = ensureStorage(player);
        current.setEnd(loc, player.getWorld());
        return current.isolate();
    }

    @Nullable
    BlockRange current(Player player) {
        MutableBlockRange current = ensureStorage(player);
        return current.isolate();
    }

    private String key(Player player) {
        return player.getName();
    }

    private MutableBlockRange ensureStorage(Player player) {
        String name = key(player);
        if (!this.storage.containsKey(name)) {
            this.storage.put(name, new MutableBlockRange());
        }
        return this.storage.get(name);
    }
}
