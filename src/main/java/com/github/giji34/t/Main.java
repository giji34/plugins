package com.github.giji34.t;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.block.Block;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.GameMode;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class Main extends JavaPlugin implements Listener {
    public static HashMap<String, Vector> _knownBuildings;
    public static final String[] allMaterials;
    private static final int kMaxFillVolume = 4096;

    static {
        _knownBuildings = new HashMap<String, Vector>();
        allMaterials = Arrays.stream(Material.values())
            .filter(it -> it.isBlock())
            .map(it -> {
                try {
                    String prefix = "minecraft:";
                    String key = it.getKey().toString();
                    if (key.startsWith(prefix)) {
                        return key.substring(prefix.length());
                    } else {
                        return key;
                    }
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(it -> {
                return it != null && !"tnt".equals(it);
            })
            .toArray(String[]::new);
    }

    private SelectedBlockRangeRegistry selectedBlockRangeRegistry;

    public Main() {
        this.selectedBlockRangeRegistry = new SelectedBlockRangeRegistry();
    }

    @Override
    public void onLoad() {
        try {
            loadBuildings();
        } catch (Exception e) {
            getLogger().info("error: loadBuildings");
        }
    }

    private synchronized void loadBuildings() throws Exception {
        File jar = getFile();
        File json = new File(new File(jar.getParent(), "giji34"), "buildings.tsv");
        if (json.exists()) {
            HashMap<String, Vector> buildings = new HashMap<String, Vector>();
            BufferedReader br = new BufferedReader(new FileReader(json));
            String line;
            int lineN = 0;
            while ((line = br.readLine()) != null) {
                lineN++;
                if (line.startsWith("#")) {
                    continue;
                }
                String[] tokens = line.split("\t");
                if (tokens.length < 4) {
                    continue;
                }
                String name = tokens[0];
                double x;
                double y;
                double z;
                try {
                    x = parseX(tokens[1], 0);
                    y = parseY(tokens[2], 0);
                    z = parseZ(tokens[3], 0);
                } catch (Exception e) {
                    getLogger().warning("line " + lineN + " parse error: \"" + line + "\"");
                    return;
                }
                getLogger().info(name + ": [" + x + ", " + y + ", " + z + "]");
                buildings.put(name, new Vector(x, y, z));
            }
            _knownBuildings = buildings;
        } else {
            BufferedWriter bw = new BufferedWriter(new FileWriter(json));
            bw.write("#建物名\tX\tY\tZ");
            bw.newLine();
            bw.flush();
            bw.close();
            return;
        }
    }

    public static synchronized HashMap<String, Vector> ensureKnownBuildings() {
        return new HashMap<String, Vector>(_knownBuildings);
    }

    @Override
    public void onEnable() {
        PluginCommand tpb = getCommand("tpb");
        if (tpb != null) {
            tpb.setTabCompleter(new TeleportBuildingTabCompleter());
        }
        PluginCommand gfill = getCommand("gfill");
        if (gfill != null) {
            gfill.setTabCompleter(new BlockNameTabCompleter());
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player)sender;
        if ("tpl".equals(label)) {
            return this.onTeleportCommand(player, args);
        } else if ("tpb".equals(label)) {
            return this.onTeleportToBuilding(player, args);
        } else if ("gm".equals(label)) {
            return this.onToggleGameMode(player);
        } else if ("gfill".equals(label)) {
            return this.onFillCommand(player, args);
        } else {
            return false;
        }
    }

    private boolean onTeleportCommand(Player player, String[] args) {
        if (args.length != 3) {
            return false;
        }
        if (!assertGameMode(player)) {
            return false;
        }
        Location loc = player.getLocation().clone();
        try {
            loc.setX(parseX(args[0], loc.getX()));
            loc.setY(parseY(args[1], loc.getY()));
            loc.setZ(parseZ(args[2], loc.getZ()));
        } catch (Exception e) {
            return false;
        }

        player.teleport(loc);
        return true;
    }

    private boolean onTeleportToBuilding(Player player, String[] args) {
        if (args.length != 1) {
            return false;
        }
        if (!assertGameMode(player)) {
            return false;
        }
        Location loc = player.getLocation().clone();
        String name = args[0];
        HashMap<String, Vector> knownBuildings = ensureKnownBuildings();
        if (!knownBuildings.containsKey(name)) {
            return false;
        }
        Vector p = knownBuildings.get(name);
        loc.setX(p.getX());
        loc.setY(p.getY());
        loc.setZ(p.getZ());
        player.teleport(loc);
        return true;
    }

    private boolean onToggleGameMode(Player player) {
        if (!assertGameMode(player)) {
            return false;
        }
        GameMode current = player.getGameMode();
        if (current == GameMode.CREATIVE) {
            player.setGameMode(GameMode.SPECTATOR);
        } else if (current == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.CREATIVE);
        }
        return true;
    }

    private boolean onFillCommand(Player player, String[] args) {
        SelectedBlockRange current = this.selectedBlockRangeRegistry.current(player);
        if (current == null) {
            player.sendMessage(ChatColor.RED + "まだ選択範囲が設定されていません");
            return false;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "ブロック名を指定してください (例: /gfill dirt)");
            return false;
        }
        String name = args[0];
        Material material = Material.matchMaterial("minecraft:" + name);
        if (material == null) {
            player.sendMessage(ChatColor.RED + "ブロック名が正しくありません");
            return false;
        }
        int volume = current.volume();
        if (volume > kMaxFillVolume) {
            player.sendMessage(ChatColor.RED + "ブロックの個数が多すぎます ( " + volume + " / " + kMaxFillVolume + " )");
            return false;
        }
        World world = player.getWorld();
        int x0 = Math.min(current.start.x, current.end.x);
        int x1 = Math.max(current.start.x, current.end.x);
        int y0 = Math.min(current.start.y, current.end.y);
        int y1 = Math.max(current.start.y, current.end.y);
        int z0 = Math.min(current.start.z, current.end.z);
        int z1 = Math.max(current.start.z, current.end.z);

        int changed = 0;
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().equals(material)) {
                        continue;
                    }
                    block.setType(material);
                    changed++;
                }
            }
        }
        player.sendMessage(changed + "個のブロックを " + name + " に置き換えました");
        return true;
    }

    private boolean assertGameMode(Player player) {
        GameMode current = player.getGameMode();
        return current == GameMode.CREATIVE || current == GameMode.SPECTATOR;
    }

    private static double parseX(String s, double defaultValue) throws Exception {
        return parseCoordinate(s, defaultValue, 0.5);
    }

    private static double parseY(String s, double defaultValue) throws Exception {
        return parseCoordinate(s, defaultValue, 0);
    }

    private static double parseZ(String s, double defaultValue) throws Exception {
        return parseCoordinate(s, defaultValue, 0.5);
    }

    private static double parseCoordinate(String s, double defaultValue, double offset) throws Exception {
        if ("~".equals(s)) {
            return defaultValue;
        }
        try {
            int v = Integer.parseInt(s);
            return v + offset;
        } catch (Exception e) {
        }
        return Double.parseDouble(s);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!assertGameMode(player)) {
            return;
        }
        if (!e.hasItem()) {
            return;
        }
        ItemStack tool = e.getItem();
        if (tool.getType() != Material.WOODEN_AXE) {
            return;
        }
        Block block = e.getClickedBlock();
        Loc loc = Loc.fromVectorFloored(block.getLocation().toVector());
        Action action = e.getAction();
        SelectedBlockRange range;
        if (action == Action.LEFT_CLICK_BLOCK) {
            range = this.selectedBlockRangeRegistry.setStart(player, loc);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            range = this.selectedBlockRangeRegistry.setEnd(player, loc);
        } else {
            return;
        }
        e.setCancelled(true);
        if (range != null) {
            player.sendMessage(range.start.toString() + " - " + range.end.toString() + " が選択されました (" + range.volume() + " ブロック)");
        }
    }
}


class TeleportBuildingTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete​(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
        ArrayList<String> knownBuildings = new ArrayList<String>(Main.ensureKnownBuildings().keySet());
        Collections.sort(knownBuildings);
        if (args.length == 0) {
            return knownBuildings;
        }
        String name = args[0];
        if ("".equals(name)) {
            return knownBuildings;
        }
        knownBuildings.removeIf(it -> { return !it.startsWith(name); });
        return knownBuildings;
    }
}


class BlockNameTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete​(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
        ArrayList<String> blocks = new ArrayList<>(Arrays.asList(Main.allMaterials));
        Collections.sort(blocks);
        if (args.length == 0) {
            return blocks;
        }
        String name = args[0];
        if ("".equals(name)) {
            return blocks;
        }
        blocks.removeIf(it -> !it.startsWith(name));
        return blocks;
    }
}


class SelectedBlockRangeRegistry {
    private HashMap<String, MutableSelectedBlockRange> storage;

    SelectedBlockRangeRegistry() {
        this.storage = new HashMap<String, MutableSelectedBlockRange>();
    }

    /*nullable*/ SelectedBlockRange setStart(Player player, Loc loc) {
        MutableSelectedBlockRange current = ensureStorage(player);
        current.setStart(loc);
        return current.isolate();
    }

    /*nullable*/ SelectedBlockRange setEnd(Player player, Loc loc) {
        MutableSelectedBlockRange current = ensureStorage(player);
        current.setEnd(loc);
        return current.isolate();
    }

    boolean isReady(Player player) {
        MutableSelectedBlockRange current = ensureStorage(player);
        return current.isReady();
    }

    /*nullable*/ SelectedBlockRange current(Player player) {
        MutableSelectedBlockRange current = ensureStorage(player);
        return current.isolate();
    }

    private String key(Player player) {
        return player.getName();
    }

    private MutableSelectedBlockRange ensureStorage(Player player) {
        String name = key(player);
        if (!this.storage.containsKey(name)) {
            this.storage.put(name, new MutableSelectedBlockRange());
        }
        return this.storage.get(name);
    }
}


class Loc {
    public final int x;
    public final int y;
    public final int z;

    Loc(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    static Loc fromVectorFloored(Vector v) {
        return new Loc((int)Math.floor(v.getX()), (int)Math.floor(v.getY()), (int)Math.floor(v.getZ()));
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + "]";
    }

    @Override
    public Loc clone() {
        return new Loc(x, y, z);
    }
}


class SelectedBlockRange {
    public final Loc start;
    public final Loc end;

    SelectedBlockRange(Loc start, Loc end) {
        this.start = start.clone();
        this.end = end.clone();
    }

    int volume() {
        int dx = Math.abs(start.x - end.x) + 1;
        int dy = Math.abs(start.y - end.y) + 1;
        int dz = Math.abs(start.z - end.z) + 1;
        return dx * dy * dz;
    }
}


class MutableSelectedBlockRange {
    public Loc start;
    public Loc end;

    MutableSelectedBlockRange() {
    }

    void setStart(Loc start) {
        if (start == null) {
            return;
        }
        this.start = start.clone();
    }

    void setEnd(Loc end) {
        if (end == null) {
            return;
        }
        this.end = end.clone();
    }

    boolean isReady() {
        return this.start != null && this.end != null;
    }

    /*nullable*/ SelectedBlockRange isolate() {
        if (this.start == null || this.end == null) {
            return null;
        }
        return new SelectedBlockRange(this.start, this.end);
    }
}
