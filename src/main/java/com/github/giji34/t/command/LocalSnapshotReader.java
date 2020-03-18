package com.github.giji34.t.command;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.InflaterInputStream;

class LocalSnapshotReader {
    final File pluginDirectory;

    LocalSnapshotReader(File pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
    }

    @NotNull
    Snapshot read(String name, int dimension, BlockRange current) {
        MutableSnapshot s = new MutableSnapshot(current);
        ArrayList<String> palette = new ArrayList<>();
        File dir = new File(new File(new File(pluginDirectory, "wildblocks"), name), Integer.toString(dimension));
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(dir, "palette.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                palette.add(line);
            }
        } catch (Exception e) {
            s.setErrorMessage("パレットの読み込みエラー");
            return s;
        }
        try {
            int minChunkX = current.getMinX() >> 4;
            int maxChunkX = current.getMaxX() >> 4;
            int minChunkZ = current.getMinZ() >> 4;
            int maxChunkZ = current.getMaxZ() >> 4;
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                    File idx = new File(dir, "c." + chunkX + "." + chunkZ + ".idx");
                    if (!idx.exists()) {
                        s.setErrorMessage("指定した範囲のブロック情報がまだありません (" + idx.getName() + ")");
                        return s;
                    }
                    FileInputStream fileInputStream = new FileInputStream(idx);
                    InputStream inputStream = new InflaterInputStream(fileInputStream);
                    int minX = chunkX * 16;
                    int minZ = chunkZ * 16;
                    int x = 0;
                    int y = 0;
                    int z = 0;
                    int materialId = 0;
                    int digit = 0;
                    while (true) {
                        int b = inputStream.read();
                        if (b < 0) {
                            break;
                        }
                        materialId = materialId | ((0x7f & b) << (7 * digit));
                        if (b < 0x80) {
                            int blockX = minX + x;
                            int blockY = y;
                            int blockZ = minZ + z;
                            if (current.contains(blockX, blockY, blockZ)) {
                                String data = palette.get(materialId);
                                s.setBlockData(blockX, blockY, blockZ, data);
                            }
                            x = x + 1;
                            if (x == 16) {
                                x = 0;
                                z = z + 1;
                                if (z == 16) {
                                    y = y + 1;
                                    z = 0;
                                    if (y == 256) {
                                        break;
                                    }
                                }
                            }
                            materialId = 0;
                            digit = 0;
                        } else {
                            digit++;
                        }
                    }
                    inputStream.close();
                    fileInputStream.close();
                }
            }
            return s;
        } catch (Exception e) {
            s.setErrorMessage("データベースの読み込みエラー");
            e.printStackTrace();
            return s;
        }
    }
}
