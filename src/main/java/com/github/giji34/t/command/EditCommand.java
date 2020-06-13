package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EditCommand {
    private SelectedBlockRangeRegistry selectedBlockRangeRegistry;
    private UndoOperationRegistry undoOperationRegistry;
    public static final String[] allMaterials;
    private static final int kMaxFillVolume = 4096;
    private static final HashSet<Material> kTreeMaterials = new HashSet<>();
    private final JavaPlugin owner;
    private File pluginDirectory;
    private String snapshotServerHost;
    private int snapshotServerPort;

    static {
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
        kTreeMaterials.add(Material.OAK_LOG);
        kTreeMaterials.add(Material.OAK_LEAVES);
        kTreeMaterials.add(Material.BIRCH_LOG);
        kTreeMaterials.add(Material.BIRCH_LEAVES);
        kTreeMaterials.add(Material.SPRUCE_LOG);
        kTreeMaterials.add(Material.SPRUCE_LEAVES);
        kTreeMaterials.add(Material.ACACIA_LOG);
        kTreeMaterials.add(Material.ACACIA_LEAVES);
        kTreeMaterials.add(Material.DARK_OAK_LOG);
        kTreeMaterials.add(Material.DARK_OAK_LEAVES);
        kTreeMaterials.add(Material.JUNGLE_LOG);
        kTreeMaterials.add(Material.JUNGLE_LEAVES);
    }

    public EditCommand(JavaPlugin owner) {
        this.owner = owner;
        selectedBlockRangeRegistry = new SelectedBlockRangeRegistry();
        undoOperationRegistry = new UndoOperationRegistry();
    }

    public void init(File pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
        File config = new File(pluginDirectory, "config.properties");
        try {
            FileInputStream fis = new FileInputStream(config);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("=");
                if (tokens.length != 2) {
                    continue;
                }
                String key = tokens[0];
                String value = tokens[1];
                if (key.equals("snapshotserver.host")) {
                    this.snapshotServerHost = value;
                } else if (key.equals("snapshotserver.port")) {
                    this.snapshotServerPort = Integer.parseInt(value, 10);
                }
            }
        } catch (Exception e) {
            owner.getLogger().warning("config.properties がありません");
        }
    }

    public boolean fill(Player player, String[] args) {
        BlockRange current = this.selectedBlockRangeRegistry.current(player);
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

    public boolean replace(Player player, String[] args) {
        BlockRange current = this.selectedBlockRangeRegistry.current(player);
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

    public boolean undo(Player player) {
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

    public boolean regenerate(Player player, String[] args) {
        BlockRange current = this.selectedBlockRangeRegistry.current(player);
        if (current == null) {
            player.sendMessage(ChatColor.RED + "まだ選択範囲が設定されていません");
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
        Snapshot snapshot = null;
        String info = "";
        if (args.length == 2) {
            String dateString = args[0] + " " + args[1];
            SnapshotServerClient client = new SnapshotServerClient(snapshotServerHost, snapshotServerPort);
            OffsetDateTime date = null;
            try {
                date = ParseDateString(dateString);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                player.sendMessage(ChatColor.RED + "日付のフォーマットが不正です(例: 2020-03-20 16:31");
                return true;
            }
            snapshot = client.getBackupSnapshot(date, dimension, current);
            info = dateString + "時点の状態";
        } else if (args.length == 1) {
            String version = args[0];
            final SnapshotServerClient client = new SnapshotServerClient(snapshotServerHost, snapshotServerPort);
            snapshot = client.getWildSnapshot(version, dimension, current);
            info = "バージョン" + version + "の初期状態";
        } else {
            player.sendMessage(ChatColor.RED + "コマンドの引数が不正です。/gregenerate <バージョン> または /gregenerate <yyyy-MM-dd hh:mm> として下さい");
            return false;
        }
        ReplaceOperation operation = new ReplaceOperation(world);
        final String error = snapshot.getErrorMessage();
        if (error != null) {
            player.sendMessage(ChatColor.RED + error);
            return true;
        }
        final Server server = owner.getServer();

        final Snapshot s = snapshot;
        final boolean ok = current.forEach(loc -> {
            BlockData bd = s.blockAt(loc, server);
            if (bd == null) {
                player.sendMessage(ChatColor.RED + "指定した範囲のブロック情報がまだありません (" + loc.toString() + ")");
                return false;
            }
            Block block = world.getBlockAt(loc.x, loc.y, loc.z);
            if (!block.getBlockData().matches(bd)) {
                operation.register(loc, new ReplaceData(bd.getAsString()));
            }
            return true;
        });
        if (ok) {
            ReplaceOperation undo = operation.apply(player.getServer(), player.getWorld(), false);
            undoOperationRegistry.push(player, undo);
            player.sendMessage(ChatColor.GRAY + "指定した範囲のブロックを" + info + "に戻しました");
        } else {
            player.sendMessage(ChatColor.RED + "指定した範囲のブロック情報がまだありません");
        }
        return true;
    }

    public boolean tree(Player player, String[] args) {
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
            player.sendMessage(ChatColor.RED + "未知の木の種類 \"" + args[0] + "\" が指定されました");
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
            boolean ok = world.generateTree(new Location(world, start.x, start.y + 1, start.z), treeType, new BlockChangeDelegate() {
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
            if (!ok) {
                continue;
            }
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

        player.sendMessage(ChatColor.RED + "" + maxTry + "回 " + treeType + " の生成を試みましたが幹の長さが " + logLength + " のものは生成できませんでした");
        return true;
    }

    public void setSelectionStartBlock(Player player, Loc loc) {
        BlockRange range = selectedBlockRangeRegistry.setStart(player, loc);
        if (range != null) {
            sendSelectionMessage(player, range);
        }
    }

    public void setSelectionEndBlock(Player player, Loc loc) {
        BlockRange range = selectedBlockRangeRegistry.setEnd(player, loc);
        if (range != null) {
            sendSelectionMessage(player, range);
        }
    }

    public boolean fellTrees(Player player) {
        BlockRange current = this.selectedBlockRangeRegistry.current(player);
        if (current == null) {
            player.sendMessage(ChatColor.RED + "まだ選択範囲が設定されていません");
            return false;
        }
        ReplaceOperation op = replaceBlocks(player, current, Material.AIR, (block) -> {
            Material m = block.getType();
            return kTreeMaterials.contains(m);
        });
        if (op.count() > kMaxFillVolume) {
            player.sendMessage(ChatColor.RED + "ブロックの個数が多すぎます ( " + op.count() + " / " + kMaxFillVolume + " )");
            return false;
        }
        ReplaceOperation undo = op.apply(player.getServer(), player.getWorld(), true);
        player.sendMessage(op.count() + " 個のブロックを air に置き換えました");
        undoOperationRegistry.push(player, undo);
        return true;
    }

    public boolean chunk(Player player) {
        Location location = player.getLocation();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        World world = player.getWorld();
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        this.selectedBlockRangeRegistry.setStart(player, new Loc(minX, 0, minZ));
        this.selectedBlockRangeRegistry.setEnd(player, new Loc(maxX, world.getMaxHeight() - 1, maxZ));
        return true;
    }

    @Nullable
    public BlockRange getCurrentSelection(Player player) {
        return selectedBlockRangeRegistry.current(player);
    }

    private void sendSelectionMessage(Player player, BlockRange range) {
        player.sendMessage(ChatColor.GRAY + range.start.toString() + " - " + range.end.toString() + " が選択されました (" + range.volume() + " ブロック)");
    }

    @NotNull
    private ReplaceOperation replaceBlocks(Player player, BlockRange range, Material toMaterial, Function<Block, Boolean> predicate) {
        World world = player.getWorld();
        final ReplaceOperation operation = new ReplaceOperation(player.getWorld());
        final Server server = player.getServer();
        range.forEach(loc -> {
            Block from = world.getBlockAt(loc.x, loc.y, loc.z);
            BlockData toBlockData = server.createBlockData(toMaterial);
            String data = toMaterial.getKey().toString();
            if (toBlockData instanceof Leaves) {
                data += "[persistent=true]";
            }
            data = MergeBlockData(from.getBlockData().getAsString(true), data, server);
            if (predicate.apply(from)) {
                operation.register(loc, new ReplaceData(data));
            }
            return true;
        });
        return operation;
    }

    private static HashMap<String, String> Properties(String blockData) {
        int begin = blockData.indexOf("[");
        int end = blockData.indexOf("]");
        HashMap<String, String> result = new HashMap<>();
        if (begin < 0 || end < 0) {
            return result;
        }
        String propsString = blockData.substring(begin + 1, end);
        String[] props = propsString.split(",");
        for (String prop : props) {
            String[] kv = prop.split("=");
            if (kv.length < 2) {
                continue;
            }
            result.put(kv[0], kv[1]);
        }
        return result;
    }

    private static String[] AvailableProperties(Material material, Server server) {
        String defaultBlockData = server.createBlockData(material).getAsString(false);
        HashMap<String, String> props = Properties(defaultBlockData);
        return props.keySet().toArray(new String[]{});
    }


    private static String MergeBlockData(String existing, String next, Server server) {
        HashMap<String, String> existingProps = Properties(existing);
        HashMap<String, String> nextProps = Properties(next);
        BlockData blockData = server.createBlockData(next);
        Material nextMaterial = blockData.getMaterial();
        String[] availableProps = AvailableProperties(nextMaterial, server);
        HashMap<String, String> resultProps = new HashMap<>();
        for (String key : existingProps.keySet()) {
            if (Arrays.asList(availableProps).contains(key)) {
                resultProps.put(key, existingProps.get(key));
            }
        }
        for (String key : nextProps.keySet()) {
            resultProps.put(key, nextProps.get(key));
        }
        String result = nextMaterial.getKey().toString();
        if (resultProps.size() > 0) {
            StringBuilder props = new StringBuilder();
            for (String key : resultProps.keySet()) {
                if (props.length() > 0) {
                    props.append(",");
                }
                props.append(key + "=" + resultProps.get(key));
            }
            result += "[" + props + "]";
        }
        return result;
    }

    private OffsetDateTime ParseDateString(String s) throws Exception {
        String[] tokens = s.split(" ");
        if (tokens.length != 2) {
            System.out.println("tokens.length=" + tokens.length);
            throw new Exception("invalid date format");
        }
        String ymdString = tokens[0];
        String hmString = tokens[1];
        String[] ymd = ymdString.split("-");
        if (ymd.length != 3) {
            System.out.println("ymd.length=" + ymd.length);
            throw new Exception("invalid date format");
        }
        String[] hm = hmString.split(":");
        if (hm.length != 2) {
            System.out.println("hm.length=" + hm.length);
            throw new Exception("invalid date format");
        }
        int year = Integer.parseInt(ymd[0], 10);
        int month = Integer.parseInt(ymd[1], 10);
        int day = Integer.parseInt(ymd[2], 10);
        int hour = Integer.parseInt(hm[0], 10);
        int minute = Integer.parseInt(hm[1], 10);
        ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());;
        return OffsetDateTime.of(year, month, day, hour, minute,0, 0, zoneOffset);
    }
}
