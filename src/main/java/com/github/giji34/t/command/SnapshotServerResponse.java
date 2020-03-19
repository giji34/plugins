package com.github.giji34.t.command;

/**
 {
 status: string,
 palette: string[], // [ "minecraft:oak_slab[type=bottom,water_logged:true]", ... ]
 blocks: number[] // for(y){for(z){for(x){}}} の順で格納. palette[blocks[i]] で BlockData を取得する.
 }
 */

public class SnapshotServerResponse {
    public String status;
    public String[] palette;
    public int[] blocks;

    public SnapshotServerResponse(String status, String[] palette, int[] blocks) {
        this.status = status;
        this.palette = palette;
        this.blocks = blocks;
    }
}
