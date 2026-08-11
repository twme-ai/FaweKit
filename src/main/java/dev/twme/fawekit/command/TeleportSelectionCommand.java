package dev.twme.fawekit.command;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import dev.twme.fawekit.service.SelectionStackService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.concurrent.ThreadLocalRandom;

public final class TeleportSelectionCommand extends PlayerFaweCommand {
    private final SelectionStackService selections;

    public TeleportSelectionCommand(SelectionStackService selections) {
        this.selections = selections;
    }

    @Override
    protected void execute(org.bukkit.entity.Player player, Player actor, String[] args) throws Exception {
        LocalSession session = session(actor);
        int stackIndex = 0;
        int argumentOffset = 0;
        if (args.length >= 2 && args[0].equalsIgnoreCase("-s")) {
            stackIndex = Integer.parseInt(args[1]);
            argumentOffset = 2;
        }
        Region region = stackIndex == 0 ? session.getSelection(actor.getWorld())
                : selections.history(player.getUniqueId()).get(stackIndex);
        String[] coordinates = java.util.Arrays.copyOfRange(args, argumentOffset, args.length);
        Location destination = coordinates.length == 0
                ? findSafeLocation(player.getWorld(), region)
                : coordinateLocation(player, region, coordinates);
        if (destination == null) {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            throw new IllegalArgumentException("No safe teleport location found near " + min + " to " + max + '.');
        }
        player.teleport(destination);
        player.sendMessage(ChatColor.GRAY + "Teleported to " + destination.getBlockX() + ", "
                + destination.getBlockY() + ", " + destination.getBlockZ() + '.');
    }

    private static Location coordinateLocation(org.bukkit.entity.Player player, Region region, String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: //tpsel [<x> <y> <z>]");
        }
        BlockVector3 center = region.getCenter().toBlockPoint();
        Location current = player.getLocation();
        return new Location(player.getWorld(), coordinate(args[0], center.x()), coordinate(args[1], center.y()),
                coordinate(args[2], center.z()), current.getYaw(), current.getPitch());
    }

    private static double coordinate(String value, int base) {
        if (value.startsWith("~")) {
            return base + 0.5 + (value.length() == 1 ? 0 : Double.parseDouble(value.substring(1)));
        }
        return Double.parseDouble(value);
    }

    private static Location findSafeLocation(World world, Region region) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        for (int attempt = 0; attempt < 64; attempt++) {
            int x = random.nextInt(min.x() - 16, max.x() + 17);
            int z = random.nextInt(min.z() - 16, max.z() + 17);
            if (x >= min.x() && x <= max.x() && z >= min.z() && z <= max.z()) {
                continue;
            }
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location result = safeFeet(world, x, y, z);
            if (result != null && world.getBlockAt(x, y, z).getLightFromSky() > 1) {
                return result;
            }
        }
        for (int attempt = 0; attempt < 64; attempt++) {
            int x = random.nextInt(min.x(), max.x() + 1);
            int y = random.nextInt(min.y(), max.y() + 1);
            int z = random.nextInt(min.z(), max.z() + 1);
            Location result = safeFeet(world, x, y, z);
            if (result != null && world.getBlockAt(x, y, z).getLightLevel() > 1 && hasClearance(world, x, y, z)) {
                return result;
            }
        }
        return null;
    }

    private static Location safeFeet(World world, int x, int y, int z) {
        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()
                || !world.getBlockAt(x, y - 1, z).getType().isSolid()
                || !world.getBlockAt(x, y, z).isPassable()
                || !world.getBlockAt(x, y + 1, z).isPassable()) {
            return null;
        }
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private static boolean hasClearance(World world, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    if (!world.getBlockAt(x + dx, y + dy, z + dz).isPassable()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
