package gg.elg.honeyGuard;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Location;
import org.bukkit.Material;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;

class WaxedBlockManager {

    private final Logger logger;
    private final Gson gson = new Gson();
    private final HashMap<String, HashSet<Coordinate>> waxedBlocks;
    private final File dataFolder;
    private final List<Material> allWaxableMaterials;
    private static HashSet<String> unsavedWorldNames = new HashSet<>();

    WaxedBlockManager(Logger logger, File dataFolder, List<Material> allWaxableMaterials) {
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.allWaxableMaterials = allWaxableMaterials;
        waxedBlocks = new HashMap<>();
    }

    void addWaxedBlock(Location location) {
        HashSet<Coordinate> waxedCoordinates = getWaxedCoordinates(location.getWorld().getName());
        if (waxedCoordinates.add(Coordinate.fromLocation(location)))
            unsavedWorldNames.add(location.getWorld().getName());
    }

    boolean removeWaxedBlock(Location location) {
        HashSet<Coordinate> coordinates = waxedBlocks.get(location.getWorld().getName());
        if (coordinates == null) return false;

        if (coordinates.remove(Coordinate.fromLocation(location))) {
            unsavedWorldNames.add(location.getWorld().getName());
            return true;
        }

        return false;
    }

    boolean isWaxed(Location location) {
        HashSet<Coordinate> coordinates = getWaxedCoordinates(location.getWorld().getName());

        // Remove if block has since been indirectly destroyed
        if (!allWaxableMaterials.contains(location.getWorld().getBlockAt(location).getType())) {
            removeWaxedBlock(location);
            return false;
        }

        return coordinates.contains(Coordinate.fromLocation(location));
    }

    private void saveFile(String worldName) {
        try (Writer writer = new FileWriter(new File(dataFolder, worldName + ".json"))) {
            HashSet<Coordinate> coordinates = waxedBlocks.getOrDefault(worldName, new HashSet<>());
            gson.toJson(coordinates, writer);
        } catch (IOException e) {
            logger.severe("Error saving file for world " + worldName + ": " + e.getMessage());
        }
    }

    void saveAllUnsavedChanges() {
        for (String worldName : unsavedWorldNames) saveFile(worldName);
        unsavedWorldNames = new HashSet<>();
    }

    HashSet<Coordinate> getWaxedCoordinates(String worldName) {
        if (waxedBlocks.containsKey(worldName)) return waxedBlocks.get(worldName);
        File dataFile = new File(dataFolder, worldName + ".json");

        if (!dataFile.exists()){
            waxedBlocks.put(worldName, new HashSet<>());
            return new HashSet<>();
        }

        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<HashSet<Coordinate>>() {
            }.getType();
            waxedBlocks.put(worldName, gson.fromJson(reader, type));
        } catch (IOException e) {
            logger.severe("Error loading from file for world " + worldName + ": " + e.getMessage());
            return new HashSet<>();
        }

        return waxedBlocks.get(worldName);
    }
}
