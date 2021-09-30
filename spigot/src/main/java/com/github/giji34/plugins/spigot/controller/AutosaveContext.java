package com.github.giji34.plugins.spigot.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class AutosaveContext implements HttpHandler {
  private final ControllerService service;
  static final String kPath = "/autosave/";

  AutosaveContext(ControllerService service) {
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
    if (method.equalsIgnoreCase("post")) {
      if (path.equalsIgnoreCase("increment_suspention_ticket")) {
        service.incrementAutosaveSuspentionTicket();
        ControllerService.CloseHttpExchange(t, 200);
        return;
      } else if (path.equalsIgnoreCase("decrement_suspention_ticket")) {
        service.decrementAutosaveSuspentionTicket();
        ControllerService.CloseHttpExchange(t, 200);
        return;
      }
    }
    ControllerService.CloseHttpExchange(t, 404);
  }
}
