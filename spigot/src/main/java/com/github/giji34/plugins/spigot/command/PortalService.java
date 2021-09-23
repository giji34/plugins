package com.github.giji34.plugins.spigot.command;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class PortalService implements com.github.giji34.plugins.shared.PortalService {
  private final HashMap<UUID, ReservedSpawnLocation> reservation = new HashMap<>();

  @Override
  public boolean reserveSpawnLocation(UUID player, int dimension, double x, double y, double z) {
    synchronized (this) {
      ReservedSpawnLocation location = new ReservedSpawnLocation(dimension, x, y, z);
      this.reservation.put(player, location);
      return true;
    }
  }

  public Optional<ReservedSpawnLocation> drainReservation(UUID player) {
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
}
