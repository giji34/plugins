package com.github.giji34.plugins.spigot;

import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BlockStateMapping {
  private final JavaPlugin owner;

  private final Map<String, String>[] tableChain;
  private final GameVersion[] tableVersions;

  BlockStateMapping(JavaPlugin owner) throws AssertionError {
    this.owner = owner;

    // https://github.com/ViaVersion/ViaVersion/tree/master/common/src/main/resources/assets/viaversion/data
    Map<String, String> table1_13To1_13_2 = load("1.13", "1.13.2");
    Map<String, String> table1_13_2To1_14 = load("1.13.2", "1.14");
    Map<String, String> table1_14To1_15 = load("1.14", "1.15");
    Map<String, String> table1_15To1_16 = load("1.15", "1.16");
    Map<String, String> table1_16To1_16_2 = load("1.16", "1.16.2");
    Map<String, String> table1_16_2To1_17 = load("1.16.2", "1.17");
    Map<String, String> table1_17To1_18 = load("1.17", "1.18");

    this.tableChain = new Map[]{table1_13To1_13_2, table1_13_2To1_14, table1_14To1_15, table1_15To1_16, table1_16To1_16_2, table1_16_2To1_17, table1_17To1_18};
    this.tableVersions = new GameVersion[]{
      new GameVersion(1, 13, 0),
      new GameVersion(1, 13, 2),
      new GameVersion(1, 14, 0),
      new GameVersion(1, 15, 0),
      new GameVersion(1, 16, 0),
      new GameVersion(1, 16, 2),
      new GameVersion(1, 17, 0),
      new GameVersion(1, 18, 0),
    };
    if (this.tableChain.length + 1 != this.tableVersions.length) {
      throw new AssertionError();
    }
  }

  Map<String, String> load(String from, String to) {
    HashMap<String, String> ret = new HashMap<>();

    JsonObject fromData = MappingDataLoader.loadData("mapping-" + from + ".json");
    Optional<JsonObject> fromBlockStates = GetAsJsonObject(fromData, "blockstates");
    if (fromBlockStates.isEmpty()) {
      return ret;
    }

    JsonObject diffData = MappingDataLoader.loadData("mappingdiff-" + from + "to" + to + ".json");
    Optional<JsonObject> diffBlockStates = GetAsJsonObject(diffData, "blockstates");
    if (diffBlockStates.isEmpty()) {
      return ret;
    }

    for (String id : diffBlockStates.get().keySet()) {
      Optional<String> changedTo = GetAsString(diffBlockStates.get(), id);
      Optional<String> changedFrom = GetAsString(fromBlockStates.get(), id);
      if (changedTo.isPresent() && changedFrom.isPresent()) {
        ret.put(changedFrom.get(), changedTo.get());
      }
    }
    return ret;
  }

  private static Optional<String> GetAsString(JsonObject object, String name) {
    if (object == null) {
      return Optional.empty();
    }
    if (!object.has(name)) {
      return Optional.empty();
    }
    JsonElement element = object.get(name);
    if (!element.isJsonPrimitive()) {
      return Optional.empty();
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (!primitive.isString()) {
      return Optional.empty();
    }
    return Optional.of(primitive.getAsString());
  }

  private static Optional<JsonObject> GetAsJsonObject(JsonObject object, String name) {
    if (object == null) {
      return Optional.empty();
    }
    if (!object.has(name)) {
      return Optional.empty();
    }
    JsonElement element = object.get(name);
    if (!element.isJsonObject()) {
      return Optional.empty();
    }
    return Optional.of(element.getAsJsonObject());
  }

  public String migrate(String blockData, GameVersion from) {
    GameVersion current = null;
    try {
      current = GameVersion.fromServer(owner.getServer());
    } catch (Exception e) {
      e.printStackTrace(System.out);
      System.out.println(e.getMessage());
    }
    if (current == null) {
      return blockData;
    }
    int fromIndex = -1;
    int toIndex = -1;
    for (int i = 0; i < tableChain.length; i++) {
      GameVersion v0 = tableVersions[i];
      GameVersion v1 = tableVersions[i + 1];
      if (v0.lessOrEqual(from) && from.less(v1)) {
        fromIndex = i;
        break;
      }
    }
    for (int i = 0; i < tableChain.length; i++) {
      GameVersion v0 = tableVersions[i];
      GameVersion v1 = tableVersions[i + 1];
      if (v0.less(current) && current.lessOrEqual(v1)) {
        toIndex = i;
        break;
      }
    }
    if (fromIndex < 0) {
      return blockData;
    }
    if (toIndex < 0) {
      toIndex = tableChain.length - 1;
    }
    for (int i = fromIndex; i <= toIndex; i++) {
      Map<String, String> table = tableChain[i];
      if (table.containsKey(blockData)) {
        blockData = table.get(blockData);
      }
    }
    return blockData;
  }
}
