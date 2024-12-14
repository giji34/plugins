package com.github.giji34.plugins.spigot;

import org.bukkit.plugin.java.JavaPlugin;

public class BlockStateMapping {
  private final GameVersion to;

  BlockStateMapping(JavaPlugin owner) throws Exception {
    this.to = GameVersion.fromServer(owner.getServer());
  }

  public String migrate(String blockData, GameVersion from) {
    var hasNamespace = blockData.startsWith("minecraft:");
    var name = hasNamespace ? blockData.substring(10) : blockData;
    var data = "";
    var index = name.indexOf("[");
    if (index > 0) {
      data = name.substring(index + 1, name.length() - 1);
      name = name.substring(0, index);
    }
    if (name.equals("grass_path")) {
      var v = new GameVersion(1, 17, 0);
      if (from.less(v) && to.graterOrEqual(v)) {
        name = "dirt_path";
      }
    } else if (name.equals("grass")) {
      var v = new GameVersion(1, 20, 3);
      if (from.less(v) && to.graterOrEqual(v)) {
        name = "short_grass";
      }
    }
    var ret = hasNamespace ? "minecraft:" + name : name;
    if (data.isEmpty()) {
      return ret;
    } else {
      return ret + "[" + data + "]";
    }
  }
}
