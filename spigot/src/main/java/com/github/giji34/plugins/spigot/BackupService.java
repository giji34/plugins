package com.github.giji34.plugins.spigot;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class BackupService {
  private final String gbackupDirectory;
  private final String gitDirectory;
  private final String serverRootDirectory;
  private final JavaPlugin owner;
  private final AtomicBoolean needsBackup = new AtomicBoolean(false);
  private Optional<Integer> shutdownTimerTask = Optional.empty();
  private Optional<Integer> backupTimerTask = Optional.empty();

  static final int kBackupIntervalMinutes = 15;

  BackupService(String gbackupDirectory, String gitDirectory, JavaPlugin owner) {
    this.gbackupDirectory = gbackupDirectory;
    this.gitDirectory = gitDirectory;
    this.serverRootDirectory = owner.getDataFolder().getParentFile().getParentFile().toPath().toAbsolutePath().toString();
    this.owner = owner;

    schedule();
  }

  void timerCallback() {
    long numPlayers = owner.getServer().getOnlinePlayers().size();
    new Thread(() -> {
      if (!needsBackup.get()) {
        schedule();
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

      schedule();
    }).start();
  }

  private void schedule() {
    if (backupTimerTask.isPresent()) {
      owner.getServer().getScheduler().cancelTask(backupTimerTask.get());
      backupTimerTask = Optional.empty();
    }
    if (shutdownTimerTask.isPresent()) {
      return;
    }
    int taskId = owner.getServer().getScheduler()
      .scheduleSyncDelayedTask(owner, this::timerCallback, TimeUnit.MINUTES.toMillis(kBackupIntervalMinutes));
    backupTimerTask = Optional.of(taskId);
  }

  void onPlayerJoin() {
    needsBackup.set(true);
    if (shutdownTimerTask.isPresent()) {
      int taskId = shutdownTimerTask.get();
      owner.getServer().getScheduler().cancelTask(taskId);
      shutdownTimerTask = Optional.empty();

      schedule();
    }
  }

  void onPlayerQuit() {
    int numPlayers = owner.getServer().getOnlinePlayers().size();
    if (numPlayers > 0) {
      return;
    }
    if (shutdownTimerTask.isPresent()) {
      int id = shutdownTimerTask.get();
      owner.getServer().getScheduler().cancelTask(id);
      shutdownTimerTask = Optional.empty();
    }
    int taskId = owner.getServer().getScheduler()
      .scheduleSyncDelayedTask(owner, this::onShutdownTimerCallback, TimeUnit.MINUTES.toMillis(kBackupIntervalMinutes));
    shutdownTimerTask = Optional.of(taskId);
  }

  void onShutdownTimerCallback() {
    owner.getServer().shutdown();

    // backup should be done after shutdown, typically on the last lines of server startup script
  }
}
