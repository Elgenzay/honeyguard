package gg.elg.honeyGuard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class Commands implements CommandExecutor {

    private final HoneyGuard honeyGuard;

    Commands(HoneyGuard honeyGuard){
        this.honeyGuard = honeyGuard;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        } else if (args[0].equalsIgnoreCase("standardblocks")) {
            Component subtitle = Component.text("These blocks require one honeycomb to wax and guarantee one honeycomb drop upon destruction.", NamedTextColor.GOLD);
            sendList(sender, "Standard Blocks", subtitle, honeyGuard.nonChanceBasedWaxableMaterials);
            return true;
        } else if (args[0].equalsIgnoreCase("chanceblocks")) {
            if (honeyGuard.chanceBasedWaxableMaterials.isEmpty()){
                sender.sendMessage(Component.text("Chance-based blocks are disabled on this server.", NamedTextColor.GOLD));
                return true;
            }

            Component subtitle = Component.text("These blocks have a ")
                    .color(NamedTextColor.GOLD)
                    .append(Component.text(honeyGuard.honeycombConsumptionChance + "%")
                            .color(NamedTextColor.WHITE))
                    .append(Component.text(" chance of consuming a honeycomb upon being waxed, and a ")
                            .color(NamedTextColor.GOLD))
                    .append(Component.text(honeyGuard.honeycombDropChance + "%")
                            .color(NamedTextColor.WHITE))
                    .append(Component.text(" chance of dropping a honeycomb when destroyed.")
                            .color(NamedTextColor.GOLD));

            sendList(sender, "Chance Blocks", subtitle, honeyGuard.chanceBasedWaxableMaterials);
            return true;
        }

        return false;
    }

    private static void sendList(CommandSender sender, String title, Component subtitle, List<Material> materials) {
        sender.sendMessage(Component.text("=== HoneyGuard " + title + " ===", NamedTextColor.GOLD));
        sender.sendMessage(subtitle);
        NamedTextColor color;

        for (int i = 0; i < materials.size(); i++) {
            if (i % 2 == 0) color = NamedTextColor.WHITE;
            else color = NamedTextColor.YELLOW;
            sender.sendMessage(Component.text(materials.get(i).toString(), color));
        }

        sender.sendMessage(Component.text("=======================", NamedTextColor.GOLD));
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(Component.text("=== HoneyGuard Help ===", NamedTextColor.GOLD));

        sender.sendMessage(Component.text("HoneyGuard extends the functionality of honeycombs, allowing them to be used to protect certain blocks from interaction ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text("(but not destruction!)")
                        .color(NamedTextColor.WHITE))
                .append(Component.text(", ideal for securing decorative elements like trapdoors or levers, or to prevent accidental modifications such as the stripping of logs. While waxed blocks are protected from interaction, they can still be destroyed in the usual way and will drop a honeycomb upon destruction.", NamedTextColor.YELLOW)));

        if (honeyGuard.fireProtection) {
            sender.sendMessage(Component.text("Fire Protection", NamedTextColor.GOLD, TextDecoration.UNDERLINED));
            sender.sendMessage(Component.text("Fire will not spread to waxed blocks.", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("If a fire is ignited on a waxed block, it will burn indefinitely without destroying the block.", NamedTextColor.YELLOW));
        }

        if (!honeyGuard.chanceBasedWaxableMaterials.isEmpty()) {
            sender.sendMessage(Component.text("Chance Blocks", NamedTextColor.GOLD, TextDecoration.UNDERLINED));
            sender.sendMessage(Component.text("To prevent commonly used blocks from being cost-prohibitive to protect, some blocks have only a ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(honeyGuard.honeycombConsumptionChance + "%")
                            .color(NamedTextColor.WHITE))
                    .append(Component.text(" chance of consuming a honeycomb upon waxing, and a ")
                            .color(NamedTextColor.YELLOW))
                    .append(Component.text(honeyGuard.honeycombDropChance + "%")
                            .color(NamedTextColor.WHITE))
                    .append(Component.text(" chance of dropping a honeycomb when destroyed.")
                            .color(NamedTextColor.YELLOW)));
        }

        sender.sendMessage(Component.text("Commands", NamedTextColor.GOLD, TextDecoration.UNDERLINED));
        sender.sendMessage(Component.text("/honeyguard standardblocks", NamedTextColor.WHITE)
                .clickEvent(ClickEvent.runCommand("/honeyguard standardblocks"))
                .append(Component.text(" - List blocks that require one honeycomb to wax and guarantee one honeycomb drop upon destruction.", NamedTextColor.YELLOW)));

        sender.sendMessage(Component.text("/honeyguard chanceblocks", NamedTextColor.WHITE)
                .clickEvent(ClickEvent.runCommand("/honeyguard chanceblocks"))
                .append(Component.text(" - List blocks that operate on a chance basis.", NamedTextColor.YELLOW)));

        if (honeyGuard.chanceBasedWaxableMaterials.isEmpty())
            sender.sendMessage(Component.text("(Chance-based blocks are disabled on this server)", NamedTextColor.YELLOW));

        sender.sendMessage(Component.text("Usage", NamedTextColor.GOLD, TextDecoration.UNDERLINED));
        sender.sendMessage(Component.text("To wax a block: ", NamedTextColor.YELLOW)
                .append(Component.text("Right-click the block with a honeycomb.", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("To display particles on waxed blocks: ", NamedTextColor.YELLOW)
                .append(Component.text("Hold a honeycomb in your main hand.", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("To check if a block is standard or chance-based: ", NamedTextColor.YELLOW)
                .append(Component.text("Left-click the block with a honeycomb.", NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("=======================", NamedTextColor.GOLD));
    }

}
