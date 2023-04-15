package com.github.giji34.plugins.spigot;

import com.github.giji34.plugins.shared.ChannelNames;
import com.github.giji34.plugins.spigot.command.*;
import com.github.giji34.plugins.spigot.controller.ControllerService;
import com.github.giji34.plugins.spigot.controller.ReservedSpawnLocation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {
  private final ToggleGameModeCommand toggleGameModeCommand = new ToggleGameModeCommand();
  private final TeleportCommand teleportCommand = new TeleportCommand(this);
  private EditCommand editCommand;
  private final PortalCommand portalCommand = new PortalCommand(this);
  private Permission permission;
  private MobSpawnProhibiter mobSpawnProhibiter;
  private Borders borders;
  private Hibernate hibernate;
  private BlockStateMapping blockStateMapping;
  private final Config config;

  private static final int kPlayerIdleTimeoutMinutes = 10;
  private BukkitTask playerActivityWatchdog;
  private HashMap<UUID, LocalDateTime> playerActivity = new HashMap<>();
  private ControllerService controllerService;

  private final DebugStick debugStick = new DebugStick();
  private @Nullable BackupService backupService;

  public Main() {
    File jar = getFile();
    File pluginDirectory = new File(jar.getParent(), "giji34");

    config = Config.Load(getLogger(), pluginDirectory);
  }

  @Override
  public void onLoad() {
    try {
      File jar = getFile();
      File pluginDirectory = new File(jar.getParent(), "giji34");
      this.permission = new Permission(new File(pluginDirectory, "permission.yml"));
      this.teleportCommand.init(pluginDirectory);
      this.blockStateMapping = new BlockStateMapping(this);
      this.editCommand = new EditCommand(this, this.blockStateMapping);
      this.editCommand.init(config);
      this.portalCommand.init(pluginDirectory);
      this.mobSpawnProhibiter = new MobSpawnProhibiter(new File(pluginDirectory, "mob_spawn_allowed_regions.yml"), this);
      this.borders = new Borders(new File(pluginDirectory, "borders.yml"));
      this.hibernate = new Hibernate(this);
      this.controllerService = new ControllerService(this, this.config.rpcPort);
      if (new File(config.gbackupToolDirectory).isDirectory() && new File(config.gbackupGitDirectory).isDirectory()) {
        this.backupService = new BackupService(config.gbackupToolDirectory, config.gbackupGitDirectory, this);
      } else {
        getLogger().info("backup service is disabled because backup directory is not configured.");
      }
    } catch (Exception e) {
      getLogger().warning("error: " + e);
    }
    startPortalService();
  }

  private void reload() {
    File jar = getFile();
    File pluginDirectory = new File(jar.getParent(), "giji34");
    this.permission = new Permission(new File(pluginDirectory, "permission.yml"));
    try {
      this.teleportCommand.reload();
    } catch (Exception e) {
      getLogger().warning("reload failed: " + e.getMessage());
    }
    this.portalCommand.reload();
    this.mobSpawnProhibiter = new MobSpawnProhibiter(new File(pluginDirectory, "mob_spawn_allowed_regions.yml"), this);
    File dynmap = new File(pluginDirectory.getParentFile(), "dynmap");
    this.borders = new Borders(new File(dynmap, "markers.yml"));
    Server server = getServer();
    CommandSender console = server.getConsoleSender();
    server.dispatchCommand(console, "whitelist reload");
  }

  @Override
  public void onEnable() {
    PluginCommand tpb = getCommand("tpb");
    if (tpb != null) {
      tpb.setTabCompleter(new TeleportLandmarkTabCompleter(teleportCommand, 0));
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
    PluginCommand guide = getCommand("guide");
    if (guide != null) {
      guide.setTabCompleter(new TeleportLandmarkTabCompleter(teleportCommand, 1));
    }
    PluginCommand connect = getCommand("connect");
    if (connect != null) {
      connect.setTabCompleter(new StringListTabCompleter(new String[]{"2434_main", "2434_world06", "hololive_01", "en_hololive", "hololive_00", "id_hololive", "hololive_sports_festival_2022", "hololive_02"}));
    }
    PluginCommand clone = getCommand("/clone");
    if (clone != null) {
      clone.setTabCompleter(new LookingAtTabCompleter());
    }
    PluginCommand hotbar = getCommand("/");
    if (hotbar != null) {
      hotbar.setTabCompleter(new BlockNameTabCompleter());
    }
    getServer().getMessenger().registerOutgoingPluginChannel(this, ChannelNames.kSpigotPluginChannel);
    getServer().getPluginManager().registerEvents(this, this);

    if (backupService == null) {
      hibernate.enable();
    } else {
      backupService.onEnable();
    }

    this.startPlayerActivityWatchdog();
    this.setupGameRules();
  }

  @Override
  public void onDisable() {
    hibernate.disable();
    this.stopPlayerActivityWatchdog();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (sender instanceof ConsoleCommandSender) {
      switch (label) {
        case "giji34":
          this.handleAdminCommand(sender, args);
          return true;
        case "hibernate":
          return this.handleHibernate(sender, args);
      }
    }
    if (!(sender instanceof Player)) {
      return false;
    }
    Player player = (Player) sender;
    if (invalidGameMode(player)) {
      return false;
    }
    if (!this.permission.hasPermission(player, label)) {
      player.sendMessage(ChatColor.RED + label + "コマンドを実行する権限がありません");
      return true;
    }
    switch (label) {
      case "tpl":
        return teleportCommand.teleport(player, args);
      case "tpb":
        return teleportCommand.teleportToLandmark(player, args);
      case "gm":
        toggleGameModeCommand.toggle(player);
        return true;
      case "gfill":
        return editCommand.fill(player, args);
      case "greplace":
        return editCommand.replace(player, args);
      case "gundo":
        return editCommand.undo(player);
      case "gregenerate":
        return editCommand.regenerate(player, args);
      case "gtree":
        return editCommand.tree(player, args);
      case "guide":
        return teleportCommand.guide(player, this.borders, args);
      case "follow":
        return teleportCommand.follow(player, args);
      case "create_inter_server_portal":
        return portalCommand.createInterServerPortal(player, args, editCommand);
      case "create_intra_server_portal":
        return portalCommand.createIntraServerPortal(player, args, editCommand);
      case "delete_portal":
        return portalCommand.delete(player, args);
      case "fell_trees":
        return editCommand.fellTrees(player);
      case "/chunk":
        return editCommand.chunk(player);
      case "uuid":
        World world = player.getWorld();
        getLogger().info(world.getName() + ":" + world.getUID().toString());
        return true;
      case "connect":
        return this.handleConnectCommand(player, args);
      case "kusa":
        return editCommand.kusa(player);
      case "giji34":
        return this.handleAdminCommand(player, args);
      case "/clone":
        return this.handleCloneCommand(player, args);
      case "hibernate":
        return this.handleHibernate(player, args);
      case "/":
        return this.handleHotbarCommand(player, args, command);
      default:
        return false;
    }
  }

  private boolean invalidGameMode(Player player) {
    GameMode current = player.getGameMode();
    return current != GameMode.CREATIVE && current != GameMode.SPECTATOR;
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent e) {
    Player player = e.getPlayer();
    Action action = e.getAction();
    if (action == Action.RIGHT_CLICK_AIR && e.hasItem() && player.getGameMode() == GameMode.ADVENTURE) {
      ItemStack itemStack = e.getItem();
      if (itemStack != null && itemStack.getType() == Material.FIREWORK_ROCKET) {
        ItemStack maybeElytra = player.getInventory().getChestplate();
        if (maybeElytra != null && maybeElytra.getType() == Material.ELYTRA) {
          Repair(maybeElytra);
        }
      }
    }

    if (!this.permission.hasRole(player, "member")) {
      return;
    }
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
    Material toolType = tool.getType();
    if (toolType == Material.WOODEN_AXE) {
      Block block = e.getClickedBlock();
      if (block == null) {
        return;
      }
      EquipmentSlot hand = e.getHand();
      if (hand != EquipmentSlot.HAND) {
        return;
      }
      Loc loc = Loc.fromVectorFloored(block.getLocation().toVector());
      if (action == Action.LEFT_CLICK_BLOCK) {
        editCommand.setSelectionStartBlock(player, loc);
      } else if (action == Action.RIGHT_CLICK_BLOCK) {
        editCommand.setSelectionEndBlock(player, loc);
      } else {
        return;
      }
      e.setCancelled(true);
    } else if (toolType == Material.SEAGRASS) {
      Block block = e.getClickedBlock();
      if (block == null) {
        return;
      }
      EquipmentSlot hand = e.getHand();
      if (hand != EquipmentSlot.HAND) {
        return;
      }
      if (action != Action.LEFT_CLICK_BLOCK) {
        return;
      }
      Loc loc = Loc.fromVectorFloored(block.getLocation().toVector());
      if (EditCommand.TallSeaGrass(player, loc)) {
        e.setCancelled(true);
      }
    } else if (toolType == Material.DEBUG_STICK) {
      if (player.isOp()) {
        return;
      }
      Block block = e.getClickedBlock();
      if (block == null) {
        return;
      }
      if (action == Action.LEFT_CLICK_BLOCK) {
        e.setCancelled(true);
        this.debugStick.onInteractWithMainHand(player, block);
      } else if (action == Action.RIGHT_CLICK_BLOCK) {
        e.setCancelled(true);
        this.debugStick.onInteractWithOffHand(player, block);
      }
    }
  }

  static void Repair(@NotNull ItemStack item) {
    ItemMeta itemMeta = item.getItemMeta();
    if (itemMeta instanceof Damageable) {
      Damageable damageable = (Damageable) itemMeta;
      damageable.setDamage(0);
      item.setItemMeta(itemMeta);
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
  public void onCreatureSpawnSpawn(CreatureSpawnEvent e) {
    CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
    LivingEntity entity = e.getEntity();
    EntityType entityType = entity.getType();
    switch (reason) {
      case SPAWNER_EGG:
        switch (entityType) {
          case WITHER:
          case ENDER_DRAGON:
            e.setCancelled(true);
            break;
        }
        return;
      case VILLAGE_INVASION:
        e.setCancelled(true);
        getLogger().info("村の襲撃: " + entityType + " のスポーンをキャンセルしました");
        return;
      case NATURAL:
        if (this.mobSpawnProhibiter.isMobSpawnAllowed(e.getLocation())) {
          switch (entityType) {
            case WANDERING_TRADER:
            case TRADER_LLAMA:
              // 作業の邪魔なのでスポーンを阻止する
              getLogger().info("行商人: " + entityType + " のスポーンをキャンセルしました");
              e.setCancelled(true);
              break;
          }
        } else {
          e.setCancelled(true);
        }
        return;
      case BUILD_WITHER:
        e.setCancelled(true);
        return;
      case DEFAULT:
        if (entityType == EntityType.CAT) {
          e.setCancelled(true);
        } else {
          System.out.println(entityType + " spawned with reason: " + reason + " at " + entity.getLocation());
        }
        return;
    }
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    this.applyPortalTeleport(player);
    this.correctPlayerLocation(player);
    this.updatePlayerActivity(player);
  }

  private void applyPortalTeleport(Player player) {
    Portal portal = portalCommand.filterPortalByCooldown(player, portalCommand.findPortal(player));
    if (portal == null) {
      return;
    }
    portalCommand.markPortalUsed(player, portal);
    portal.apply(player, this);
  }

  private void correctPlayerLocation(Player player) {
    if (this.permission.hasRole(player, "member")) {
      return;
    }
    this.borders.correct(player);
  }

  private boolean isSightseeingServer() {
    return config.isSightSeeing;
  }

  private void updatePlayerActivity(Player player) {
    UUID uuid = player.getUniqueId();
    if (this.permission.hasRole(player, "member") && isSightseeingServer()) {
      this.playerActivity.remove(uuid);
    } else {
      LocalDateTime now = LocalDateTime.now();
      this.playerActivity.put(uuid, now);
    }
  }

  static final String[] kCanPlaceOnRails = {
    "minecraft:powered_rail",
    "minecraft:detector_rail",
    "minecraft:activator_rail",
    "minecraft:rail",
  };

  private static List<String> GetMissingRails(ItemStack itemStack) {
    List<String> canPlaceOn = ItemStackHelper.GetCanPlaceOn(itemStack);
    ArrayList<String> rails = new ArrayList<>();
    for (String rail : kCanPlaceOnRails) {
      if (!canPlaceOn.contains(rail)) {
        rails.add(rail);
      }
    }
    return rails;
  }

  private static String TagCanPlaceOn(List<String> canPlaceOnRails) {
    String rails = String.join(",", canPlaceOnRails.stream().map(t -> "\"" + t + "\"").collect(Collectors.toList()));
    return "CanPlaceOn:[" + rails + "]";
  }

  private static String ItemStringMinecart(List<String> canPlaceOnRails) {
    String s = "minecraft:minecart";
    if (!canPlaceOnRails.isEmpty()) {
      s += "{" + TagCanPlaceOn(canPlaceOnRails) + "}";
    }
    return s;
  }

  @EventHandler
  public void onEntitySpawn(EntitySpawnEvent e) {
    if (e.getEntityType() != EntityType.DROPPED_ITEM) {
      return;
    }
    Item item = (Item) e.getEntity();
    ItemStack itemStack = item.getItemStack();
    Material material = itemStack.getType();
    Server server = getServer();
    UUID id = e.getEntity().getUniqueId();

    if (material == Material.MINECART) {
      List<String> rails = GetMissingRails(itemStack);
      if (rails.isEmpty()) {
        return;
      }
      server.getScheduler().runTask(this, () -> {
        ConsoleCommandSender console = server.getConsoleSender();
        String command = "data merge entity " + id + " {Item:{tag:{" + TagCanPlaceOn(rails) + "}}}";
        server.dispatchCommand(console, command);
      });
    } else if (material.name().startsWith("MUSIC_DISC_")) {
      server.getScheduler().runTask(this, () -> {
        ConsoleCommandSender console = server.getConsoleSender();
        String command = "data merge entity " + id + " {Item:{tag:{CanPlaceOn:[\"minecraft:jukebox\"]}}}";
        server.dispatchCommand(console, command);
      });
    }
  }

  @EventHandler
  public void onPlayerJoined(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    addPotionEffects(player);
    notifyOp(player);
    this.playerActivity.put(player.getUniqueId(), LocalDateTime.now());
    if (this.backupService != null) {
      this.backupService.onPlayerJoin();
    }

    if (this.permission.hasRole(player, "member")) {
      portalCommand.setAnyPortalCooldown(player);
    } else {
      Server server = player.getServer();
      server.getScheduler().runTaskLater(this, () -> {
        PlayerInventory inventory = player.getInventory();
        boolean hasDiamondSword = false;
        boolean hasBoat = false;
        for (int i = 0; i < inventory.getSize(); i++) {
          ItemStack itemStack = inventory.getItem(i);
          if (itemStack == null) {
            continue;
          }
          Material material = itemStack.getType();
          if (material == Material.DIAMOND_SWORD) {
            Repair(itemStack);
            hasDiamondSword = true;
          } else if (MaterialHelper.isBoat(material)) {
            hasBoat = true;
          }
        }
        if (!hasDiamondSword) {
          inventory.addItem(new ItemStack(Material.DIAMOND_SWORD));
        }
        if (!hasBoat) {
          inventory.addItem(new ItemStack(Material.OAK_BOAT));
        }
        ConsoleCommandSender console = server.getConsoleSender();
        ItemStack offHand = inventory.getItemInOffHand();
        if (inventory.contains(Material.MINECART) || (offHand.getType() == Material.MINECART)) {
          ItemStack[] storageContents = inventory.getStorageContents();
          for (int i = 0; i < storageContents.length; i++) {
            ItemStack itemStack = storageContents[i];
            if (itemStack == null) {
              continue;
            }
            if (itemStack.getType() != Material.MINECART) {
              continue;
            }
            List<String> rails = GetMissingRails(itemStack);
            if (!rails.isEmpty()) {
              String category = i < 9 ? "hotbar" : "inventory";
              int offset = i < 9 ? 0 : 9;
              String command = "item replace entity " + player.getUniqueId() + " " + category + "." + (i - offset) + " with " + ItemStringMinecart(rails);
              server.dispatchCommand(console, command);
            }
          }

          if (offHand.getType() == Material.MINECART) {
            List<String> rails = GetMissingRails(offHand);
            if (!rails.isEmpty()) {
              String command = "item replace entity " + player.getUniqueId() + " weapon.offhand with " + ItemStringMinecart(rails);
              server.dispatchCommand(console, command);
            }
          }
        } else {
          server.dispatchCommand(console, "give " + player.getName() + " " + ItemStringMinecart(Arrays.asList(kCanPlaceOnRails.clone())));
        }
      }, 40);
    }
  }

  @EventHandler
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    final Player player = event.getPlayer();
    player.getServer().getScheduler().runTaskLater(this, () -> {
      addPotionEffects(player);
    }, 40);
  }

  private void addPotionEffects(Player player) {
    ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
    int duration = 7 * 24 * 60 * 60 * 20;
    int amplifier = 0;
    boolean ambient = false;
    boolean particles = false;
    effects.add(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, amplifier, ambient, particles));
    effects.add(new PotionEffect(PotionEffectType.SATURATION, duration, amplifier, ambient, particles));
    effects.add(new PotionEffect(PotionEffectType.HEAL, duration, amplifier, ambient, particles));
    effects.add(new PotionEffect(PotionEffectType.WATER_BREATHING, duration, amplifier, ambient, particles));
    if (!player.addPotionEffects(effects)) {
      getLogger().warning("failed adding portion effects for player: " + player.getName());
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    this.playerActivity.remove(player.getUniqueId());
    this.borders.forget(player);
    this.editCommand.forget(player);
    this.debugStick.forget(player);
  }

  private boolean handleConnectCommand(Player player, String[] args) {
    if (args.length != 1) {
      player.sendMessage(ChatColor.RED + "接続先を指定してください. (例) /connect 2434_main");
      return false;
    }
    String destination = args[0];
    if (destination.equals("2434_main")) {
      destination = "main";
    } else if (destination.equals("2434_world06")) {
      destination = "world06";
    }
    try {
      InterServerPortal.Connect(player, destination, this);
    } catch (Exception e) {
      player.sendMessage(ChatColor.RED + args[0] + " に接続できませんでした");
      getLogger().warning(player.getName() + " failed connecting server " + destination);
      return false;
    }
    player.sendMessage(args[0] + " に接続しています...");
    return true;
  }

  private boolean handleAdminCommand(CommandSender sender, String[] args) {
    if (args.length < 1) {
      return false;
    }
    Player player = null;
    if (sender instanceof Player) {
      player = (Player) sender;
      if (!this.permission.hasRole(player, "admin")) {
        return false;
      }
      if (!player.isOp()) {
        return false;
      }
    }
    String subCommand = args[0];
    switch (subCommand) {
      case "reload": {
        this.reload();
        if (player != null) {
          player.sendMessage("reload しました");
        }
        return true;
      }
      default:
        return false;
    }
  }

  private boolean handleCloneCommand(Player player, String[] args) {
    for (int i = 0; i < args.length; i++) {
      try {
        Integer.parseInt(args[i], 10);
      } catch (Exception e) {
        player.sendMessage(ChatColor.RED + "座標の値が不正です: " + args[i]);
        return false;
      }
    }
    if (args.length == 3 || args.length == 6) {
      return true;
    }
    if (args.length != 9) {
      player.sendMessage(ChatColor.RED + "引数の個数が" + args.length + "個になっています. 正しくは 9 個です");
      return false;
    }
    String command = "clone " + String.join(" ", args);
    return runCommandAsTemporaryCommandSender(player, command);
  }

  private boolean runCommandAsTemporaryCommandSender(Player player, String commandString) {
    Server server = getServer();
    CommandSender console = server.getConsoleSender();
    Optional<World> maybeOverworld = server.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.NORMAL).findFirst();
    if (maybeOverworld.isEmpty()) {
      return false;
    }
    World overworld = maybeOverworld.get();
    overworld.loadChunk(0, 0);
    overworld.setChunkForceLoaded(0, 0, true);
    UUID uuid = UUID.randomUUID();
    if (!server.dispatchCommand(console, "summon command_block_minecart 0 -66 0 {CustomName:\"\\\"" + uuid + "\\\"\",NoGravity:1b,Invisible:1b}")) {
      player.sendMessage(ChatColor.RED + "コマンドが失敗しました(1)");
    }
    // command_block_minecart doesn't tick. Summon armor_stand here in order to tick the force loaded chunk.
    if (!server.dispatchCommand(console, "summon armor_stand 0 -66 0 {CustomName:\"\\\"" + uuid + "\\\"\",NoGravity:1b,Invisible:1b}")) {
      player.sendMessage(ChatColor.RED + "コマンドが失敗しました(2)");
    }
    if (!server.dispatchCommand(console, "tp @e[name=" + uuid + "] " + player.getName())) {
      player.sendMessage(ChatColor.RED + "コマンドが失敗しました(3)");
    }
    overworld.setChunkForceLoaded(0, 0, false);
    String command = "execute as @e[name=" + uuid + "] run " + commandString;
    try {
      if (!server.dispatchCommand(console, command)) {
        player.sendMessage(ChatColor.RED + "コマンドが失敗しました(4)");
      }
    } catch (CommandException e) {
      player.sendMessage(ChatColor.RED + e.getLocalizedMessage());
    }

    World world = player.getWorld();
    Optional<Entity> commandSender = world.getNearbyEntities(player.getLocation(), 5, 5, 5, e -> {
      String name = e.getCustomName();
      if (name == null) {
        return false;
      }
      return name.equals(uuid.toString());
    }).stream().findFirst();
    if (commandSender.isPresent()) {
      commandSender.get().remove();
    }
    return true;
  }

  @EventHandler
  public void onEntityPotionEffect(EntityPotionEffectEvent e) {
    EntityPotionEffectEvent.Cause cause = e.getCause();
    if (cause != EntityPotionEffectEvent.Cause.BEACON) {
      return;
    }
    Entity entity = e.getEntity();
    if (e.getEntityType() != EntityType.PLAYER) {
      return;
    }
    if (!(entity instanceof Player)) {
      return;
    }
    Player player = (Player) entity;
    if (!this.permission.hasRole(player, "member")) {
      return;
    }
    GameMode gameMode = player.getGameMode();
    if (gameMode != GameMode.CREATIVE) {
      return;
    }
    e.setCancelled(true);
  }

  private void setupGameRules() {
    if (!isSightseeingServer()) {
      return;
    }
    Server server = this.getServer();
    for (World world : server.getWorlds()) {
      world.setGameRule(GameRule.DISABLE_RAIDS, true);
      TrySetGameRule(world, "doInsomnia", false);
      TrySetGameRule(world, "doPatrolSpawning", false);
      TrySetGameRule(world, "doTraderSpawning", false);
      world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
      world.setThundering(false);
      world.setStorm(false);
      world.setGameRule(GameRule.KEEP_INVENTORY, true);
      world.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
    }
    server.setDefaultGameMode(GameMode.ADVENTURE);
    server.setIdleTimeout(0);
  }

  private static <T> void TrySetGameRule(World world, String gameRule, T value) {
    GameRule<?> rule = GameRule.getByName(gameRule);
    if (rule == null) {
      return;
    }
    world.setGameRule((GameRule<T>) rule, value);
  }

  private void startPlayerActivityWatchdog() {
    this.playerActivity.clear();
    this.playerActivityWatchdog = getServer().getScheduler().runTaskTimer(this, () -> {
      final Server server = getServer();
      final LocalDateTime now = LocalDateTime.now();
      for (Player player : server.getOnlinePlayers()) {
        UUID uuid = player.getUniqueId();
        LocalDateTime lastActive = this.playerActivity.get(uuid);
        if (lastActive == null) {
          continue;
        }
        long minutes = ChronoUnit.MINUTES.between(lastActive, now);
        if (minutes >= kPlayerIdleTimeoutMinutes) {
          this.playerActivity.remove(uuid);
          player.kickPlayer("You have been kicked for idling too long");
        }
      }
    }, 0, 5 * 20);
  }

  private void stopPlayerActivityWatchdog() {
    if (this.playerActivityWatchdog != null) {
      getServer().getScheduler().cancelTask(this.playerActivityWatchdog.getTaskId());
      this.playerActivityWatchdog = null;
    }
  }

  private boolean handleHibernate(CommandSender sender, String[] args) {
    if (args.length != 1) {
      return false;
    }
    String arg = args[0];
    if (arg.equals("on")) {
      if (hibernate.enable()) {
        sender.sendMessage("hibernate を有効にしました");
      } else {
        sender.sendMessage(ChatColor.RED + "hibernate が既に有効です");
      }
      return true;
    } else if (arg.equals("off")) {
      if (hibernate.disable()) {
        sender.sendMessage("hibernate を無効にしました");
      } else {
        sender.sendMessage("hibernate が既に無効です");
      }
      return true;
    } else {
      return false;
    }
  }

  private void notifyOp(Player player) {
    if (!player.isOp()) {
      return;
    }
    try {
      unsafeNotifyOp(player);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void unsafeNotifyOp(Player player) throws Exception {
    for (FileStore store : FileSystems.getDefault().getFileStores()) {
      String str = store.toString();
      String mountPoint = Arrays.stream(config.monitoringFilesystemMountPoints).filter((it) -> str.startsWith(it + " (")).findFirst().orElse(null);
      if (mountPoint == null) {
        continue;
      }
      long usable = store.getUsableSpace();
      long total = store.getTotalSpace();
      player.sendMessage(ChatColor.AQUA + mountPoint);
      player.sendMessage("  " + StringFromBytes(usable) + " / " + StringFromBytes(total) + " (" + String.format("%.1f", 100.0 * usable / total) + "%)");
    }
  }

  private static String StringFromBytes(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KiB", (bytes / 1024.0));
    } else if (bytes < 1024 * 1024 * 1024) {
      return String.format("%.1f MiB", (bytes / (1024.0 * 1024.0)));
    } else {
      return String.format("%.1f GiB", (bytes / (1024.0 * 1024.0 * 1024.0)));
    }
  }

  private void startPortalService() {
    this.controllerService.start();
  }

  @EventHandler
  public void onPlayerSpawnLocation(PlayerSpawnLocationEvent e) {
    Player player = e.getPlayer();
    UUID uuid = player.getUniqueId();
    Optional<ReservedSpawnLocation> maybeLocation = this.controllerService.drainReservedSpawnLocation(uuid);
    if (maybeLocation.isEmpty()) {
      return;
    }
    ReservedSpawnLocation location = maybeLocation.get();
    Optional<World> spawnWorld = getServer().getWorlds().stream().filter((world) -> {
      World.Environment env = world.getEnvironment();
      switch (location.dimension) {
        case 0:
          return env == World.Environment.NORMAL;
        case 1:
          return env == World.Environment.THE_END;
        case -1:
          return env == World.Environment.NETHER;
        default:
          return false;
      }
    }).findFirst();
    if (spawnWorld.isEmpty()) {
      return;
    }
    Location loc = new Location(spawnWorld.get(), location.x, location.y, location.z, location.yaw, 0);
    e.setSpawnLocation(loc);
  }

  private boolean handleHotbarCommand(Player player, String[] args, Command command) {
    if (!this.permission.hasRole(player, "member")) {
      return false;
    }
    if (args.length == 0) {
      return false;
    }
    String blockName = args[0];
    BlockNameTabCompleter completer = new BlockNameTabCompleter();
    List<String> candidates = completer.onTabComplete(player, command, "", args);
    if (candidates != null && candidates.size() == 1) {
      blockName = candidates.get(0);
    }

    PlayerInventory inventory = player.getInventory();
    Server server = player.getServer();
    BlockData blockData;
    try {
      blockData = server.createBlockData(blockName);
    } catch (Throwable e) {
      player.sendMessage(ChatColor.RED + "Cannot create BlockData from: \"" + blockName + "\"");
      return true;
    }
    ItemStack itemStack = new ItemStack(blockData.getMaterial());
    inventory.setItemInMainHand(itemStack);
    return true;
  }

  @EventHandler
  public void onEntityChangeBlock(EntityChangeBlockEvent e) {
    if (e.getEntityType() != EntityType.SHEEP) {
      return;
    }
    Entity entity = e.getEntity();
    if (!(entity instanceof Sheep)) {
      return;
    }
    Sheep sheep = (Sheep) entity;
    if (!sheep.isSheared()) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onServerLoad(ServerLoadEvent e) {
    if (e.getType() != ServerLoadEvent.LoadType.STARTUP) {
      return;
    }
    controllerService.setServerReady();
  }
}
