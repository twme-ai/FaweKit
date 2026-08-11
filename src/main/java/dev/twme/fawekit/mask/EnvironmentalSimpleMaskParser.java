package dev.twme.fawekit.mask;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.internal.registry.SimpleInputParser;

import java.util.List;

final class EnvironmentalSimpleMaskParser extends SimpleInputParser<Mask> {
    private final EnvironmentalMask.Kind kind;
    private final List<String> aliases;

    EnvironmentalSimpleMaskParser(WorldEdit worldEdit, EnvironmentalMask.Kind kind, String alias) {
        super(worldEdit);
        this.kind = kind;
        this.aliases = List.of(alias);
    }

    @Override
    public List<String> getMatchedAliases() {
        return aliases;
    }

    @Override
    public Mask parseFromSimpleInput(String input, ParserContext context) throws InputParseException {
        return new EnvironmentalMask(context.requireExtent(), kind, 1, 15);
    }
}
