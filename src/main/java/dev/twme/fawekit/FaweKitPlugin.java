package dev.twme.fawekit;

import dev.twme.fawekit.command.AutoRotatePasteCommand;
import dev.twme.fawekit.command.ClipboardCommand;
import dev.twme.fawekit.command.CopyNearCommand;
import dev.twme.fawekit.command.MultiReplaceCommand;
import dev.twme.fawekit.command.MultiSelectionCommand;
import dev.twme.fawekit.command.TeleportSelectionCommand;
import dev.twme.fawekit.command.SavedSelectionCommand;
import dev.twme.fawekit.command.BiomeMaskCommand;
import dev.twme.fawekit.command.CheatSheetCommand;
import dev.twme.fawekit.command.EchoCommand;
import dev.twme.fawekit.command.ShortcutCommand;
import dev.twme.fawekit.listener.ShortcutListener;
import dev.twme.fawekit.listener.SchematicSearchListener;
import dev.twme.fawekit.service.ShortcutService;
import dev.twme.fawekit.command.PinCommand;
import dev.twme.fawekit.listener.PinnedCommandListener;
import dev.twme.fawekit.service.PinService;
import dev.twme.fawekit.mask.EnvironmentalMaskParser;
import com.sk89q.worldedit.WorldEdit;
import dev.twme.fawekit.listener.CopyDirectionListener;
import dev.twme.fawekit.listener.CompatibilityAliasListener;
import dev.twme.fawekit.service.CopyDirectionStore;
import dev.twme.fawekit.service.SelectionStackService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class FaweKitPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        CopyDirectionStore directions = new CopyDirectionStore();
        SelectionStackService selections = new SelectionStackService();
        ShortcutService shortcuts = new ShortcutService(getDataFolder().toPath());
        PinService pins = new PinService();
        EnvironmentalMaskParser.registerAll(WorldEdit.getInstance().getMaskFactory());
        register("/tpsel", new TeleportSelectionCommand(selections));
        register("/multireplace", new MultiReplaceCommand());
        register("/clipboard", new ClipboardCommand());
        register("/copynear", new CopyNearCommand());
        register("/autorotatepaste", new AutoRotatePasteCommand(directions));
        register("/msel", new MultiSelectionCommand(selections));
        register("/ssel", new SavedSelectionCommand(getDataFolder().toPath(), selections));
        register("/bmask", new BiomeMaskCommand());
        register("/help-masks", CheatSheetCommand.masks());
        register("/help-patterns", CheatSheetCommand.patterns());
        register("/echo", new EchoCommand());
        register("/shortcut", new ShortcutCommand(shortcuts));
        register("/pin", new PinCommand(pins, true));
        register("/unpin", new PinCommand(pins, false));
        getServer().getPluginManager().registerEvents(new CopyDirectionListener(directions), this);
        getServer().getPluginManager().registerEvents(new CompatibilityAliasListener(), this);
        getServer().getPluginManager().registerEvents(new SchematicSearchListener(), this);
        getServer().getPluginManager().registerEvents(new ShortcutListener(shortcuts), this);
        getServer().getPluginManager().registerEvents(new PinnedCommandListener(pins), this);
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), "Missing command " + name);
        command.setExecutor(executor);
    }
}
