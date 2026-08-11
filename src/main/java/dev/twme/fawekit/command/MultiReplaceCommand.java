package dev.twme.fawekit.command;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.ChatColor;

public final class MultiReplaceCommand extends PlayerFaweCommand {
    @Override
    protected void execute(org.bukkit.entity.Player player, Player actor, String[] args) throws Exception {
        if (args.length < 2 || args.length % 2 != 0) {
            throw new IllegalArgumentException("Usage: //multireplace <mask> <pattern> [<mask> <pattern> ...]");
        }
        LocalSession localSession = session(actor);
        ParserContext context = parserContext(actor, localSession);
        int pairCount = args.length / 2;
        Mask[] masks = new Mask[pairCount];
        Pattern[] patterns = new Pattern[pairCount];
        for (int i = 0; i < pairCount; i++) {
            masks[i] = WorldEdit.getInstance().getMaskFactory().parseFromInput(args[i * 2], context);
            patterns[i] = WorldEdit.getInstance().getPatternFactory().parseFromInput(args[i * 2 + 1], context);
        }
        Region region = localSession.getSelection(actor.getWorld());
        int changed = 0;
        try (EditSession editSession = localSession.createEditSession(actor)) {
            for (BlockVector3 position : region) {
                Pattern selected = null;
                for (int i = 0; i < pairCount; i++) {
                    if (masks[i].test(position)) {
                        selected = patterns[i];
                    }
                }
                if (selected != null && editSession.setBlock(position, selected)) {
                    changed++;
                }
            }
            localSession.remember(editSession);
        }
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Changed " + changed + " blocks in one undoable operation.");
    }
}
