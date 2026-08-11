package dev.twme.fawekit.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class CheatSheetCommand implements CommandExecutor {
    private final String title;
    private final List<String> lines;

    private CheatSheetCommand(String title, List<String> lines) {
        this.title = title;
        this.lines = lines;
    }

    public static CheatSheetCommand masks() {
        return new CheatSheetCommand("FAWE mask examples", List.of(
                "stone - one block type; !stone - inverse",
                "stone,dirt - either; stone dirt - both conditions",
                "#existing - clipboard has a block; #surface - exposed surface",
                ">stone / <stone - above / below stone",
                "#tag[mineable/pickaxe] - blocks in a Minecraft tag",
                "Use //bmask plains,forest to constrain edits by biome"));
    }

    public static CheatSheetCommand patterns() {
        return new CheatSheetCommand("FAWE pattern examples", List.of(
                "stone - one block; 70%stone,30%dirt - weighted random",
                "#clipboard - clipboard pattern; #existing - keep existing",
                "^stone - copy compatible states to stone",
                "#offset[1][0][0][stone] - offset a pattern",
                "#linear[stone,dirt] - repeat patterns in order"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.LIGHT_PURPLE + title + ':');
        lines.forEach(line -> sender.sendMessage(ChatColor.GRAY + " - " + line));
        return true;
    }
}
