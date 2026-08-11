package dev.twme.fawekit.mask;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.MaskFactory;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public final class EnvironmentalMaskParser extends RichParser<Mask> {
    private final EnvironmentalMask.Kind kind;

    private EnvironmentalMaskParser(WorldEdit worldEdit, EnvironmentalMask.Kind kind, String... aliases) {
        super(worldEdit, aliases);
        this.kind = kind;
    }

    public static void registerAll(MaskFactory factory) {
        WorldEdit worldEdit = WorldEdit.getInstance();
        factory.register(new EnvironmentalSimpleMaskParser(worldEdit, EnvironmentalMask.Kind.VISIBLE, "#visible"));
        factory.register(new EnvironmentalSimpleMaskParser(worldEdit, EnvironmentalMask.Kind.SKY, "#sky"));
        factory.register(new EnvironmentalSimpleMaskParser(worldEdit, EnvironmentalMask.Kind.TRANSPARENT, "#transparent"));
        factory.register(new EnvironmentalSimpleMaskParser(worldEdit, EnvironmentalMask.Kind.CONDUCTIVE, "#conductive"));
        factory.register(new EnvironmentalMaskParser(worldEdit, EnvironmentalMask.Kind.SKY_LIGHT, "#skylight"));
        factory.register(new EnvironmentalMaskParser(worldEdit, EnvironmentalMask.Kind.BLOCK_LIGHT, "#blocklight"));
        factory.register(new EnvironmentalMaskParser(worldEdit, EnvironmentalMask.Kind.LIGHT, "#light"));
        factory.register(new EnvironmentalMaskParser(worldEdit, EnvironmentalMask.Kind.EMITS_LIGHT, "#emitslight"));
        factory.register(new EnvironmentalMaskParser(worldEdit, EnvironmentalMask.Kind.OPACITY, "#opacity"));
        factory.register(new EnvironmentalSimpleMaskParser(worldEdit, EnvironmentalMask.Kind.HAS_LIGHT, "#haslight"));
        factory.register(new EnvironmentalSimpleMaskParser(worldEdit, EnvironmentalMask.Kind.NO_LIGHT, "#nolight"));
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        return index < 2 ? Stream.of("0", "1", "7", "15").filter(value -> value.startsWith(argumentInput)) : Stream.empty();
    }

    @Override
    protected Mask parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        boolean ranged = switch (kind) {
            case SKY_LIGHT, BLOCK_LIGHT, LIGHT, EMITS_LIGHT, OPACITY -> true;
            default -> false;
        };
        if (!ranged && arguments.length != 0) throw new InputParseException(getPrefix() + " takes no arguments.");
        if (arguments.length > 2) throw new InputParseException(getPrefix() + " accepts at most two light values.");
        int minimum = arguments.length == 0 ? 1 : parseLevel(arguments[0]);
        int maximum = arguments.length < 2 ? (arguments.length == 0 ? 15 : minimum) : parseLevel(arguments[1]);
        if (minimum > maximum) throw new InputParseException("Minimum must not exceed maximum.");
        return new EnvironmentalMask(context.getExtent(), kind, minimum, maximum);
    }

    private static int parseLevel(String value) throws InputParseException {
        try {
            int level = Integer.parseInt(value);
            if (level < 0 || level > 15) throw new NumberFormatException();
            return level;
        } catch (NumberFormatException exception) {
            throw new InputParseException("Light values must be integers from 0 through 15.");
        }
    }
}
