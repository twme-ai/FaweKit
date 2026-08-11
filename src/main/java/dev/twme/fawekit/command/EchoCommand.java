package dev.twme.fawekit.command;

import dev.twme.fawekit.util.BlockGlob;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class EchoCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: //echo <command...>");
            return true;
        }
        String expanded = Arrays.stream(args).map(BlockGlob::expand).collect(Collectors.joining(" "));
        sender.sendMessage(ChatColor.GRAY + "@> " + expanded);
        return true;
    }
}
