package com.github.giji34.t;

import org.bukkit.Material;

class MaterialHelper {
    private MaterialHelper() {}

    static boolean isBoat(Material m) {
        switch (m) {
            case OAK_BOAT:
            case SPRUCE_BOAT:
            case BIRCH_BOAT:
            case JUNGLE_BOAT:
            case ACACIA_BOAT:
            case DARK_OAK_BOAT:
                return true;
            default:
                return false;
        }
    }
}
