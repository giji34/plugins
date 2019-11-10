package com.github.giji34.t;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;

public class Main extends JavaPlugin implements Listener {
    private static HashMap<String, Landmark> _knownLandmarks;
    static final String[] allMaterials;
    private static final int kMaxFillVolume = 4096;

    static {
        _knownLandmarks = new HashMap<>();
        allMaterials = Arrays.stream(Material.values())
            .filter(Material::isBlock)
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
            .filter(it -> it != null && !"tnt".equals(it))
            .toArray(String[]::new);

        registerSqlite3Driver();
    }

    private static void registerSqlite3Driver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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
            loadLandmarks();
        } catch (Exception e) {
            getLogger().warning("error: loadLandmarks");
        }
    }

    private synchronized void loadLandmarks() throws Exception {
        File jar = getFile();
        File json = new File(new File(jar.getParent(), "giji34"), "buildings.tsv");
        if (json.exists()) {
            HashMap<String, Landmark> landmarks = new HashMap<>();
            BufferedReader br = new BufferedReader(new FileReader(json));
            String line;
            int lineN = 0;
            while ((line = br.readLine()) != null) {
                lineN++;
                if (line.startsWith("#")) {
                    continue;
                }
                String[] tokens = line.split("\t");
                if (tokens.length < 5) {
                    continue;
                }
                String name = tokens[0];
                double x;
                double y;
                double z;
                UUID uid;
                try {
                    x = parseX(tokens[1], 0);
                    y = parseY(tokens[2], 0);
                    z = parseZ(tokens[3], 0);
                    uid = UUID.fromString(tokens[4]);
                } catch (Exception e) {
                    getLogger().warning("line " + lineN + " parse error: \"" + line + "\"");
                    return;
                }
                landmarks.put(name, new Landmark(new Vector(x, y, z), uid));
            }
            _knownLandmarks = landmarks;
        } else {
            BufferedWriter bw = new BufferedWriter(new FileWriter(json));
            bw.write("#地点名\tX\tY\tZ\tワールドUID");
            bw.newLine();
            bw.flush();
            bw.close();
        }
    }

    static synchronized HashMap<String, Landmark> ensureKnownLandmarks() {
        return new HashMap<>(_knownLandmarks);
    }

    private Connection connectWildBlocksDatabase() throws Exception {
        File jar = getFile();
        File db = new File(new File(jar.getParent(), "giji34"), "regenerate.db");
        return DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
    }

    @Override
    public void onEnable() {
        PluginCommand tpb = getCommand("tpb");
        if (tpb != null) {
            tpb.setTabCompleter(new TeleportLandmarkTabCompleter());
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player)sender;
        if (invalidGameMode(player)) {
            return false;
        }
        switch (label) {
            case "tpl":
                return this.onTeleportCommand(player, args);
            case "tpb":
                return this.onTeleportToLandmark(player, args);
            case "gm":
                return this.onToggleGameMode(player);
            case "gfill":
                return this.onFillCommand(player, args);
            case "greplace":
                return this.onReplaceCommand(player, args);
            case "gundo":
                return this.onUndoCommand(player);
            case "gregenerate":
                return this.onRegenerateCommand(player, args);
            default:
                return false;
        }
    }

    private boolean onTeleportCommand(Player player, String[] args) {
        if (args.length != 3) {
            return false;
        }
        if (invalidGameMode(player)) {
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

    private boolean onTeleportToLandmark(Player player, String[] args) {
        if (args.length != 1) {
            return false;
        }
        if (invalidGameMode(player)) {
            return false;
        }
        Location loc = player.getLocation().clone();
        UUID uuid = player.getWorld().getUID();
        String name = args[0];
        HashMap<String, Landmark> knownLandmarks = ensureKnownLandmarks();
        if (!knownLandmarks.containsKey(name)) {
            return false;
        }
        Landmark landmark = knownLandmarks.get(name);
        Vector p = landmark.location;
        if (!uuid.equals(landmark.worldUID)) {
            player.sendMessage(ChatColor.RED + "地点 \"" + name + "\" はこのディメンジョンには存在しません");
            return false;
        }
        loc.setX(p.getX());
        loc.setY(p.getY());
        loc.setZ(p.getZ());
        player.teleport(loc);
        return true;
    }

    private boolean onToggleGameMode(Player player) {
        if (invalidGameMode(player)) {
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
        ReplaceOperation operation = replaceBlocks(player, current, material, (block) -> !block.getType().equals(material));
        if (operation.count() > kMaxFillVolume) {
            player.sendMessage(ChatColor.RED + "ブロックの個数が多すぎます ( " + operation.count() + " / " + kMaxFillVolume + " )");
            return false;
        }
        ReplaceOperation undo = operation.apply(player.getServer(), player.getWorld());
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
        ReplaceOperation undo = op.apply(player.getServer(), player.getWorld());
        player.sendMessage(op.count() + " 個の " + fromName + " ブロックを " + toName + " に置き換えました");
        undoOperationRegistry.push(player, undo);
        return true;
    }

    @NotNull
    private ReplaceOperation replaceBlocks(Player player, SelectedBlockRange range, Material toMaterial, Function<Block, Boolean> predicate) {
        World world = player.getWorld();
        final ReplaceOperation operation = new ReplaceOperation(player.getWorld());
        range.forEach(loc -> {
            Block block = world.getBlockAt(loc.x, loc.y, loc.z);
            if (predicate.apply(block)) {
                operation.register(loc, new ReplaceData(toMaterial, null));
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
        undo.apply(player.getServer(), player.getWorld());
        return true;
    }

    private boolean onRegenerateCommand(Player player, String[] args) {
        SelectedBlockRange current = this.selectedBlockRangeRegistry.current(player);
        if (current == null) {
            player.sendMessage(ChatColor.RED + "まだ選択範囲が設定されていません");
            return false;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "再生成するバージョンを指定してください (1.13, 1.14 など)");
            return false;
        }
        String version = args[0];
        Connection connection;
        try {
            connection = connectWildBlocksDatabase();
        } catch (Exception e) {
            getLogger().info("db への接続に失敗: e=" + e);
            return false;
        }
        World world = player.getWorld();
        World.Environment environment = world.getEnvironment();
        int dimension = 0;
        switch (environment) {
            case NETHER:
                dimension = -1;
                break;
            case THE_END:
                dimension = 1;
                break;
            default:
            case NORMAL:
                dimension = 0;
                break;
        }
        ResultSet resultSet;
        ReplaceOperation operation = new ReplaceOperation(world);
        try {
            Statement statement = connection.createStatement();
            resultSet = statement.executeQuery(""
                    + "select x, y, z, data from wild_blocks"
                    + "    inner join materials on wild_blocks.material_id = materials.id"
                    + "    inner join versions on wild_blocks.version_id = versions.id"
                    + "where "
                    + current.getMinX() + " <= x and x <= " + current.getMaxX()
                    + " and " + current.getMinY() + " <= y and y <= " + current.getMaxY()
                    + " and " + current.getMinZ() + " <= z and z <= " + current.getMaxZ()
                    + " and version = " + version
                    + " and dimension = " + dimension);
            while (resultSet.next()) {
                String data = resultSet.getString("data");
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                int z = resultSet.getInt("z");
                BlockData blockData = getServer().createBlockData(data);
                Block block = world.getBlockAt(x, y, z);
                if (!block.getBlockData().matches(blockData)) {
                    operation.register(new Loc(x, y, z), new ReplaceData(data));
                }
            }
            ReplaceOperation undo = operation.apply(player.getServer(), player.getWorld());
            undoOperationRegistry.push(player, undo);
            return true;
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "指定した範囲のブロック情報がまだありません");
            getLogger().warning(e.toString());
            return false;
        }
    }

    private boolean invalidGameMode(Player player) {
        GameMode current = player.getGameMode();
        return current != GameMode.CREATIVE && current != GameMode.SPECTATOR;
    }

    private static double parseX(String s, double defaultValue) {
        return parseCoordinate(s, defaultValue, 0.5);
    }

    private static double parseY(String s, double defaultValue) {
        return parseCoordinate(s, defaultValue, 0);
    }

    private static double parseZ(String s, double defaultValue) {
        return parseCoordinate(s, defaultValue, 0.5);
    }

    private static double parseCoordinate(String s, double defaultValue, double offset) {
        if ("~".equals(s)) {
            return defaultValue;
        }
        try {
            int v = Integer.parseInt(s);
            return v + offset;
        } catch (Exception ignored) {
        }
        return Double.parseDouble(s);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (invalidGameMode(player)) {
            return;
        }
        if (!e.hasItem()) {
            return;
        }
        ItemStack tool = e.getItem();
        if (tool == null) {
            return;
        }
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        if (invalidGameMode(player)) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack tool = inventory.getItemInMainHand();
        if (tool.getType() != Material.WOODEN_AXE) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
        EntityType entityType = e.getEntity().getType();
        if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            return;
        }
        if (entityType == EntityType.WANDERING_TRADER) {
            e.setCancelled(true);
        } else if (entityType == EntityType.TRADER_LLAMA) {
            e.setCancelled(true);
        } else {
            return;
        }
        getLogger().info("Cancel spawning " + entityType + "; reason=" + reason + "; location=" + e.getLocation());
    }
}
