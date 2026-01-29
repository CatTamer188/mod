package com.example.levitationarrows;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.Random;

public class LevitationArrows extends JavaPlugin implements Listener, CommandExecutor {

    private double launchSpeed;
    private double impSpawnChance;
    private int impLevDuration;

    private NamespacedKey featherKey;
    private NamespacedKey arrowKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadValues();

        featherKey = new NamespacedKey(this, "feather_of_flight");
        arrowKey = new NamespacedKey(this, "levitation_arrow");

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("lareload").setExecutor(this);

        registerRecipes();
    }

    private void loadValues() {
        FileConfiguration cfg = getConfig();
        launchSpeed = cfg.getDouble("launch-speed");
        impSpawnChance = cfg.getDouble("imp-spawn-chance");
        impLevDuration = cfg.getInt("imp-lev-duration");
    }

    private void registerRecipes() {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r.getKey() != null && (r.getKey().equals(featherKey) || r.getKey().equals(arrowKey))) {
                it.remove();
            }
        }

        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta fm = feather.getItemMeta();
        fm.setDisplayName("§bFeather of Flight");
        feather.setItemMeta(fm);

        ShapelessRecipe featherRecipe = new ShapelessRecipe(featherKey, feather);
        featherRecipe.addIngredient(4, Material.PRISMARINE_CRYSTALS);
        featherRecipe.addIngredient(Material.FEATHER);
        Bukkit.addRecipe(featherRecipe);

        ItemStack arrow = new ItemStack(Material.TIPPED_ARROW, 4);
        ItemMeta am = arrow.getItemMeta();
        am.setDisplayName("§dLevitation Arrow");
        arrow.setItemMeta(am);

        ShapelessRecipe arrowRecipe = new ShapelessRecipe(arrowKey, arrow);
        arrowRecipe.addIngredient(Material.ARROW);
        arrowRecipe.addIngredient(new RecipeChoice.ExactChoice(feather));
        Bukkit.addRecipe(arrowRecipe);
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(event.getHitEntity() instanceof Player player)) return;

        ItemStack stack = arrow.getItemStack();
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (!"§dLevitation Arrow".equals(meta.getDisplayName())) return;

        player.setVelocity(player.getVelocity().setX(0).setZ(0).setY(launchSpeed));
        player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);

        arrow.remove();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        if (Math.random() > impSpawnChance) return;

        World world = event.getWorld();
        int x = (event.getChunk().getX() << 4) + new Random().nextInt(16);
        int z = (event.getChunk().getZ() << 4) + new Random().nextInt(16);
        int y = world.getHighestBlockYAt(x, z) + 1;

        Silverfish imp = (Silverfish) world.spawnEntity(
                world.getBlockAt(x, y, z).getLocation(),
                EntityType.SILVERFISH
        );

        imp.setCustomName("§5Floating Imp");
        imp.setCustomNameVisible(true);
        imp.setSilent(true);
        imp.addPotionEffect(
                new PotionEffect(PotionEffectType.LEVITATION, impLevDuration, 1, true, false)
        );
    }

    @EventHandler
    public void onImpDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Silverfish imp)) return;
        if (!"§5Floating Imp".equals(imp.getCustomName())) return;

        event.getDrops().clear();
        event.getDrops().add(new ItemStack(Material.PRISMARINE_CRYSTALS, 1 + new Random().nextInt(2)));

        if (Math.random() < 0.1) {
            ItemStack feather = new ItemStack(Material.FEATHER);
            ItemMeta fm = feather.getItemMeta();
            fm.setDisplayName("§bFeather of Flight");
            feather.setItemMeta(fm);
            event.getDrops().add(feather);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        reloadConfig();
        loadValues();
        registerRecipes();
        sender.sendMessage("§aLevitationArrows reloaded.");
        return true;
    }
}

