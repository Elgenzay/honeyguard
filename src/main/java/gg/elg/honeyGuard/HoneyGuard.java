package gg.elg.honeyGuard;

import org.bukkit.*;
import org.bukkit.block.Block;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public final class HoneyGuard extends JavaPlugin implements Listener {

    private static final String CONFIG_KEY_WAXABLE_MATERIALS = "waxable-materials";
    private static final String CONFIG_KEY_WAXABLE_MATERIALS_SUFFIXES = "waxable-materials-suffixes";
    private static final String CONFIG_KEY_PARTICLE_RANGE = "particle-range";
    private static final String WORLD_DATA_FOLDER_NAME = "world_data";
    private static final long PARTICLE_RATE = 20;
    private int particleRange = 20;

    private WaxedBlockManager waxedBlockManager;
    private List<Material> waxableMaterials;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        particleRange = config.getInt(CONFIG_KEY_PARTICLE_RANGE, particleRange);

        waxableMaterials = config.getStringList(CONFIG_KEY_WAXABLE_MATERIALS).stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        File worldDataFolder = new File(getDataFolder(), WORLD_DATA_FOLDER_NAME);

        if (!worldDataFolder.exists() && !worldDataFolder.mkdirs()){
            getLogger().severe("Could not create " + WORLD_DATA_FOLDER_NAME + " folder. Using parent data folder.");
            worldDataFolder = getDataFolder();
        }

        waxedBlockManager = new WaxedBlockManager(getLogger(), worldDataFolder);
        List<String> waxableMaterialsSubstrings = config.getStringList(CONFIG_KEY_WAXABLE_MATERIALS_SUFFIXES);

        for (Material material : Material.values()) {
            if (waxableMaterials.contains(material)) continue;

            for (String substring : waxableMaterialsSubstrings)
                if (material.toString().endsWith(substring)) waxableMaterials.add(material);
        }

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

                            if (!waxableMaterials.contains(world.getBlockAt(coordinateLocation).getType())) {
                                // The block has since been destroyed indirectly
                                toRemove.add(coordinateLocation);
                                continue;
                            }

                            int distance = Coordinate.calculateFastDistance(Coordinate.fromLocation(player.getLocation()), coordinate);

                            if (distance < particleRange) {
                                Location particleLocation = new Location(world, coordinate.getX() + 0.5, coordinate.getY() + 0.5, coordinate.getZ() + 0.5);
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
        Location location = clickedBlock.getLocation();
        Player player = event.getPlayer();

        if (waxedBlockManager.isWaxed(location)){
            ItemStack handItem = event.getItem();
            if (handItem != null && handItem.getType().isBlock() && player.isSneaking()) return;
            event.setCancelled(true);
            player.playSound(location, Sound.BLOCK_SIGN_WAXED_INTERACT_FAIL, 1.0f, 1.0f);
            Location particleLocation = new Location(location.getWorld(), location.getX() + 0.5, location.getY() + 0.5, location.getZ() + 0.5);
            player.spawnParticle(Particle.WAX_ON, particleLocation, 5, 0.25, 0.25, 0.25, 0);
        } else if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.HONEYCOMB && waxableMaterials.contains(clickedBlock.getType())) {
            event.setCancelled(true);
            takeHeldHoneycomb(player);
            waxBlockAt(location);

            if (clickedBlock.getBlockData() instanceof Door door) {
                Location otherHalfLocation;

                if (door.getHalf() == Bisected.Half.TOP)
                    otherHalfLocation = clickedBlock.getRelative(org.bukkit.block.BlockFace.DOWN).getLocation();
                else otherHalfLocation = clickedBlock.getRelative(org.bukkit.block.BlockFace.UP).getLocation();

                waxBlockAt(otherHalfLocation);
            }
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
        Location eventLocation = event.getBlock().getLocation();
        if (waxedBlockManager.removeWaxedBlock(eventLocation))
            eventLocation.getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.HONEYCOMB));
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

}
