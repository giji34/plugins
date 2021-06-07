package com.github.giji34.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@com.velocitypowered.api.plugin.Plugin(id = "giji34_velocity_plugin", name = "Giji34VelocityPlugin", version = "1.0", description = "A velocity plugin for giji34", authors = { "kbinani" })
public class Plugin {
  private final ProxyServer server;
  private final Logger logger;
  private final HashMap<UUID, String> previousServer = new HashMap<UUID, String>();

  @Inject
  public Plugin(ProxyServer server, Logger logger) {
    this.server = server;
    this.logger = logger;
    logger.info("Loading giji34_velocity_plugin");
  }

  @Subscribe
  public void onLeave(DisconnectEvent event) {
    Player left = event.getPlayer();
    Optional<ServerConnection> connection = left.getCurrentServer();
    if (connection.isPresent()) {
      String name = connection.get().getServerInfo().getName();
      previousServer.put(left.getUniqueId(), name);
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
}
