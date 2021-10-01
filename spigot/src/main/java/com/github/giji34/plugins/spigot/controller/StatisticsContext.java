package com.github.giji34.plugins.spigot.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StatisticsContext implements HttpHandler {
  private final ControllerService service;
  static final String kPath = "/statistics/";

  StatisticsContext(ControllerService service) {
    this.service = service;
  }

  @Override
  public void handle(HttpExchange t) throws IOException {
    final String method = t.getRequestMethod();
    final String fullPath = t.getRequestURI().getPath();
    if (!fullPath.startsWith(kPath)) {
      ControllerService.CloseHttpExchange(t, 404);
      return;
    }
    String path = fullPath.substring(kPath.length());
    if (method.equalsIgnoreCase("get")) {
      if (path.equalsIgnoreCase("needs_backup")) {
        OutputStream os = t.getResponseBody();
        byte[] message;
        if (service.needsBackup()) {
          int onlinePlayers = Bukkit.getServer().getOnlinePlayers().size();
          message = ("" + onlinePlayers).getBytes(StandardCharsets.UTF_8);
        } else {
          message = "no".getBytes(StandardCharsets.UTF_8);
        }
        t.sendResponseHeaders(200, message.length);
        os.write(message);
        os.close();
        return;
      }
    } else if (method.equalsIgnoreCase("post")) {
      if (path.equalsIgnoreCase("clear_needs_backup_flag")) {
        service.clearNeedsBackupIfPossible();
        ControllerService.CloseHttpExchange(t, 200);
        return;
      }
    }
    ControllerService.CloseHttpExchange(t, 404);
  }
}
