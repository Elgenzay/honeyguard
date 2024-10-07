package gg.elg.honeyGuard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public final class HoneyGuard extends JavaPlugin implements Listener {

    private static final String CONFIG_KEY_WAXABLE_MATERIALS = "waxable-materials";
    private static final String CONFIG_KEY_WAXABLE_MATERIALS_SUFFIXES = "waxable-materials-suffixes";
    private static final String CONFIG_KEY_PARTICLE_RANGE = "particle-range";
    private static final String CONFIG_KEY_FIRE_PROTECTION = "enable-fire-spread-protection";
    private static final String CONFIG_KEY_CHANCE_BASED_WAXABLE_MATERIALS = "chance-based-waxable-materials";
    private static final String CONFIG_KEY_CHANCE_BASED_WAXABLE_MATERIALS_SUFFIXES = "chance-based-waxable-materials-suffixes";
    private static final String CONFIG_KEY_HONEYCOMB_CONSUMPTION_CHANCE = "honeycomb-consumption-chance";
    private static final String CONFIG_KEY_HONEYCOMB_DROP_CHANCE = "honeycomb-drop-chance";
    private static final String CONFIG_KEY_INTERACTIVE_MATERIALS = "interactive-materials";
    private static final String CONFIG_KEY_INTERACTIVE_MATERIALS_SUFFIXES = "interactive-materials-suffixes";
    private static final String CONFIG_KEY_SAVE_RATE = "autosave-rate";
    private static final String CONFIG_KEY_INTRO_MESSAGE = "enable-introductory-message";
    private static final String CONFIG_KEY_USE_WORLD_WHITELIST = "use-world-whitelist";
    private static final String CONFIG_KEY_WORLD_LIST = "world-list";
    private static final String WORLD_DATA_FOLDER_NAME = "world_data";
    private static final long PARTICLE_RATE_TICKS = 20;
    private int particleRange = 20;
    boolean fireProtection = true;
    private boolean showIntroductionMessage = true;
    int honeycombConsumptionChance = 10;
    int honeycombDropChance = 5;
    private long saveRateMinutes = 5;

    private WaxedBlockManager waxedBlockManager;
    private IntroductionManager introductionManager;
    List<Material> chanceBasedWaxableMaterials;
    List<Material> nonChanceBasedWaxableMaterials;
    private List<Material> interactiveMaterials;
    private List<Material> allWaxableMaterials;
    private List<String> worldList;
    private boolean usingWorldWhitelist;

    private List<String> getStringListOrEmpty(String path) {
        FileConfiguration config = getConfig();

        if (config.contains(path, true) && config.isList(path)) {
            List<?> list = config.getList(path);
            if (list == null || list.isEmpty()) return Collections.emptyList();

            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private List<Material> getMaterialList(String configKey) {
        return getStringListOrEmpty(configKey).stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isDisabledInWorld(World world) {
        return usingWorldWhitelist != worldList.contains(world.getName());
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        particleRange = config.getInt(CONFIG_KEY_PARTICLE_RANGE, particleRange);
        fireProtection = config.getBoolean(CONFIG_KEY_FIRE_PROTECTION, fireProtection);
        honeycombConsumptionChance = config.getInt(CONFIG_KEY_HONEYCOMB_CONSUMPTION_CHANCE, honeycombConsumptionChance);
        honeycombDropChance = config.getInt(CONFIG_KEY_HONEYCOMB_DROP_CHANCE, honeycombDropChance);
        saveRateMinutes = config.getLong(CONFIG_KEY_SAVE_RATE, saveRateMinutes);
        showIntroductionMessage = config.getBoolean(CONFIG_KEY_INTRO_MESSAGE, showIntroductionMessage);
        nonChanceBasedWaxableMaterials = getMaterialList(CONFIG_KEY_WAXABLE_MATERIALS);
        chanceBasedWaxableMaterials = getMaterialList(CONFIG_KEY_CHANCE_BASED_WAXABLE_MATERIALS);
        interactiveMaterials = getMaterialList(CONFIG_KEY_INTERACTIVE_MATERIALS);
        File worldDataFolder = new File(getDataFolder(), WORLD_DATA_FOLDER_NAME);

        if (!worldDataFolder.exists() && !worldDataFolder.mkdirs()) {
            getLogger().severe("Could not create " + WORLD_DATA_FOLDER_NAME + " folder. Using parent data folder.");
            worldDataFolder = getDataFolder();
        }

        List<String> nonChanceBasedWaxableMaterialsSuffixes = getStringListOrEmpty(CONFIG_KEY_WAXABLE_MATERIALS_SUFFIXES);
        List<String> chanceBasedWaxableMaterialsSuffixes = getStringListOrEmpty(CONFIG_KEY_CHANCE_BASED_WAXABLE_MATERIALS_SUFFIXES);
        List<String> interactiveMaterialsSuffixes = getStringListOrEmpty(CONFIG_KEY_INTERACTIVE_MATERIALS_SUFFIXES);
        worldList = getStringListOrEmpty(CONFIG_KEY_WORLD_LIST);
        usingWorldWhitelist = config.getBoolean(CONFIG_KEY_USE_WORLD_WHITELIST);

        for (Material material : Material.values()) {
            if (nonChanceBasedWaxableMaterials.contains(material) || chanceBasedWaxableMaterials.contains(material))
                continue;

            for (String substring : nonChanceBasedWaxableMaterialsSuffixes)
                if (material.toString().endsWith(substring)) nonChanceBasedWaxableMaterials.add(material);

            for (String substring : chanceBasedWaxableMaterialsSuffixes)
                if (material.toString().endsWith(substring)) chanceBasedWaxableMaterials.add(material);

            for (String substring : interactiveMaterialsSuffixes)
                if (material.toString().endsWith(substring)) interactiveMaterials.add(material);
        }

        nonChanceBasedWaxableMaterials.sort(Comparator.comparing(Material::toString));
        chanceBasedWaxableMaterials.sort(Comparator.comparing(Material::toString));
        allWaxableMaterials = new ArrayList<>(nonChanceBasedWaxableMaterials);
        allWaxableMaterials.addAll(chanceBasedWaxableMaterials);
        waxedBlockManager = new WaxedBlockManager(getLogger(), worldDataFolder, allWaxableMaterials);
        introductionManager = new IntroductionManager(getLogger(), getDataFolder());
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand command = Objects.requireNonNull(getCommand("honeyguard"));
        command.setExecutor(new Commands(this));
        command.setTabCompleter(new TabCompletions());

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    World world = player.getWorld();
                    if (isDisabledInWorld(world)) return;

                    if (player.getInventory().getItemInMainHand().getType() == Material.HONEYCOMB) {
                        HashSet<Coordinate> waxedCoordinates = waxedBlockManager.getWaxedCoordinates(world.getName());
                        HashSet<Location> toRemove = new HashSet<>();

                        for (Coordinate coordinate : waxedCoordinates) {
                            Location coordinateLocation = new Location(world, coordinate.getX(), coordinate.getY(),
                                    coordinate.getZ());
                            Material coordinateMaterial = world.getBlockAt(coordinateLocation).getType();

                            if (!allWaxableMaterials.contains(coordinateMaterial)) {
                                // The block has since been destroyed indirectly
                                toRemove.add(coordinateLocation);
                                continue;
                            }

                            int distance = Coordinate
                                    .calculateFastDistance(Coordinate.fromLocation(player.getLocation()), coordinate);

                            if (distance < particleRange) {
                                Location particleLocation = new Location(world, coordinate.getX() + 0.5,
                                        coordinate.getY() + 0.5, coordinate.getZ() + 0.5);

                                if (coordinateMaterial.isOccluding()) {
                                    Location playerLoc = player.getLocation();
                                    double dx = playerLoc.getX() - particleLocation.getX();
                                    double dy = playerLoc.getY() - particleLocation.getY();
                                    double dz = playerLoc.getZ() - particleLocation.getZ();
                                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                                    // Move particles closer to the player so they're not obstructed inside the occluding block
                                    particleLocation.add((dx / length) * 0.5, (dy / length) * 0.5, (dz / length) * 0.5);
                                }

                                player.spawnParticle(Particle.WAX_ON, particleLocation, 5, 0.25, 0.25, 0.25, 0);
                            }
                        }

                        for (Location removeLocation : toRemove) waxedBlockManager.removeWaxedBlock(removeLocation);
                    }
                }
            }
        }.runTaskTimer(this, 0L, PARTICLE_RATE_TICKS);

        if (saveRateMinutes == 0) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                waxedBlockManager.saveAllUnsavedChanges();
                introductionManager.saveIfNeeded();
            }
        }.runTaskTimer(this, 0L, 20 * 60 * saveRateMinutes);
    }

    @Override
    public void onDisable() {
        waxedBlockManager.saveAllUnsavedChanges();
        introductionManager.saveIfNeeded();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isDisabledInWorld(event.getPlayer().getWorld())) return;
        Action action = event.getAction();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        Material clickedBlockMaterial = clickedBlock.getType();
        Location location = clickedBlock.getLocation();
        Player player = event.getPlayer();
        boolean isHoldingHoneycomb = player.getInventory().getItemInMainHand().getType() == Material.HONEYCOMB;

        if (action == Action.LEFT_CLICK_BLOCK && isHoldingHoneycomb) {
            String waxedStateString = waxedBlockManager.isWaxed(location) ? "Waxed" : "Unwaxed";

            if (nonChanceBasedWaxableMaterials.contains(clickedBlockMaterial))
                player.sendActionBar(Component.text(waxedStateString + " Standard Block (").color(NamedTextColor.YELLOW)
                        .append(Component.text("/honeyguard").color(NamedTextColor.WHITE))
                        .append(Component.text(")").color(NamedTextColor.YELLOW)));
            else if (chanceBasedWaxableMaterials.contains(clickedBlockMaterial))
                player.sendActionBar(Component.text(waxedStateString + " Chance Block (").color(NamedTextColor.YELLOW)
                        .append(Component.text("/honeyguard").color(NamedTextColor.WHITE))
                        .append(Component.text(")").color(NamedTextColor.YELLOW)));

            return;
        }

        if (action != Action.RIGHT_CLICK_BLOCK) return;

        if (waxedBlockManager.isWaxed(location)) {
            if (!interactiveMaterials.contains(clickedBlockMaterial)) return;
            if (event.getItem() != null && player.isSneaking()) return;

            event.setCancelled(true);
            player.playSound(location, Sound.BLOCK_SIGN_WAXED_INTERACT_FAIL, 1.0f, 1.0f);
            Location particleLocation = new Location(location.getWorld(), location.getX() + 0.5, location.getY() + 0.5,
                    location.getZ() + 0.5);
            player.spawnParticle(Particle.WAX_ON, particleLocation, 5, 0.25, 0.25, 0.25, 0);
        } else if (isHoldingHoneycomb && allWaxableMaterials.contains(clickedBlockMaterial)) {
            event.setCancelled(true);

            if (player.getGameMode() != GameMode.CREATIVE)
                if (chanceBasedWaxableMaterials.contains(clickedBlockMaterial)) {
                    if (new Random().nextInt(100) < honeycombConsumptionChance) takeHeldHoneycomb(player);
                } else takeHeldHoneycomb(player);

            waxBlockAt(location);
            Location otherDoorHalfLocation = otherDoorHalf(clickedBlock);

            if (otherDoorHalfLocation != null) waxBlockAt(otherDoorHalfLocation);
        }
    }

    private void waxBlockAt(Location location) {
        waxedBlockManager.addWaxedBlock(location);
        World world = location.getWorld();
        Location particleLocation = new Location(world, location.getX() + 0.5, location.getY() + 0.5, location.getZ() + 0.5);
        world.spawnParticle(Particle.WAX_ON, particleLocation, 5, 0.25, 0.25, 0.25, 0);
        world.playSound(location, Sound.ITEM_HONEYCOMB_WAX_ON, 1.0f, 1.0f);
    }

    private void takeHeldHoneycomb(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack itemInHand = inventory.getItemInMainHand();

        if (itemInHand.getType() != Material.HONEYCOMB) {
            getLogger().warning("takeHeldHoneycomb called on a player not holding a honeycomb");
            return;
        }

        if (itemInHand.getAmount() > 0) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);

            if (itemInHand.getAmount() == 0) inventory.setItemInMainHand(null);
            else inventory.setItemInMainHand(itemInHand);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        World world = location.getWorld();
        if (isDisabledInWorld(world)) return;

        if (waxedBlockManager.removeWaxedBlock(location)) {
            Material material = block.getType();

            if (chanceBasedWaxableMaterials.contains(material)) {
                if (new Random().nextInt(100) < honeycombDropChance)
                    world.dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.HONEYCOMB));

                return;
            }

            world.dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.HONEYCOMB));
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (isDisabledInWorld(event.getBlock().getWorld())) return;
        for (Block block : event.getBlocks()) waxedBlockManager.removeWaxedBlock(block.getLocation());
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (isDisabledInWorld(event.getBlock().getWorld())) return;
        for (Block block : event.getBlocks()) waxedBlockManager.removeWaxedBlock(block.getLocation());
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        if (isDisabledInWorld(event.getLocation().getWorld())) return;
        for (Block block : event.blockList()) waxedBlockManager.removeWaxedBlock(block.getLocation());
    }

    @EventHandler
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (isDisabledInWorld(event.getBlock().getWorld())) return;
        for (Block block : event.blockList()) waxedBlockManager.removeWaxedBlock(block.getLocation());
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!fireProtection || event.getSource().getType() != Material.FIRE) return;
        Block newFireBlock = event.getBlock();
        if (isDisabledInWorld(newFireBlock.getWorld())) return;

        for (BlockFace face : BlockFace.values()) {
            Location adjacent = newFireBlock.getRelative(face).getLocation();

            if (waxedBlockManager.isWaxed(adjacent)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (isDisabledInWorld(block.getWorld())) return;
        if (fireProtection && waxedBlockManager.isWaxed(block.getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (isDisabledInWorld(block.getWorld())) return;
        waxedBlockManager.removeWaxedBlock(block.getLocation());
        Location otherDoorHalfLocation = otherDoorHalf(block);
        if (otherDoorHalfLocation != null) waxedBlockManager.removeWaxedBlock(otherDoorHalfLocation);
    }

    private static Location otherDoorHalf(Block block) {
        if (block.getBlockData() instanceof Door door) {
            Location otherHalfLocation;

            if (door.getHalf() == Bisected.Half.TOP)
                otherHalfLocation = block.getRelative(BlockFace.DOWN).getLocation();
            else otherHalfLocation = block.getRelative(BlockFace.UP).getLocation();

            return otherHalfLocation;
        }

        return null;
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        LivingEntity entity = event.getEntity();
        if (isDisabledInWorld(entity.getWorld()) || !showIntroductionMessage) return;

        if (entity instanceof Player player) {
            if (event.getItem().getItemStack().getType() != Material.HONEYCOMB) return;
            if (introductionManager.hasNotBeenIntroduced(player)) introductionManager.introduce(player);
        }
    }


    @EventHandler
    public void onInventoryPickup(InventoryOpenEvent event) {
        HumanEntity entity = event.getPlayer();
        if (isDisabledInWorld(entity.getWorld()) || !showIntroductionMessage) return;

        if (event.getPlayer() instanceof Player player) {
            Inventory inventory = event.getInventory();
            for (ItemStack item : inventory.getContents())
                if (item != null && item.getType() == Material.HONEYCOMB && introductionManager.hasNotBeenIntroduced(player)) {
                    introductionManager.introduce(player);
                    break;
                }
        }
    }
}
