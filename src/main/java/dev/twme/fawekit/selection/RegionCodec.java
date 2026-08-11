package dev.twme.fawekit.selection;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;
import com.sk89q.worldedit.world.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RegionCodec {
    private RegionCodec() {
    }

    public static void write(ConfigurationSection out, Region region) {
        if (region instanceof CuboidRegion cuboid) {
            out.set("type", "cuboid");
            vector(out.createSection("pos1"), cuboid.getPos1());
            vector(out.createSection("pos2"), cuboid.getPos2());
        } else if (region instanceof Polygonal2DRegion polygon) {
            out.set("type", "polygon2d");
            out.set("min-y", polygon.getMinimumY());
            out.set("max-y", polygon.getMaximumY());
            write2dList(out, "points", polygon.getPoints());
        } else if (region instanceof ConvexPolyhedralRegion polyhedron) {
            out.set("type", "convex");
            write3dList(out, "vertices", polyhedron.getVertices());
        } else if (region instanceof CylinderRegion cylinder) {
            out.set("type", "cylinder");
            vector(out.createSection("center"), cylinder.getCenter().toBlockPoint());
            vector2(out.createSection("radius"), cylinder.getRadius());
            out.set("min-y", cylinder.getMinimumY());
            out.set("max-y", cylinder.getMaximumY());
        } else if (region instanceof EllipsoidRegion ellipsoid) {
            out.set("type", "ellipsoid");
            vector(out.createSection("center"), ellipsoid.getCenter().toBlockPoint());
            vector3(out.createSection("radius"), ellipsoid.getRadius());
        } else if (region instanceof RegionIntersection intersection) {
            out.set("type", "multi");
            writeRegions(out, "regions", intersection.getRegions());
        } else {
            throw new IllegalArgumentException("Unsupported selection type: " + region.getClass().getSimpleName());
        }
    }

    public static Region read(ConfigurationSection in, World world) {
        String type = require(in.getString("type"), "selection type");
        return switch (type) {
            case "cuboid" -> new CuboidRegion(world, vector(in, "pos1"), vector(in, "pos2"));
            case "polygon2d" -> new Polygonal2DRegion(world, read2dList(in, "points"),
                    in.getInt("min-y"), in.getInt("max-y"));
            case "convex" -> convex(world, read3dList(in, "vertices"));
            case "cylinder" -> new CylinderRegion(world, vector(in, "center"), vector2(in, "radius"),
                    in.getInt("min-y"), in.getInt("max-y"));
            case "ellipsoid" -> new EllipsoidRegion(world, vector(in, "center"), vector3(in, "radius"));
            case "multi" -> new RegionIntersection(world, readRegions(in, "regions", world));
            default -> throw new IllegalArgumentException("Unknown saved selection type: " + type);
        };
    }

    public static void writeRegions(ConfigurationSection out, String key, Collection<Region> regions) {
        ConfigurationSection list = out.createSection(key);
        int index = 0;
        for (Region region : regions) {
            write(list.createSection(Integer.toString(index++)), region);
        }
    }

    public static List<Region> readRegions(ConfigurationSection in, String key, World world) {
        ConfigurationSection list = requiredSection(in, key);
        return list.getKeys(false).stream().sorted(java.util.Comparator.comparingInt(Integer::parseInt))
                .map(name -> read(requiredSection(list, name), world)).toList();
    }

    private static ConvexPolyhedralRegion convex(World world, List<BlockVector3> vertices) {
        ConvexPolyhedralRegion region = new ConvexPolyhedralRegion(world);
        vertices.forEach(region::addVertex);
        return region;
    }

    private static void write3dList(ConfigurationSection out, String key, Collection<BlockVector3> values) {
        ConfigurationSection list = out.createSection(key);
        int index = 0;
        for (BlockVector3 value : values) vector(list.createSection(Integer.toString(index++)), value);
    }

    private static List<BlockVector3> read3dList(ConfigurationSection in, String key) {
        ConfigurationSection list = requiredSection(in, key);
        List<BlockVector3> result = new ArrayList<>();
        list.getKeys(false).stream().sorted(java.util.Comparator.comparingInt(Integer::parseInt))
                .forEach(name -> result.add(vector(list, name)));
        return result;
    }

    private static void write2dList(ConfigurationSection out, String key, Collection<BlockVector2> values) {
        ConfigurationSection list = out.createSection(key);
        int index = 0;
        for (BlockVector2 value : values) vector2(list.createSection(Integer.toString(index++)), value.toVector2());
    }

    private static List<BlockVector2> read2dList(ConfigurationSection in, String key) {
        ConfigurationSection list = requiredSection(in, key);
        List<BlockVector2> result = new ArrayList<>();
        list.getKeys(false).stream().sorted(java.util.Comparator.comparingInt(Integer::parseInt))
                .forEach(name -> result.add(vector2(list, name).toBlockPoint()));
        return result;
    }

    private static void vector(ConfigurationSection out, BlockVector3 value) {
        out.set("x", value.x()); out.set("y", value.y()); out.set("z", value.z());
    }

    private static void vector2(ConfigurationSection out, Vector2 value) {
        out.set("x", value.x()); out.set("z", value.z());
    }

    private static void vector3(ConfigurationSection out, Vector3 value) {
        out.set("x", value.x()); out.set("y", value.y()); out.set("z", value.z());
    }

    private static BlockVector3 vector(ConfigurationSection in, String key) {
        ConfigurationSection value = requiredSection(in, key);
        return BlockVector3.at(value.getInt("x"), value.getInt("y"), value.getInt("z"));
    }

    private static Vector2 vector2(ConfigurationSection in, String key) {
        ConfigurationSection value = requiredSection(in, key);
        return Vector2.at(value.getDouble("x"), value.getDouble("z"));
    }

    private static Vector3 vector3(ConfigurationSection in, String key) {
        ConfigurationSection value = requiredSection(in, key);
        return Vector3.at(value.getDouble("x"), value.getDouble("y"), value.getDouble("z"));
    }

    private static ConfigurationSection requiredSection(ConfigurationSection in, String key) {
        ConfigurationSection section = in.getConfigurationSection(key);
        if (section == null) throw new IllegalArgumentException("Missing saved selection field: " + key);
        return section;
    }

    private static String require(String value, String name) {
        if (value == null) throw new IllegalArgumentException("Missing " + name + '.');
        return value;
    }
}
