package gg.elg.honeyGuard;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
    private static final String WORLD_DATA_FOLDER_NAME = "world_data";
    private static final long PARTICLE_RATE = 20;
    private int particleRange = 20;
    private boolean fireProtection = false;
    private int honeycombConsumptionChance = 10;
    private int honeycombDropChance = 5;

    private WaxedBlockManager waxedBlockManager;
    private List<Material> chanceBasedWaxableMaterials;
    private List<Material> allWaxableMaterials;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        particleRange = config.getInt(CONFIG_KEY_PARTICLE_RANGE, particleRange);
        fireProtection = config.getBoolean(CONFIG_KEY_FIRE_PROTECTION, fireProtection);
        honeycombConsumptionChance = config.getInt(CONFIG_KEY_HONEYCOMB_CONSUMPTION_CHANCE, honeycombConsumptionChance);
        honeycombDropChance = config.getInt(CONFIG_KEY_HONEYCOMB_DROP_CHANCE, honeycombDropChance);

        List<Material> nonChanceBasedWaxableMaterials = config.getStringList(CONFIG_KEY_WAXABLE_MATERIALS).stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        chanceBasedWaxableMaterials = config.getStringList(CONFIG_KEY_CHANCE_BASED_WAXABLE_MATERIALS).stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        File worldDataFolder = new File(getDataFolder(), WORLD_DATA_FOLDER_NAME);

        if (!worldDataFolder.exists() && !worldDataFolder.mkdirs()){
            getLogger().severe("Could not create " + WORLD_DATA_FOLDER_NAME + " folder. Using parent data folder.");
            worldDataFolder = getDataFolder();
        }

        List<String> nonChanceBasedWaxableMaterialsSuffixes = config.getStringList(CONFIG_KEY_WAXABLE_MATERIALS_SUFFIXES);
        List<String> chanceBasedWaxableMaterialsSuffixes = config.getStringList(CONFIG_KEY_CHANCE_BASED_WAXABLE_MATERIALS_SUFFIXES);

        for (Material material : Material.values()) {
            if (nonChanceBasedWaxableMaterials.contains(material) || chanceBasedWaxableMaterials.contains(material))
                continue;

            for (String substring : nonChanceBasedWaxableMaterialsSuffixes)
                if (material.toString().endsWith(substring)) nonChanceBasedWaxableMaterials.add(material);

            for (String substring : chanceBasedWaxableMaterialsSuffixes)
                if (material.toString().endsWith(substring)) chanceBasedWaxableMaterials.add(material);
        }

        allWaxableMaterials = new ArrayList<>(nonChanceBasedWaxableMaterials);
        allWaxableMaterials.addAll(chanceBasedWaxableMaterials);

        waxedBlockManager = new WaxedBlockManager(getLogger(), worldDataFolder, allWaxableMaterials);
        getServer().getPluginManager().registerEvents(this, this);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getServer().getOnlinePlayers())
                    if (player.getInventory().getItemInMainHand().getType() == Material.HONEYCOMB) {
                        World world = player.getWorld();
                        HashSet<Coordinate> waxedCoordinates = waxedBlockManager.getWaxedCoordinates(world.getName());
                        HashSet<Location> toRemove = new HashSet<>();

                        for (Coordinate coordinate : waxedCoordinates) {
                            Location coordinateLocation = new Location(world, coordinate.getX(), coordinate.getY(), coordinate.getZ());
                            Material coordinateMaterial = world.getBlockAt(coordinateLocation).getType();

                            if (!allWaxableMaterials.contains(coordinateMaterial)) {
                                // The block has since been destroyed indirectly
                                toRemove.add(coordinateLocation);
                                continue;
                            }

                            int distance = Coordinate.calculateFastDistance(Coordinate.fromLocation(player.getLocation()), coordinate);

                            if (distance < particleRange) {
                                Location particleLocation = new Location(world, coordinate.getX() + 0.5, coordinate.getY() + 0.5, coordinate.getZ() + 0.5);

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
        }.runTaskTimer(this, 0L, PARTICLE_RATE);
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        Material clickedBlockMaterial = clickedBlock.getType();
        Location location = clickedBlock.getLocation();
        Player player = event.getPlayer();

        if (waxedBlockManager.isWaxed(location)){
            ItemStack handItem = event.getItem();

            if (handItem != null && handItem.getType().isBlock()){
                // Let players place blocks against interactable waxed blocks by sneaking
                if (player.isSneaking()) return;

                // Assume occluding blocks are not interactable so we can place blocks on them without sneaking
                if (clickedBlockMaterial.isOccluding()) return;
            }

            event.setCancelled(true);
            player.playSound(location, Sound.BLOCK_SIGN_WAXED_INTERACT_FAIL, 1.0f, 1.0f);
            Location particleLocation = new Location(location.getWorld(), location.getX() + 0.5, location.getY() + 0.5, location.getZ() + 0.5);
            player.spawnParticle(Particle.WAX_ON, particleLocation, 5, 0.25, 0.25, 0.25, 0);
        } else if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.HONEYCOMB && allWaxableMaterials.contains(clickedBlockMaterial)) {
            event.setCancelled(true);

            if (chanceBasedWaxableMaterials.contains(clickedBlockMaterial)){
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

        if (itemInHand.getType() != Material.HONEYCOMB){
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
        if (waxedBlockManager.removeWaxedBlock(location)){
            Material material = block.getType();

            if (chanceBasedWaxableMaterials.contains(material)){
                if (new Random().nextInt(100) < honeycombDropChance)
                    location.getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.HONEYCOMB));

                return;
            }

            location.getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.HONEYCOMB));
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) waxedBlockManager.removeWaxedBlock(block.getLocation());
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) waxedBlockManager.removeWaxedBlock(block.getLocation());
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        List<Block> blocks = event.blockList();
        for (Block block : blocks) waxedBlockManager.removeWaxedBlock(block.getLocation());
    }

    @EventHandler
    public void onBlockExplosion(BlockExplodeEvent event) {
        List<Block> blocks = event.blockList();
        for (Block block : blocks) waxedBlockManager.removeWaxedBlock(block.getLocation());
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!fireProtection || event.getSource().getType() != Material.FIRE) return;
        Block newFireBlock = event.getBlock();

        for (BlockFace face : BlockFace.values()) {
            Location adjacent = newFireBlock.getRelative(face).getLocation();

            if (waxedBlockManager.isWaxed(adjacent)){
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (fireProtection && waxedBlockManager.isWaxed(event.getBlock().getLocation())) event.setCancelled(true);
    }


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
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
}
