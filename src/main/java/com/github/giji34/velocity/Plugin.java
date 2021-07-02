package com.github.giji34.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@com.velocitypowered.api.plugin.Plugin(id = "giji34_velocity_plugin", name = "giji34-velocity-plugin", version = "1.1.1", description = "A velocity plugin for giji34", authors = { "kbinani" })
public class Plugin {
  private final ProxyServer server;
  private final Logger logger;
  private final HashMap<UUID, String> previousServer = new HashMap<>();
  private final @DataDirectory Path configPaths;
  private final HashSet<UUID> members = new HashSet<>();

  @Inject
  public Plugin(ProxyServer server, Logger logger, @DataDirectory Path configPaths) {
    this.server = server;
    this.logger = logger;
    this.configPaths = configPaths;
  }

  @Subscribe
  public void onLeave(DisconnectEvent event) {
    Player left = event.getPlayer();
    Optional<ServerConnection> connection = left.getCurrentServer();
    UUID uuid = left.getUniqueId();
    if (connection.isPresent() && this.members.contains(uuid)) {
      String name = connection.get().getServerInfo().getName();
      previousServer.put(uuid, name);
      this.savePreviousServer();
    }
    for (Player player : server.getAllPlayers()) {
      if (player.getUniqueId().equals(left.getUniqueId())) {
        continue;
      }
      TabList list = player.getTabList();
      list.removeEntry(left.getUniqueId());
    }
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    this.loadConfig();
    server
      .getScheduler()
      .buildTask(this, this::updateTabList)
      .repeat(1, TimeUnit.SECONDS)
      .delay(1, TimeUnit.SECONDS)
      .schedule();
  }

  @Subscribe
  public void onPlayerChat(PlayerChatEvent e) {
    e.setResult(PlayerChatEvent.ChatResult.denied());
    Player player = e.getPlayer();

    Optional<ServerConnection> currentServer = player.getCurrentServer();
    String message = "";
    if (currentServer.isPresent()) {
      message = "[" + currentServer.get().getServerInfo().getName() + "]";
    }
    message += "<" + player.getUsername() + "> ";
    message += e.getMessage();

    for (RegisteredServer server : this.server.getAllServers()) {
      server.sendMessage(Component.text(message), MessageType.CHAT);
    }
  }

