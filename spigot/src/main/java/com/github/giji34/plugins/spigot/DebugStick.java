package com.github.giji34.plugins.spigot;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

class DebugStick {
  private final HashMap<UUID, String> properties = new HashMap<>();

  void onInteractWithMainHand(Player player, Block block) {
    String fullName = block.getBlockData().getAsString(false);
    HashMap<String, String> props = BlockPropertyHelper.Properties(fullName);
    if (props.isEmpty()) {
      SendSubtitle(player, fullName + " はプロパティを持っていません");
      return;
    }
    UUID uuid = player.getUniqueId();
    String[] keys = props.keySet().toArray(new String[0]);
    String current = this.properties.get(uuid);
    String next;
    if (current == null) {
      next = keys[0];
    } else {
      int index = -1;
      for (int i = 0; i < keys.length; i++) {
        if (current.equals(keys[i])) {
          index = i;
          break;
        }
      }
      int nextIndex = (index + 1) % keys.length;
      next = keys[nextIndex];
    }
    this.properties.put(uuid, next);
    String subtitle = "「" + next + "」を選択しました(" + props.get(next) + ")";
    SendSubtitle(player, subtitle);
  }

  void onInteractWithOffHand(Player player, Block block) {
    Server server = player.getServer();
    BlockData blockData = block.getBlockData();
    String fullName = blockData.getAsString(false);
    HashMap<String, String> props = BlockPropertyHelper.Properties(fullName);
    if (props.isEmpty()) {
      SendSubtitle(player, fullName + " はプロパティを持っていません");
      return;
    }
    UUID uuid = player.getUniqueId();
    String name = this.properties.get(uuid);
    if (name == null || !props.containsKey(name)) {
      String[] keys = props.keySet().toArray(new String[0]);
      name = keys[0];
    }
    String value = props.get(name);
    String nextBlockData;
    if (value.equals("true") || value.equals("false")) {
      String next = value.equals("true") ? "false" : "true";
      String existing = blockData.getAsString();
      String materialName = existing;
      if (existing.contains("[")) {
        materialName = existing.substring(0, existing.indexOf("["));
      }
      nextBlockData = BlockPropertyHelper.MergeBlockData(existing, materialName + "[" + name + "=" + next + "]", server);
    } else {
      boolean changed = BlockPropertyHelper.RotatePropertyValue(blockData, name);
      if (!changed) {
        player.sendMessage(ChatColor.RED + "「" + name + "」の変更は未実装です");
        return;
      }
      nextBlockData = blockData.getAsString();
    }
    try {
      block.setBlockData(server.createBlockData(nextBlockData), false);
    } catch (IllegalArgumentException e) {
      player.sendMessage(ChatColor.RED + "BlockData の作成に失敗しました. name=" + name + " " + value);
      return;
    }
    SendSubtitle(player, "「" + name + "」を変更しました");
  }

  public void forget(Player player) {
    properties.remove(player.getUniqueId());
  }

  private static void SendSubtitle(Player player, String subtitle) {
    player.sendTitle("", subtitle, 10, 70, 20);
  }
}
