package com.nova41.bukkitdev.slr.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Class for managing sub commands of SnowLeopard.
 */
public class CommandManager implements CommandExecutor {

    // All registered commands
    private final Map<String, BiConsumer<CommandSender, String[]>> registeredCommands = new HashMap<>();

    // Creates an instance of the CommandManager class
    public CommandManager(Plugin plugin, String baseCommand) {
        if (plugin != null)
            plugin.getServer().getPluginCommand(baseCommand).setExecutor(this);
    }

    // Register command
    public void register(String command, BiConsumer<CommandSender, String[]> event) {
        registeredCommands.put(command.toLowerCase(Locale.ENGLISH), event);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 && registeredCommands.containsKey("")) {
            registeredCommands.get("").accept(sender, args);
            return true;
        }

        String fullExecution = String.join(" ", args).toLowerCase();
        Optional<Map.Entry<String, BiConsumer<CommandSender, String[]>>> matchedCommand =
                registeredCommands.entrySet().stream()
                        .filter(entry -> !entry.getKey().equals(""))
                        .filter(entry -> fullExecution.startsWith(entry.getKey()))
                        .findAny();

        if (matchedCommand.isPresent()) {
            String[] param = args.length == 0 ? new String[0] : Arrays.copyOfRange(args,
                    matchedCommand.get().getKey().split(" ").length, args.length);

            matchedCommand.get().getValue().accept(sender, param);
        } else
            sender.sendMessage(ChatColor.RED + "Unknown command. Please check your spellings.");
        return true;
    }

}
