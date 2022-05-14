package com.github.giji34.plugins.spigot.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ControllerService {
  private final HashMap<UUID, ReservedSpawnLocation> reservation = new HashMap<>();
  private final JavaPlugin owner;
  private final Logger logger;
  private final int port;
  private final PortalContext portalContext;
  private final AutosaveContext autosaveContext;
  private AtomicInteger autosaveSuspentionTicket = new AtomicInteger(0);
  private AtomicBoolean needsBackup = new AtomicBoolean(false);
  private final StatisticsContext statisticsContext;
  private final AtomicBoolean serverReady = new AtomicBoolean(false);

  public ControllerService(JavaPlugin owner, int port) {
    this.owner = owner;
    this.logger = owner.getLogger();
    this.port = port;
    this.portalContext = new PortalContext(this);
    this.autosaveContext = new AutosaveContext(this);
    this.statisticsContext = new StatisticsContext(this);
  }

  public void start() {
    try {
      unsafeStart();
    } catch (Throwable e) {
      logger.warning(e.getMessage());
    }
  }

  private void unsafeStart() throws Throwable {
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext(PortalContext.kPath, portalContext);
    server.createContext(AutosaveContext.kPath, autosaveContext);
    server.createContext(StatisticsContext.kPath, statisticsContext);
    server.start();
    logger.info("started rpc server on port: " + port);
  }

  void reserveSpawnLocation(UUID player, int dimension, double x, double y, double z, float yaw) {
    synchronized (this) {
      ReservedSpawnLocation location = new ReservedSpawnLocation(dimension, x, y, z, yaw);
      this.reservation.put(player, location);
    }
  }

  public Optional<ReservedSpawnLocation> drainReservedSpawnLocation(UUID player) {
    synchronized (this) {
      if (this.reservation.containsKey(player)) {
        ReservedSpawnLocation location = this.reservation.get(player);
        this.reservation.remove(player);
        return Optional.of(location);
      } else {
        return Optional.empty();
      }
    }
  }

  void incrementAutosaveSuspentionTicket() {
    int count = autosaveSuspentionTicket.incrementAndGet();
    if (count == 1) {
      Server server = Bukkit.getServer();
      BukkitScheduler scheduler = server.getScheduler();
      scheduler.runTask(owner, () -> {
        for (World world : server.getWorlds()) {
          world.setAutoSave(false);
        }
        for (World world : server.getWorlds()) {
          world.save();
        }
      });
    }
  }

  void decrementAutosaveSuspentionTicket() {
    int count = autosaveSuspentionTicket.decrementAndGet();
    if (count == 0) {
      Server server = Bukkit.getServer();
      BukkitScheduler scheduler = server.getScheduler();
      scheduler.runTask(owner, () -> {
        for (World world : server.getWorlds()) {
          world.setAutoSave(true);
        }
      });
    }
  }

  public void setNeedsBackup() {
    needsBackup.set(true);
  }

  void clearNeedsBackupIfPossible() {
    Server server = Bukkit.getServer();
    needsBackup.set(!server.getOnlinePlayers().isEmpty());
  }

  boolean needsBackup() {
    return needsBackup.get();
  }

  static void CloseHttpExchange(HttpExchange t, int status) throws IOException {
    t.sendResponseHeaders(status, 0);
    OutputStream os = t.getResponseBody();
    os.close();
  }

  public void setServerReady() {
    serverReady.set(true);
  }

  public boolean isServerReady() {
    return serverReady.get();
  }
}
