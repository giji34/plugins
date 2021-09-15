package com.github.giji34.spigot.command;

import com.github.giji34.spigot.Loc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

class MutableSnapshot implements Snapshot {
    final BlockRange range;
    @Nullable String errorMessage;
    final String[] blockData;
    final String[] biomes;
    final int[] versions;

    MutableSnapshot(BlockRange range) {
        this.range = range;
        this.blockData = new String[range.volume()];
        this.biomes = new String[range.volume()];
        this.versions = new int[range.volume()];
    }

    void setErrorMessage(@NotNull String s) {
        this.errorMessage = s;
    }

    void set(int x, int y, int z, @NotNull String blockData, @Nullable String biome, int version) {
        final int idx = getIndex(x, y, z);
        if (0 <= idx && idx < this.blockData.length) {
            this.blockData[idx] = blockData;
            this.biomes[idx] = biome;
            this.versions[idx] = version;
        }
    }

    int getIndex(int x, int y, int z) {
        final int dx = range.getMaxX() - range.getMinX() + 1;
        final int dz = range.getMaxZ() - range.getMinZ() + 1;
        return dx * dz * (y - range.getMinY()) + dx * (z - range.getMinZ()) + (x - range.getMinX());
    }

    @Override
    public @NotNull BlockRange getRange() {
        return range;
    }

    @Override
    public @Nullable String blockAt(Loc loc) {
        final int idx = getIndex(loc.x, loc.y, loc.z);
        if (0 <= idx && idx < this.blockData.length) {
            return this.blockData[idx];
        } else {
            return null;
        }
    }

    @Override
    public @Nullable String biomeAt(Loc loc) {
        final int idx = getIndex(loc.x, loc.y, loc.z);
        if (0 <= idx && idx < this.biomes.length) {
            return this.biomes[idx];
        } else {
            return null;
        }
    }

    @Override
    public Optional<Integer> versionAt(Loc loc) {
        final int idx = getIndex(loc.x, loc.y, loc.z);
        if (0 <= idx && idx < this.versions.length) {
            return Optional.of(this.versions[idx]);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public @Nullable String getErrorMessage() {
        return errorMessage;
    }
}
