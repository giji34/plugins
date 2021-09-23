package com.github.giji34.plugins.spigot.command;

/**
 {
   status: string,
   block:{
     palette: string[], [ "minecraft:oak_slab[type=bottom,water_logged:true]", ... ]
     indices: number[], for(y){for(z){for(x){}}} の順で格納. palette[blocks[i]] で BlockData を取得する.
   },
   biome: {
     palette: string[],
     indices: number[]
   },
   version: {
     palette: number[],
     indices: number[]
   }
 }
 */

public class SnapshotServerResponse {
  public String status;
  public StringPaletteAndIndices block;
  public StringPaletteAndIndices biome;
  public IntPaletteAndIndices version;
}
