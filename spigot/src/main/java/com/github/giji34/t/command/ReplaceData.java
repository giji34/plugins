package com.github.giji34.t.command;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class ReplaceData {
    final Material material;
    @Nullable final String data;
    @Nullable final String biome;

    ReplaceData(String blockData, @Nullable String biome) {
        int begin = blockData.indexOf("[");
        int end = blockData.indexOf("]");
        if (begin > 0 && end > 0) {
            String name = blockData.substring(0, begin);
            this.material = Material.matchMaterial(name);
            this.data = blockData.substring(begin + 1, end);
        } else {
            this.material = Material.matchMaterial(blockData);
            this.data = null;
        }
        this.biome = biome;
    }

    String getAsString() {
        if (this.data == null) {
            return this.material.createBlockData().getAsString(true);
        } else {
            return this.material.createBlockData("[" + this.data + "]").getAsString(true);
        }
    }
}
