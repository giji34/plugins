package com.github.giji34.plugins.spigot;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ItemStackHelper {
  private static final Pattern kCanPlaceOnPattern = Pattern.compile("CanPlaceOn=\\[([^]]*)\\]");

  static List<String> GetCanPlaceOn(ItemStack itemStack) {
    ArrayList<String> ret = new ArrayList<>();
    if (itemStack == null) {
      return ret;
    }
    ItemMeta meta = itemStack.getItemMeta();
    // UNSPECIFIC_META:{meta-type=UNSPECIFIC, CanPlaceOn=[minecraft:powered_rail, minecraft:detector_rail, minecraft:activator_rail, minecraft:rail]}
    String s = meta.toString();
    Matcher m = kCanPlaceOnPattern.matcher(s);
    if (!m.find()) {
      return ret;
    }
    String canPlaceOn = m.group(1);
    return Arrays.stream(canPlaceOn.split(",")).map(String::trim).collect(Collectors.toList());
  }
}
