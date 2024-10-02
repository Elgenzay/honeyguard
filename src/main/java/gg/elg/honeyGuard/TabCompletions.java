package gg.elg.honeyGuard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TabCompletions implements TabCompleter {
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("honeyguard") && args.length == 1){
            List<String> subcommands = new ArrayList<>();
            subcommands.add("help");
            subcommands.add("standardblocks");
            subcommands.add("chanceblocks");
            return subcommands;
        }
        return null;
    }
}
