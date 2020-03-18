package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EditCommand {
    private SelectedBlockRangeRegistry selectedBlockRangeRegistry;
    private UndoOperationRegistry undoOperationRegistry;
    public static final String[] allMaterials;
    private static final int kMaxFillVolume = 4096;
    private final JavaPlugin owner;
    private File pluginDirectory;

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
    }

    public EditCommand(JavaPlugin owner) {
        this.owner = owner;
        selectedBlockRangeRegistry = new SelectedBlockRangeRegistry();
        undoOperationRegistry = new UndoOperationRegistry();
    }

    public void init(File pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
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
        final LocalSnapshotReader reader = new LocalSnapshotReader(pluginDirectory);
        final Snapshot snapshot = reader.read(version, dimension, current);
        final String error = snapshot.getErrorMessage();
        if (error != null) {
            player.sendMessage(ChatColor.RED + error);
            return true;
        }
        final Server server = owner.getServer();

        final boolean ok = current.forEach(loc -> {
            BlockData bd = snapshot.blockAt(loc, server);
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
}
