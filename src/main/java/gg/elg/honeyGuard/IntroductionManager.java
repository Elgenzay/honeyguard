package gg.elg.honeyGuard;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

class IntroductionManager {

    private final Logger logger;
    private final File dataFile;
    private final Gson gson = new Gson();
    private final HashSet<UUID> introducedPlayers;
    private boolean hasUnsavedIntroductions = false;

    IntroductionManager(Logger logger, File dataFolder) {
        this.logger = logger;
        dataFile = new File(dataFolder, "introduced_players.json");
        introducedPlayers = loadIntroducedPlayers();
    }

    private HashSet<UUID> loadIntroducedPlayers() {
        if (!dataFile.exists()) return new HashSet<>();
        try (Reader reader = new FileReader(dataFile)) {
            return gson.fromJson(reader, new TypeToken<HashSet<UUID>>(){}.getType());
        } catch (IOException e) {
            logger.severe("Error loading introduced players: " + e.getMessage());
            return new HashSet<>();
        }
    }

    void saveIfNeeded() {
        if (hasUnsavedIntroductions) {
            try (Writer writer = new FileWriter(dataFile)) {
                gson.toJson(introducedPlayers, writer);
            } catch (IOException e) {
                logger.severe("Error saving introduced players: " + e.getMessage());
            }
            hasUnsavedIntroductions = false;
        }
    }

    boolean hasNotBeenIntroduced(Player player) {
        return !introducedPlayers.contains(player.getUniqueId());
    }

    void introduce(Player player) {
        introducedPlayers.add(player.getUniqueId());
        hasUnsavedIntroductions = true;

        player.sendMessage(Component.text("You've found your first honeycomb on a server running ", NamedTextColor.YELLOW)
                .append(Component.text("HoneyGuard", NamedTextColor.GOLD))
                .append(Component.text(".", NamedTextColor.YELLOW)));

        player.sendMessage(Component.text("Run ", NamedTextColor.YELLOW)
                .append(Component.text("/honeyguard", NamedTextColor.WHITE)
                        .clickEvent(ClickEvent.runCommand("/honeyguard")))
                .append(Component.text(" for more information.", NamedTextColor.YELLOW)));
    }
}