package dev.twme.fawekit.command;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import dev.twme.fawekit.mask.BiomeMask;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class BiomeMaskCommand extends PlayerFaweCommand {
    @Override
    protected void execute(org.bukkit.entity.Player player, Player actor, String[] args) {
        LocalSession session = session(actor);
        if (args.length == 0 || args[0].equalsIgnoreCase("clear")) {
            session.setMask(null);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Biome global mask cleared.");
            return;
        }
        Set<BiomeType> biomes = new LinkedHashSet<>();
        Arrays.stream(String.join("", args).split(",")).forEach(name -> {
            String id = name.toLowerCase(Locale.ROOT);
            if (!id.contains(":")) id = "minecraft:" + id;
            BiomeType biome = BiomeTypes.get(id);
            if (biome == null) throw new IllegalArgumentException("Unknown biome: " + name);
            biomes.add(biome);
        });
        session.setMask(new BiomeMask(actor.getWorld(), biomes));
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Biome global mask set to "
                + String.join(", ", biomes.stream().map(BiomeType::id).toList()) + '.');
    }
}
