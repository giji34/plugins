package com.github.giji34.t;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.InflaterInputStream;

public class Main extends JavaPlugin implements Listener {
    private static HashMap<UUID, HashMap<String, Landmark>> _knownLandmarks;
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
            HashMap<UUID, HashMap<String, Landmark>> landmarks = new HashMap<>();
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
                    continue;
                }
                if (!landmarks.containsKey(uid)) {
                    landmarks.put(uid, new HashMap<>());
                }
                landmarks.get(uid).put(name, new Landmark(new Vector(x, y, z), uid));
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

    static synchronized HashMap<String, Landmark> ensureKnownLandmarks(UUID uuid) {
        if (_knownLandmarks.containsKey(uuid)) {
            return new HashMap<>(_knownLandmarks.get(uuid));
        } else {
            return new HashMap<>();
        }
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
        PluginCommand gtree = getCommand("gtree");
        if (gtree != null) {
            gtree.setTabCompleter(new TreeTypeTabCompleter());
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
            case "gtree":
                return this.onTreeCommand(player, args);
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
        HashMap<String, Landmark> knownLandmarks = ensureKnownLandmarks(uuid);
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
        ReplaceOperation undo = operation.apply(player.getServer(), player.getWorld(), true);
        player.sendMessage(operation.count() + " 個の " + name + " ブロックを設置しました");
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
        ReplaceOperation undo = op.apply(player.getServer(), player.getWorld(), true);
        player.sendMessage(op.count() + " 個の " + fromName + " ブロックを " + toName + " に置き換えました");
        undoOperationRegistry.push(player, undo);
        return true;
    }

    @NotNull
    private ReplaceOperation replaceBlocks(Player player, SelectedBlockRange range, Material toMaterial, Function<Block, Boolean> predicate) {
        World world = player.getWorld();
        final ReplaceOperation operation = new ReplaceOperation(player.getWorld());
        final Server server = player.getServer();
        range.forEach(loc -> {
            Block block = world.getBlockAt(loc.x, loc.y, loc.z);
            BlockData toBlockData = server.createBlockData(toMaterial);
            String data = toBlockData.getAsString(true);
            if (toBlockData instanceof Leaves) {
                Leaves leaves = (Leaves)toBlockData;
                leaves.setPersistent(true);
                data = leaves.getAsString(true);
            }
            if (predicate.apply(block)) {
                operation.register(loc, new ReplaceData(data));
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
        undo.apply(player.getServer(), player.getWorld(), false);
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
        ReplaceOperation operation = new ReplaceOperation(world);
        ArrayList<String> palette = new ArrayList<>();
        File jar = getFile();
        File dir = new File(new File(new File(new File(jar.getParent(), "giji34"), "wildblocks"), version), Integer.toString(dimension));
        System.out.println(dir.toString());
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(dir, "palette.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                palette.add(line);
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "パレットの読み込みエラー");
            getLogger().warning(e.toString());
            return false;
        }
        int count = 0;
        try {
            int minChunkX = current.getMinX() >> 4;
            int maxChunkX = current.getMaxX() >> 4;
            int minChunkZ = current.getMinZ() >> 4;
            int maxChunkZ = current.getMaxZ() >> 4;
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                    File idx = new File(dir, "c." + chunkX + "." + chunkZ + ".idx");
                    if (!idx.exists()) {
                        player.sendMessage(ChatColor.RED + "指定した範囲のブロック情報がまだありません (" + idx.getName() + ")");
                        return true;
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
                                BlockData blockData = getServer().createBlockData(data);
                                Block block = world.getBlockAt(blockX, blockY, blockZ);
                                if (!block.getBlockData().matches(blockData)) {
                                    operation.register(new Loc(blockX, blockY, blockZ), new ReplaceData(data));
                                }
                                count++;
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
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "データベースの読み込みエラー");
            getLogger().warning(e.toString());
            e.printStackTrace();
            return false;
        }
        if (current.volume() == count) {
            ReplaceOperation undo = operation.apply(player.getServer(), player.getWorld(), false);
            undoOperationRegistry.push(player, undo);
        } else {
            player.sendMessage(ChatColor.RED + "指定した範囲のブロック情報がまだありません (" + count + " / " + current.volume() + ")");
        }
        return true;
    }

    private boolean onTreeCommand(Player player, String[] args) {
        Loc start = this.selectedBlockRangeRegistry.getStart(player);
        if (start == null) {
            player.sendMessage(ChatColor.RED + "まだ選択範囲が設定されていません");
            return false;
        }
        if (args.length < 1) {
            return false;
        }
        TreeType treeType;
        try {
            String name = args[0];
            treeType = TreeType.valueOf(name);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "未知の木の種類 \"" + getName() + "\" が指定されました");
            String availableTreeTypes = Stream.of(TreeType.values()).map(Enum::toString).collect(Collectors.joining(", "));
            player.sendMessage(ChatColor.RED + "指定可能な木の種類は " + availableTreeTypes + " です");
            return true;
        }
        int logLength = -1;
        if (args.length > 1) {
            try {
                logLength = Integer.parseInt(args[1], 10);
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "幹の長さは 0 以上の整数で指定する必要があります. 入力値: " + args[1]);
                return true;
            }
            if (logLength < 0) {
                player.sendMessage(ChatColor.RED + "幹の長さは 0 以上の整数で指定する必要があります. 入力値: " + args[1]);
            }
        }
        final World world = player.getWorld();
        final ReplaceOperation op = new ReplaceOperation(world);
        final int maxTry = 1000;
        for (int i = 0; i < maxTry; i++) {
            op.clear();
            HashMap<Material, Integer>[] materials = new HashMap[0];
            if (logLength >= 0) {
                materials = new HashMap[logLength + 1];
                for (int j = 0; j <= logLength; j++) {
                    materials[j] = new HashMap<>();
                }
            }
            final HashMap<Material, Integer>[] foundMaterials = materials;
            world.generateTree(new Location(world, start.x, start.y + 1, start.z), treeType, new BlockChangeDelegate() {
                @Override
                public boolean setBlockData(int x, int y, int z, @NotNull BlockData blockData) {
                    op.register(new Loc(x, y, z), new ReplaceData(blockData.getAsString(true)));
                    Material material = blockData.getMaterial();
                    int height = y - start.y - 1;
                    if (0 <= height && height < foundMaterials.length) {
                        Integer cnt = 0;
                        if (foundMaterials[height].containsKey(material)) {
                            cnt = foundMaterials[height].get(material);
                        }
                        cnt += 1;
                        foundMaterials[height].put(material, cnt);
                    }
                    return true;
                }

                @Override
                public @NotNull BlockData getBlockData(int x, int y, int z) {
                    return world.getBlockAt(x, y, z).getBlockData();
                }

                @Override
                public int getHeight() {
                    return world.getMaxHeight();
                }

                @Override
                public boolean isEmpty(int x, int y, int z) {
                    return world.getBlockAt(x, y, z).isEmpty();
                }
            });
            boolean ok = true;
            if (logLength >= 0) {
                for (int y = 0; y <= logLength; y++) {
                    int numLeaveLikeBlocks = 0;
                    int numLogLikeBlocks = 0;
                    HashMap<Material, Integer> found = foundMaterials[y];
                    for (Material material : found.keySet()) {
                        BlockData blockData = material.createBlockData();
                        String blockDataString = blockData.getAsString(true);
                        if (blockData instanceof Leaves) {
                            numLeaveLikeBlocks++;
                        } else if (blockDataString.contains("_stem") || blockDataString.contains("_log")){
                            numLogLikeBlocks++;
                        } else {
                            numLeaveLikeBlocks++;
                        }
                    }
                    if (y < logLength) {
                        if (numLeaveLikeBlocks > 0 || numLogLikeBlocks < 1) {
                            ok = false;
                            break;
                        }
                    } else {
                        if (numLeaveLikeBlocks < 1) {
                            ok = false;
                            break;
                        }
                    }
                }
            }
            if (!ok) {
                continue;
            }
            ReplaceOperation undo = op.apply(player.getServer(), world, true);
            undoOperationRegistry.push(player, undo);
            return true;
        }

        player.sendMessage(ChatColor.RED + "" + maxTry + "回木の生成を試みましたが幹の長さが " + logLength + " のものは生成できませんでした");
        return true;
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
            player.sendMessage(ChatColor.GRAY + range.start.toString() + " - " + range.end.toString() + " が選択されました (" + range.volume() + " ブロック)");
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
    public void onEntitySpawn(EntitySpawnEvent ese) {
        if (!(ese instanceof CreatureSpawnEvent)) {
            return;
        }
        CreatureSpawnEvent e = (CreatureSpawnEvent)ese;
        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
        EntityType entityType = e.getEntity().getType();
        if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            switch (entityType) {
                case WITHER:
                case ENDER_DRAGON:
                    e.setCancelled(true);
                    break;
            }
            return;
        }
        if (reason == CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION) {
            e.setCancelled(true);
            getLogger().info("村の襲撃: " + entityType + " のスポーンをキャンセルしました");
            return;
        }
        if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }
        switch (entityType) {
            case SQUID:
            case BAT:
            case COD:
            case DOLPHIN:
            case SALMON:
            case TROPICAL_FISH:
            case PUFFERFISH:
            case TURTLE:
            case SHEEP:
            case PIG:
            case COW:
            case HORSE:
            case OCELOT:
                break;
            case SKELETON:
            case ZOMBIE:
            case CREEPER:
            case ENDERMAN:
            case ZOMBIE_VILLAGER:
            case SPIDER:
            case CAVE_SPIDER:
            case WITCH:
            case CHICKEN:
            case PIG_ZOMBIE:
            case SLIME:
            case DROWNED:
            case MAGMA_CUBE:
            case GHAST:
            case WITHER:
            case BLAZE:
            case WITHER_SKELETON:
            case PHANTOM:
                e.setCancelled(true);
                break;
            case WANDERING_TRADER:
            case TRADER_LLAMA:
                getLogger().info("Cancel spawning " + entityType + "; reason=" + reason + "; location=" + e.getLocation());
                e.setCancelled(true);
                break;
            default:
                getLogger().info("Spawn " + entityType + "; reason=" + reason + "; location=" + e.getLocation());
                break;
        }
    }
}
