package dev.twme.fawekit.util;

import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class BlockGlob {
    private BlockGlob() {
    }

    public static String expand(String token) {
        if (token.indexOf('*') < 0 && token.indexOf('?') < 0) return token;
        String prefix = token.startsWith("minecraft:") ? "minecraft:" : "";
        String resource = prefix.isEmpty() ? token : token.substring(prefix.length());
        if (!resource.matches("[a-z0-9_.*?,-]+")) return token;
        Pattern pattern = Pattern.compile(globRegex(resource.toLowerCase(Locale.ROOT)));
        List<String> matches = new ArrayList<>();
        for (int i = 0; i < BlockTypes.size(); i++) {
            BlockType type = BlockTypes.get(i);
            if (type == null || !type.getNamespace().equals("minecraft")) continue;
            if (pattern.matcher(type.getResource()).matches()) {
                matches.add(prefix.isEmpty() ? type.getResource() : type.id());
            }
        }
        matches.sort(String::compareTo);
        return matches.isEmpty() ? token : String.join(",", matches);
    }

    private static String globRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (char character : glob.toCharArray()) {
            switch (character) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                case '.' -> regex.append("\\.");
                default -> regex.append(character);
            }
        }
        return regex.append('$').toString();
    }

    static boolean matches(String glob, String value) {
        return Pattern.matches(globRegex(glob), value);
    }
}
