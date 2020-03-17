package com.github.giji34.t.command;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class ToggleGameMode {
    public void toggle(Player player) {
        GameMode current = player.getGameMode();
        if (current == GameMode.CREATIVE) {
            player.setGameMode(GameMode.SPECTATOR);
        } else if (current == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.CREATIVE);
        }
    }
}