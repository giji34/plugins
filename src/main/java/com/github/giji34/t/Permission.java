package com.github.giji34.t;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Permission {
    /*
    roles:
      member:
        commands:
          - tpl
          - tpb
          - gm
          - gfill
          - greplace
          - gundo
      admin:
        commands:
          - create_portal
          - delete_portal
    users:
      user1:
        roles:
          - member
          - admin
      user2:
        roles:
          - member
     */
    final HashMap<String, Role> roles = new HashMap<>();
    final HashMap<String, User> users = new HashMap<>();

    public Permission(File configFile) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        {
            Object rolesObj = config.get("roles");
            if (rolesObj instanceof ConfigurationSection) {
                ConfigurationSection roles = (ConfigurationSection) rolesObj;
                for (String name : roles.getKeys(false)) {
                    Object commandsObj = roles.get(name + ".commands");
                    ArrayList<String> commands = new ArrayList<>();
                    if (!(commandsObj instanceof ArrayList)) {
                        continue;
                    }
                    for (Object cmd : (ArrayList) commandsObj) {
                        if (cmd instanceof String) {
                            commands.add((String) cmd);
                        }
                    }
                    Role role = new Role(name, commands);
                    this.roles.put(name, role);
                }
            }
        }

        {
            Object usersObj = config.get("users");
            if (usersObj instanceof ConfigurationSection) {
                ConfigurationSection users = (ConfigurationSection) usersObj;
                for (String name : users.getKeys(false)) {
                    Object rolesObj = users.get(name + ".roles");
                    ArrayList<Role> roles = new ArrayList<>();
                    if (!(rolesObj instanceof ArrayList)) {
                        continue;
                    }
                    for (Object role : (ArrayList)rolesObj) {
                        if (!(role instanceof String)) {
                            continue;
                        }
                        Role r = this.roles.get(role);
                        if (r == null) {
                            continue;
                        }
                        roles.add(r);
                    }
                    User user = new User(roles.toArray(new Role[]{}));
                    this.users.put(name, user);
                }
            }
        }
    }

    boolean hasPermission(Player player, String command) {
        String name = player.getName();
        if (!this.users.containsKey(name)) {
            return false;
        }
        User user = this.users.get(name);
        return user.hasPermission(command);
    }

    boolean hasRole(Player player, String role) {
        String name = player.getName();
        if (!this.users.containsKey(name)) {
            return false;
        }
        User user = this.users.get(name);
        return user.hasRole(role);
    }
}

class Role {
    final String name;
    final HashSet<String> commands;

    Role(String name, List<String> commands) {
        this.name = name;
        this.commands = new HashSet<>(commands);
    }

    boolean hasPermission(String cmd) {
        return this.commands.contains(cmd);
    }
}

class User {
    final Role[] roles;

    User(Role[] roles) {
        this.roles = roles;
    }

    boolean hasPermission(String cmd) {
        for (Role role : roles) {
            if (role.hasPermission(cmd)) {
                return true;
            }
        }
        return false;
    }

    boolean hasRole(String role) {
        for (Role r : roles) {
            if (r.name.equals(role)) {
                return true;
            }
        }
        return false;
    }
}
