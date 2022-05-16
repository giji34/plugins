package com.github.giji34.plugins.spigot;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class BackupService {
  private final String gbackupDirectory;
  private final String gitDirectory;
  private final String serverRootDirectory;
  private final JavaPlugin owner;
  private final AtomicBoolean needsBackup = new AtomicBoolean(false);
  private @Nullable Integer backupTimerTask = null;
  private @Nullable Long lastPlayerQuitTimeMillis = null;

  static final int kBackupIntervalMinutes = 15;

  BackupService(String gbackupDirectory, String gitDirectory, JavaPlugin owner) {
    this.gbackupDirectory = gbackupDirectory;
    this.gitDirectory = gitDirectory;
    File dataFolder = owner.getDataFolder().getAbsoluteFile();
    this.serverRootDirectory = dataFolder.getParentFile().getParentFile().getAbsolutePath();
    this.owner = owner;
  }

  void backupTimerCallback() {
    long numPlayers = owner.getServer().getOnlinePlayers().size();
    new Thread(() -> {
      if (lastPlayerQuitTimeMillis != null) {
        long elapsedMillis = System.currentTimeMillis() - lastPlayerQuitTimeMillis;
        if (elapsedMillis >= TimeUnit.MINUTES.toMillis(kBackupIntervalMinutes)) {
          owner.getLogger().info(elapsedMillis / 1000.0 + " seconds elapsed after last player quit. Schedule shutting down the server");
          lastPlayerQuitTimeMillis = null;
          owner.getServer().getScheduler().scheduleSyncDelayedTask(owner, () -> {
            // backup should be done after shutdown, typically on the last lines of server startup script
            owner.getServer().shutdown();
          }, 0);
          return;
        }
      }
      if (!needsBackup.get()) {
        scheduleBackupTimer();
        return;
      }

      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HHmm");
      String commitMessage = numPlayers + ": " + now.format(formatter);

      try {
        ProcessBuilder pb = new ProcessBuilder(
          "bash",
          Path.of(gbackupDirectory, "backup").toString(),
          serverRootDirectory,
          gitDirectory,
          commitMessage);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
          owner.getLogger().warning("backup script exit with code: " + code);
        }

        needsBackup.set(false);
      } catch (Throwable e) {
        e.printStackTrace();
      }

      scheduleBackupTimer();
    }).start();
  }

  private void playerActivityTimerCallback() {
    int numPlayers = owner.getServer().getOnlinePlayers().size();
    if (numPlayers > 0) {
      lastPlayerQuitTimeMillis = null;
    } else if (lastPlayerQuitTimeMillis == null) {
      lastPlayerQuitTimeMillis = System.currentTimeMillis();
    }
  }

  private long ServerTicksFromMinutes(int minutes) {
    return TimeUnit.MINUTES.toMillis(minutes) / 50;
  }

  private void scheduleBackupTimer() {
    if (backupTimerTask != null) {
      owner.getServer().getScheduler().cancelTask(backupTimerTask);
      backupTimerTask = null;
    }
    int taskId = owner.getServer().getScheduler()
      .scheduleSyncDelayedTask(owner, this::backupTimerCallback, ServerTicksFromMinutes(kBackupIntervalMinutes));
    backupTimerTask = taskId;
  }

  private void initializePlayerActivityTimer() {
    owner.getServer().getScheduler()
      .scheduleSyncRepeatingTask(owner, this::playerActivityTimerCallback, 0, 20);
  }

  void onEnable() {
    lastPlayerQuitTimeMillis = System.currentTimeMillis();
    scheduleBackupTimer();
    initializePlayerActivityTimer();
  }

  void onPlayerJoin() {
    needsBackup.set(true);
  }
}