  @Subscribe
  public void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent e) {
    Player player = e.getPlayer();
    UUID uuid = player.getUniqueId();
    if (!this.members.contains(uuid)) {
      return;
    }
    if (!this.previousServer.containsKey(uuid)) {
      return;
    }
    String name = this.previousServer.get(uuid);
    Optional<RegisteredServer> original = e.getInitialServer();
    if (!original.isPresent()) {
      return;
    }
    if (original.get().getServerInfo().getName().equals(name)) {
      return;
    }
    Optional<RegisteredServer> found = server.getAllServers().stream().filter((server) -> server.getServerInfo().getName().equals(name)).findFirst();
    if (!found.isPresent()) {
      return;
    }
    e.setInitialServer(found.get());
  }

  private void updateTabList() {
    try {
      unsafeUpdateTabList();
    } catch (Exception e) {
      logger.warn(e.getMessage());
    }
  }

  private void unsafeUpdateTabList() {
    Player[] players = server.getAllPlayers().toArray(new Player[0]);
    for (Player target : players) {
      TabList list = target.getTabList();
      for (Player current : players) {
        if (target.equals(current)) {
          continue;
        }
        if (IsInSameServer(target, current)) {
          continue;
        }
        TabListEntry entry = null;
        for (TabListEntry item : list.getEntries()) {
          if (item.getProfile().getId().equals(current.getUniqueId())) {
            entry = item;
            break;
          }
        }
        if (entry == null) {
          entry = TabListEntry.builder()
            .profile(current.getGameProfile())
            .displayName(Component.text(current.getUsername()))
            .gameMode(3)
            .tabList(list)
            .build();
          list.addEntry(entry);
        } else {
          entry.setDisplayName(Component.text(current.getUsername()));
          entry.setGameMode(3);
        }
      }
    }
  }

  private static boolean IsInSameServer(Player a, Player b) {
    Optional<ServerConnection> serverA = a.getCurrentServer();
    Optional<ServerConnection> serverB = b.getCurrentServer();
    if (!serverA.isPresent() || !serverB.isPresent()) {
      return false;
    }
    String nameA = serverA.get().getServerInfo().getName();
    String nameB = serverB.get().getServerInfo().getName();
    return nameA.equals(nameB);
  }

  private void loadConfig() {
    try {
      ArrayList<UUID> loaded = new ArrayList<>();
      File members = new File(this.configPaths.toFile(), "members.tsv");
      BufferedReader br = new BufferedReader(new FileReader(members));
      String line;
      while ((line = br.readLine()) != null) {
        String[] columns = line.split("\t");
        if (columns.length < 1) {
          continue;
        }
        Optional<UUID> uuid = UuidFromString(columns[0]);
        if (!uuid.isPresent()) {
          continue;
        }
        loaded.add(uuid.get());
      }
      this.members.clear();
      this.members.addAll(loaded);
    } catch (Exception e) {
      this.logger.warn(e.getMessage());
      e.printStackTrace();
    }
    this.loadPreviousServer();
  }

  private Optional<UUID> UuidFromString(String s) {
    try {
      return Optional.of(UUID.fromString(s));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private File getUserStatusFile() throws Exception {
    return new File(this.configPaths.toFile(), "user_status.tsv");
  }

  private void loadPreviousServer() {
    try {
      File userStatus = this.getUserStatusFile();
      if (!userStatus.exists()) {
        return;
      }
      BufferedReader br = new BufferedReader(new FileReader(userStatus));
      String line;
      while ((line = br.readLine()) != null) {
        String[] columns = line.split("\t");
        if (columns.length < 2) {
          continue;
        }
        Optional<UUID> uuid = UuidFromString(columns[0]);
        if (!uuid.isPresent()) {
          continue;
        }
        if (!this.members.contains(uuid.get())) {
          continue;
        }
        String name = columns[1];
        this.previousServer.put(uuid.get(), name);
      }
    } catch( Exception e) {
      this.logger.warn(e.getMessage());
      e.printStackTrace();
    }
  }

  private void unsafeBackupPreviousServer() throws Exception {
    File existing = this.getUserStatusFile();
    if (!existing.exists()) {
      return;
    }
    File backup = new File(this.getUserStatusFile().getParentFile(), "user_status.tsv.backup");
    if (backup.exists()) {
      if (!backup.delete()) {
        this.logger.warn("Cannot delete " + backup.getAbsolutePath());
        return;
      }
    }
    if (!existing.renameTo(backup)) {
      this.logger.warn("renameTo failed: \"" + existing.getAbsolutePath() + "\" -> \"" + backup.getAbsolutePath() + "\"");
    }
  }

  private void savePreviousServer() {
    try {
      this.unsafeBackupPreviousServer();
    } catch (Exception e) {
      this.logger.warn(e.getMessage());
    }

    FileWriter writer = null;
    BufferedWriter br = null;
    try {
      File file = this.getUserStatusFile();
      writer = new FileWriter(file);
      br = new BufferedWriter(writer);
      for (UUID uuid : this.previousServer.keySet()) {
        String name = this.previousServer.get(uuid);
        br.write(uuid.toString() + "\t" + name);
        br.newLine();
      }
    } catch (Exception e) {
      this.logger.warn(e.getMessage());
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (Exception e) {
          this.logger.warn(e.getMessage());
        }
      }
      if (writer != null) {
        try {
          writer.close();
        } catch (Exception e) {
          this.logger.warn(e.getMessage());
        }
      }
    }
  }
}
