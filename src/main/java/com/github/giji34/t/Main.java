package com.github.giji34.t;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

public class Main extends JavaPlugin implements Listener {
    private static HashMap<String, Vector> _knownBuildings;
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
    private UndoOperationRegistry undoOperationRegistry;

    public Main() {
        this.selectedBlockRangeRegistry = new SelectedBlockRangeRegistry();
        this.undoOperationRegistry = new UndoOperationRegistry();
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
        PluginCommand greplace = getCommand("greplace");
        if (greplace != null) {
            greplace.setTabCompleter(new BlockNameTabCompleter());
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player)sender;
        if (!assertGameMode(player)) {
            return false;
        }
        if ("tpl".equals(label)) {
            return this.onTeleportCommand(player, args);
        } else if ("tpb".equals(label)) {
            return this.onTeleportToBuilding(player, args);
        } else if ("gm".equals(label)) {
            return this.onToggleGameMode(player);
        } else if ("gfill".equals(label)) {
            return this.onFillCommand(player, args);
        } else if ("greplace".equals(label)) {
            return this.onReplaceCommand(player, args);
        } else if ("gundo".equals(label)) {
            return this.onUndoCommand(player);
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
        final Material material = Material.matchMaterial("minecraft:" + name);
        if (material == null) {
            player.sendMessage(ChatColor.RED + "ブロック名が正しくありません");
            return false;
        }
        ReplaceOperation operation = replaceBlocks(player, current, material, (block) -> {
            return !block.getType().equals(material);
        });
        if (operation.count() > kMaxFillVolume) {
            player.sendMessage(ChatColor.RED + "ブロックの個数が多すぎます ( " + operation.count() + " / " + kMaxFillVolume + " )");
            return false;
        }
        ReplaceOperation undo = operation.apply(player.getWorld());
        player.sendMessage(operation.count() + " 個のブロックを " + name + " に置き換えました");
        undoOperationRegistry.push(player, undo);
        return true;
    }

    private boolean onReplaceCommand(Player player, String[] args) {
        SelectedBlockRange current = this.selectedBlockRangeRegistry.current(player);
        if (current == null) {
            player.sendMessage(ChatColor.RED + "まだ選択範囲が設定されていません");
            return false;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "ブロック名を指定してください (例: /greplace air dirt)");
            return false;
        }
        String fromName = args[0];
        String toName = args[1];
        final Material fromMaterial = Material.matchMaterial("minecraft:" + fromName);
        final Material toMaterial = Material.matchMaterial("minecraft:" + toName);
        if (fromMaterial == null || toMaterial == null) {
            player.sendMessage(ChatColor.RED + "ブロック名が正しくありません");
            return false;
        }
        ReplaceOperation op = replaceBlocks(player, current, toMaterial, (block) -> {
            if (!block.getType().equals(fromMaterial)) {
                return false;
            }
            return !block.getType().equals(toMaterial);
        });
        if (op.count() > kMaxFillVolume) {
            player.sendMessage(ChatColor.RED + "ブロックの個数が多すぎます ( " + op.count() + " / " + kMaxFillVolume + " )");
            return false;
        }
        ReplaceOperation undo = op.apply(player.getWorld());
        player.sendMessage(op.count() + " 個の " + fromName + " ブロックを " + toName + " に置き換えました");
        undoOperationRegistry.push(player, undo);
        return true;
    }

    private ReplaceOperation replaceBlocks(Player player, SelectedBlockRange range, Material toMaterial, Function<Block, Boolean> predicate) {
        World world = player.getWorld();
        final ReplaceOperation operation = new ReplaceOperation();
        range.forEach(loc -> {
            Block block = world.getBlockAt(loc.x, loc.y, loc.z);
            if (predicate.apply(block)) {
                operation.register(loc, toMaterial);
            }
            return true;
        });
        return operation;
    }

    private boolean onUndoCommand(Player player) {
        ReplaceOperation undo = undoOperationRegistry.pop(player);
        if (undo == null) {
            player.sendMessage(ChatColor.RED + "undo する操作がまだ存在しません");
            return false;
        }
        if (undo.count() > kMaxFillVolume) {
            player.sendMessage(ChatColor.RED + "ブロックの個数が多すぎます ( " + undo.count() + " / " + kMaxFillVolume + " )");
            return false;
        }
        undo.apply(player.getWorld());
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
        if (block == null) {
            return;
        }
        EquipmentSlot hand = e.getHand();
        if (hand != EquipmentSlot.HAND) {
            return;
        }
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
