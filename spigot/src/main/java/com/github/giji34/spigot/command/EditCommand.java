package com.github.giji34.spigot.command;

import com.github.giji34.spigot.*;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
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
  private String snapshotServerHost;
  private int snapshotServerPort;
  private final DynmapSupport dynmap;
  private final BlockStateMapping mapping;

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

  public EditCommand(JavaPlugin owner, DynmapSupport dynmap, BlockStateMapping mapping) {
    this.owner = owner;
    this.dynmap = dynmap;
    this.mapping = mapping;
    selectedBlockRangeRegistry = new SelectedBlockRangeRegistry();
    undoOperationRegistry = new UndoOperationRegistry();
  }

  public void init(Config config) {
    this.snapshotServerHost = config.snapshotServerHost;
    this.snapshotServerPort = config.snapshotServerPort;
  }

  public boolean fill(Player player, String[] args) {
    BlockRange current = this.selectedBlockRangeRegistry.current(player);
    if (current == null) {
      player.sendMessage(ChatColor.RED + "まだ選択範囲が設定されていません");
      return false;
    }
    BlockData toBlockData = null;
    if (args.length == 0) {
      Block block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
      if (block == null) {
        player.sendMessage(ChatColor.RED + "fill したいブロックと同素材のブロックをターゲットするか、ブロック名を指定して下さい");
        return true;
      }
      toBlockData = block.getBlockData();
    } else if (args.length == 1) {
      String name = args[0];
      final Material material = Material.matchMaterial("minecraft:" + name);
      if (material == null) {
        player.sendMessage(ChatColor.RED + "ブロック名が正しくありません");
        return true;
      }
      toBlockData = player.getServer().createBlockData(material);
    }
    if (toBlockData == null) {
      owner.getLogger().warning("toBlockData が null. args.length=" + args.length);
      return false;
    }
    final String toBlockDataString = toBlockData.getAsString(false);
    ReplaceOperation operation = replaceBlocks(player, current, toBlockData, (block) -> !block.getBlockData().getAsString(false).equals(toBlockDataString));
    if (operation.count() > kMaxFillVolume) {
      player.sendMessage(ChatColor.RED + "ブロックの個数が多すぎます ( " + operation.count() + " / " + kMaxFillVolume + " )");
      return true;
    }
    ReplaceOperation undo = operation.apply(player.getServer(), player.getWorld(), true, this.dynmap);
    player.sendMessage(operation.count() + " 個の " + toBlockData.getAsString(true) + " ブロックを設置しました");
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
    BlockData toBlockData = player.getServer().createBlockData(toMaterial);
    ReplaceOperation op = replaceBlocks(player, current, toBlockData, (block) -> {
      if (!block.getType().equals(fromMaterial)) {
        return false;
      }
      return !block.getType().equals(toMaterial);
    });
    if (op.count() > kMaxFillVolume) {
      player.sendMessage(ChatColor.RED + "ブロックの個数が多すぎます ( " + op.count() + " / " + kMaxFillVolume + " )");
      return false;
    }
    ReplaceOperation undo = op.apply(player.getServer(), player.getWorld(), true, this.dynmap);
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
    undo.apply(player.getServer(), player.getWorld(), false, this.dynmap);
    return true;
  }

  public boolean regenerate(Player player, String[] args) {
    final BlockRange current = this.selectedBlockRangeRegistry.current(player);
    if (current == null) {
      player.getLocale();
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
    String info = "";
    Function<Integer, Snapshot> snapshotGetter = null;
    final int finalDimension = dimension;
    if (args.length == 2) {
      String dateString = args[0] + " " + args[1];
      final SnapshotServerClient client = new SnapshotServerClient(snapshotServerHost, snapshotServerPort);
      OffsetDateTime date = null;
      try {
        date = ParseDateString(dateString);
      } catch (Exception e) {
        System.out.println(e.getMessage());
        player.sendMessage(ChatColor.RED + "日付のフォーマットが不正です(例: 2020-03-20 16:31");
        return true;
      }
      final OffsetDateTime finalDate = date;
      snapshotGetter = (Integer unused) -> client.getBackupSnapshot(finalDate, finalDimension, current);
      info = dateString + "時点の状態";
    } else if (args.length == 1) {
      String version = args[0];
      final SnapshotServerClient client = new SnapshotServerClient(snapshotServerHost, snapshotServerPort);
      final String finalVersion = version;
      snapshotGetter = (Integer unused) -> client.getWildSnapshot(finalVersion, finalDimension, current);
      info = "バージョン" + version + "の初期状態";
    } else {
      player.sendMessage(ChatColor.RED + "コマンドの引数が不正です。/gregenerate <バージョン> または /gregenerate <yyyy-MM-dd hh:mm> として下さい");
      return false;
    }
    player.sendMessage(ChatColor.GRAY + "指定した範囲のブロック情報を取得しています...");
    final Server server = owner.getServer();
    final String finalInfo = info;
    final Function<Integer, Snapshot> finalSnapshotGetter = snapshotGetter;
    server.getScheduler().runTaskAsynchronously(owner, () -> {
      final Snapshot snapshot = finalSnapshotGetter.apply(0);
      server.getScheduler().runTask(owner, () -> {
        ReplaceOperation operation = new ReplaceOperation(world);
        final String snapshotErrorMessage = snapshot.getErrorMessage();
        if (snapshotErrorMessage != null) {
          player.sendMessage(ChatColor.RED + snapshotErrorMessage);
          return;
        }

        final Snapshot s = snapshot;
        final Optional<String> error = current.forEach(loc -> {
          String bd = s.blockAt(loc);
          if (bd == null) {
            return Optional.of("指定した範囲のブロック情報がまだありません (" + loc.toString() + ")");
          }
          Optional<Integer> chunkDataVersion = s.versionAt(loc);
          if (chunkDataVersion.isPresent()) {
            GameVersion version = GameVersion.fromChunkDataVersion(chunkDataVersion.get());
            bd = this.mapping.migrate(bd, version);
          }
          BlockData blockData = server.createBlockData(bd);

          Block block = world.getBlockAt(loc.x, loc.y, loc.z);
          String opBlock = null;
          if (!block.getBlockData().matches(blockData)) {
            opBlock = blockData.getAsString();
          }
          String opBiome = null;
          String biome = s.biomeAt(loc);
          if (biome != null) {
            Biome beforeBiome;
            try {
              beforeBiome = world.getBiome(loc.x, loc.y, loc.z);
            } catch (NoSuchMethodError e) {
              beforeBiome = world.getBiome(loc.x, loc.z);
            }
            Biome afterBiome = BiomeHelper.Resolve(biome, owner.getServer());
            if (afterBiome != null && beforeBiome != afterBiome) {
              opBiome = biome;
            }
          }
          if (opBlock != null || opBiome != null) {
            if (opBlock == null) {
              opBlock = block.getBlockData().getAsString(true);
            }
            operation.register(loc, new ReplaceData(opBlock, opBiome));
          }
          return Optional.empty();
        });
        if (error.isPresent()) {
          player.sendMessage(ChatColor.RED + error.get());
        } else {
          ReplaceOperation undo = operation.apply(player.getServer(), player.getWorld(), false, this.dynmap);
          undoOperationRegistry.push(player, undo);
          player.sendMessage(ChatColor.GRAY + "指定した範囲のブロックを" + finalInfo + "に戻しました");
        }
      });
    });
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
          op.register(new Loc(x, y, z), new ReplaceData(blockData.getAsString(true), null));
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
            } else if (blockDataString.contains("_stem") || blockDataString.contains("_log")) {
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
      ReplaceOperation undo = op.apply(player.getServer(), world, true, this.dynmap);
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
    BlockData air = player.getServer().createBlockData(Material.AIR);
    ReplaceOperation op = replaceBlocks(player, current, air, (block) -> {
      Material m = block.getType();
      return kTreeMaterials.contains(m);
    });
    if (op.count() > kMaxFillVolume) {
      player.sendMessage(ChatColor.RED + "ブロックの個数が多すぎます ( " + op.count() + " / " + kMaxFillVolume + " )");
      return false;
    }
    ReplaceOperation undo = op.apply(player.getServer(), player.getWorld(), true, this.dynmap);
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
  private ReplaceOperation replaceBlocks(Player player, BlockRange range, BlockData toBlockData, Function<Block, Boolean> predicate) {
    World world = player.getWorld();
    final ReplaceOperation operation = new ReplaceOperation(player.getWorld());
    final Server server = player.getServer();
    range.forEach(loc -> {
      Block from = world.getBlockAt(loc.x, loc.y, loc.z);
      String data = toBlockData.getAsString(true);
      data = BlockPropertyHelper.MergeBlockData(from.getBlockData().getAsString(true), data, server);
      if (toBlockData instanceof Leaves) {
        data = BlockPropertyHelper.MergeBlockData(data, toBlockData.getMaterial().getKey().toString() + "[persistent=true]", server);
      }
      if (predicate.apply(from)) {
        operation.register(loc, new ReplaceData(data, null));
      }
      return Optional.empty();
    });
    return operation;
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
    ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    ;
    return OffsetDateTime.of(year, month, day, hour, minute, 0, 0, zoneOffset);
  }

  public boolean kusa(final Player player) {
    BlockRange current = this.selectedBlockRangeRegistry.current(player);
    if (current == null) {
      player.sendMessage(ChatColor.RED + "まだ選択範囲が設定されていません");
      return false;
    }
    final Server server = player.getServer();
    final World world = player.getWorld();
    final ReplaceOperation operation = new ReplaceOperation(world);
    final String grassBlock = server.createBlockData(Material.GRASS_BLOCK).getAsString();
    final String snowyGrassBlock = server.createBlockData(Material.GRASS_BLOCK, "[snowy=true]").getAsString();
    current.forEach((Loc loc) -> {
      Block block = world.getBlockAt(loc.x, loc.y, loc.z);
      if (block.getType() != Material.DIRT) {
        return Optional.empty();
      }
      Block upper = world.getBlockAt(loc.x, loc.y + 1, loc.z);
      if (!upper.getType().isTransparent()) {
        return Optional.empty();
      }
      if (upper.getType() == Material.SNOW) {
        operation.register(loc, new ReplaceData(snowyGrassBlock, null));
      } else {
        operation.register(loc, new ReplaceData(grassBlock, null));
      }
      return Optional.empty();
    });
    if (operation.count() == 0) {
      return true;
    }
    ReplaceOperation undo = operation.apply(server, world, true, this.dynmap);
    undoOperationRegistry.push(player, undo);
    return true;
  }

  public static boolean TallSeaGrass(Player player, Loc loc) {
    final World world = player.getWorld();
    Block target = world.getBlockAt(loc.x, loc.y, loc.z);
    if (target.getType() == Material.SEAGRASS || target.getType() == Material.TALL_SEAGRASS) {
      return false;
    }
    Block lower = world.getBlockAt(loc.x, loc.y + 1, loc.z);
    Block upper = world.getBlockAt(loc.x, loc.y + 2, loc.z);
    final Server server = player.getServer();
    if (lower.getType() != Material.WATER) {
      return false;
    }
    if (upper.getType() != Material.WATER) {
      return false;
    }
    BlockData tallSeaGrassUpper = server.createBlockData(Material.TALL_SEAGRASS, "[half=upper]");
    BlockData tallSeaGrassLower = server.createBlockData(Material.TALL_SEAGRASS, "[half=lower]");
    lower.setBlockData(tallSeaGrassLower, false);
    upper.setBlockData(tallSeaGrassUpper, true);
    return true;
  }

  public void forget(Player player) {
    this.selectedBlockRangeRegistry.forget(player);
    this.undoOperationRegistry.forget(player);
  }
}
