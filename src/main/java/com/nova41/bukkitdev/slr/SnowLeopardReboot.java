package com.nova41.bukkitdev.slr;

import com.nova41.bukkitdev.slr.command.CommandManager;
import com.nova41.bukkitdev.slr.listener.PlayerAttackAngleLogger;
import com.nova41.bukkitdev.slr.model.LVQNeuralNetwork;
import com.nova41.bukkitdev.slr.model.LVQNeuralNetworkPredictResult;
import com.nova41.bukkitdev.slr.model.LVQNeuralNetworkSummary;
import com.nova41.bukkitdev.slr.model.LabeledData;
import com.nova41.bukkitdev.slr.util.FileUtil;
import com.nova41.bukkitdev.slr.util.MathUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Consumer;

/**
 * SnowLeopardR - a rebooted version of SnowLeopard
 * All algorithms and designs could be found at https://www.spigotmc.org/threads/machine-learning-killaura-detection-in-minecraft.301609/
 *
 * @author Nova41
 */
public class SnowLeopardReboot extends JavaPlugin {
    /* Directory names */
    public static String DIRNAME_CATEGORY = "category";         // Directory containing all learned categories
    public static String DIRNAME_DUMPED_DATA = "dumped_data";   // Directory containing dumped angle sequence and features

    // How many features are there in our dataset; 4 currently
    public static int FEATURE_COUNT = 4;

    // Built-in neural network for category classification
    private volatile LVQNeuralNetwork neuralNetwork;

    // Map category names to an integer, because for the network, a class is an integer, not a string
    private Map<String, Integer> categoryNameMap = new HashMap<>();

    // Logger for logging angle sequence produced by players
    private PlayerAttackAngleLogger angleLogger;

    // Manager managing all sub commands of the plugin
    private CommandManager commandManager;

    public void onEnable() {
        // Initialize data folder
        FileUtil.createDirectoryIfAbsent(getDataFolder(), DIRNAME_CATEGORY);
        FileUtil.createDirectoryIfAbsent(getDataFolder(), DIRNAME_DUMPED_DATA);
        try {
            FileUtil.saveResourceIfAbsent(this, "config.yml", "config.yml");
        } catch (IOException e) {
            getLogger().severe("Unable to save resource file");
            e.printStackTrace();
        }

        // Rebuild the built-in neural network with parameters specified in config.yml
        rebuildNetworkWithDataset();

        // Logger for logging angle sequence produced by players
        angleLogger = new PlayerAttackAngleLogger();
        getServer().getPluginManager().registerEvents(angleLogger, this);

        // Manager managing all sub commands of the plugin
        commandManager = new CommandManager(this, "slr");

        // Register sub commands
        registerCommands();
    }

    // Rebuild the built-in neural network with parameters specified in config.yml and dataset stored in category folder
    @SuppressWarnings("unchecked")
    private void rebuildNetworkWithDataset() {
        // Build the network with parameters specified in config.yml
        double step_size = getConfig().getDouble("LVQNN_parameters.step_size");
        double step_dec_rate = getConfig().getDouble("LVQNN_parameters.step_dec_rate");
        double min_step_size = getConfig().getDouble("LVQNN_parameters.min_step_size");
        neuralNetwork = new LVQNeuralNetwork(SnowLeopardReboot.FEATURE_COUNT, step_size, step_dec_rate, min_step_size);

        // Read dataset from category folder and train the network with the dataset
        unregisterAllCategories();

        File[] categoryFiles = new File(getDataFolder(), "category").listFiles();
        if (categoryFiles == null) {
            getLogger().severe("Unable to read dataset: 'category' is not a directory or an I/O error occurred");
            return;
        }

        if (categoryFiles.length == 0)
            return;

        for (File categoryFile : categoryFiles)
            try {
                FileConfiguration categoryFileYaml = new YamlConfiguration();
                categoryFileYaml.load(categoryFile);
                List<List<Double>> categorySamples = (List<List<Double>>) categoryFileYaml.getList("samples");

                // The network distinguish different classes with their category id (an integer)
                // So we need to map the category's name (string) to category id (an integer) for the network
                String categoryName = FilenameUtils.removeExtension(categoryFile.getName());
                if (!categoryNameMap.containsKey(categoryName))
                    registerCategory(categoryName);
                int categoryID = categoryNameMap.get(categoryName);

                // Add the parsed data to our dataset
                for (List<Double> samples : categorySamples)
                    neuralNetwork.addData(new LabeledData(categoryID, samples.stream().mapToDouble(e -> e).toArray()));

            } catch (InvalidConfigurationException | IOException e) {
                e.printStackTrace();
                getLogger().severe("Unable to read dataset from '" + categoryFile.getName() + "'");
            }

        // train asynchronously
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            int epoch = getConfig().getInt("LVQNN_train.epoch");
            neuralNetwork.normalize();
            neuralNetwork.initializeOutputLayer();

            // (not sure) i don't think neuralNetwork will change its reference during training...??
            synchronized (neuralNetwork) {
                for (int i = 0; i <= epoch - 1; i++)
                    neuralNetwork.train();
            }
        });
    }

    private void registerCommands() {
        /* Sub Commands */
        // /slr
        commandManager.register("", (sender, params) -> {
            sender.sendMessage(ChatColor.GREEN + "SnowLeopardR " + ChatColor.YELLOW + getDescription().getVersion() + ChatColor.GOLD + " Made by Nova41 with ❤");
            sender.sendMessage(ChatColor.GREEN + "Project github page: ");
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + "https://github.com/Nova41/SnowLeopard/");
            sender.sendMessage(ChatColor.YELLOW + "/slr info" + ChatColor.WHITE + " Display network statistics");
            sender.sendMessage(ChatColor.YELLOW + "/slr train <category>" + ChatColor.WHITE + " Train the network with a new category");
            sender.sendMessage(ChatColor.YELLOW + "/slr rebuild" + ChatColor.WHITE + " Reload dataset and rebuild the network");
            sender.sendMessage(ChatColor.YELLOW + "/slr reload" + ChatColor.WHITE + " Reload configurations");
            sender.sendMessage(ChatColor.YELLOW + "/slr test <player> [duration]" + ChatColor.WHITE + " Classify motion of a player");
            sender.sendMessage(ChatColor.YELLOW + "/slr mob" + ChatColor.WHITE + " Spawn a punchbag villager (50 hearts) for sampling or testing");
        });

        // /slr version
        commandManager.register("version", ((sender, params) -> {
            sender.sendMessage(ChatColor.GREEN + "You are running SnowLeopardR " + ChatColor.YELLOW + getDescription().getVersion());
            sender.sendMessage(ChatColor.GOLD + "Made by Nova41 with ❤");
            sender.sendMessage(ChatColor.GREEN + "Project github page: " + ChatColor.AQUA + ChatColor.UNDERLINE + "https://github.com/Nova41/SnowLeopard/");
        }));

        // /slr start
        commandManager.register("start", ((sender, params) -> {
            if (CommandValidate.notPlayer(sender)) return;

            Player player = (Player) sender;
            angleLogger.registerPlayer(player); // start logging angles
            sender.sendMessage(ChatColor.GREEN + "Started logging angles for " + ChatColor.YELLOW + player.getName());

        }));

        // /slr stop
        commandManager.register("stop", (sender, params) -> {
            if (CommandValidate.notPlayer(sender)) return;

            Player player = (Player) sender;
            if (!angleLogger.getRegisteredPlayers().contains(player.getName())) {
                sender.sendMessage(ChatColor.RED + "You haven't started logging angles for " + ChatColor.YELLOW + player.getName());
                return;
            }

            angleLogger.unregisterPlayer(player); // stop logging angles
            sender.sendMessage(ChatColor.RED + "Stopped logging angles for " + ChatColor.YELLOW + player.getName());

            // save logged angle sequence
            try {
                List<Float> angleSequence = angleLogger.getLoggedAngles(player);
                double[] extractedFeatures = MathUtil.extractFeatures(angleSequence);

                // specify the name of file containing the sequence and features extracted from it
                String saveFileName = SnowLeopardReboot.DIRNAME_DUMPED_DATA + File.separator + System.currentTimeMillis() + ".yml";
                // write the sequence and the features to the file
                File saveFile = new File(getDataFolder(), saveFileName);
                if (!saveFile.createNewFile())
                    throw new IOException();

                FileConfiguration saveFileYaml = new YamlConfiguration();
                saveFileYaml.set("feature", extractedFeatures);
                saveFileYaml.set("raw_angles", angleSequence);
                saveFileYaml.save(saveFile);

                sender.sendMessage(ChatColor.GREEN + "Data have been saved to " + ChatColor.YELLOW + saveFileName
                        + ChatColor.GREEN + " (" + angleSequence.size() + " samples).");

                // The sequence saved to disk is no longer needed to be kept in memory.
                angleLogger.clearLoggedAngles(player);
            } catch (IOException e) {
                getLogger().severe("Unable to dump vector and angles of player '" + player.getName() + "'");
                e.printStackTrace();
                sender.sendMessage(ChatColor.RED + "Failed to save logged angles due to an I/O error");
            }
        });

        // /slr info
        commandManager.register("info", (sender, params) -> {
            LVQNeuralNetworkSummary summary = neuralNetwork.getSummaryStatistics();
            sender.sendMessage(ChatColor.AQUA + "Neural network layer statistics: ");
            sender.sendMessage(ChatColor.AQUA + "  Dataset size: " + ChatColor.YELLOW + summary.getInputCount());
            sender.sendMessage(ChatColor.AQUA + "  Output layer: " + ChatColor.YELLOW + summary.getOutputCount() + " neuron(s)");
            sender.sendMessage(ChatColor.AQUA + "Neural network learning statistics:");
            sender.sendMessage(ChatColor.AQUA + "  Epoch: " + ChatColor.YELLOW + summary.getEpoch());
            sender.sendMessage(ChatColor.AQUA + "  Current step size: " + ChatColor.YELLOW + summary.getCurrentStepSize());
            sender.sendMessage(ChatColor.AQUA + "Category statistics:");
            sender.sendMessage(ChatColor.AQUA + "  Loaded: " + ChatColor.YELLOW + categoryNameMap.size());
            sender.sendMessage(ChatColor.AQUA + "  Mappings: ");
            categoryNameMap.forEach((cat, id) -> sender.sendMessage(ChatColor.YELLOW + "  - [" + id + "] " + cat));
        });

        // /slr train <category-name>
        commandManager.register("train", (sender, params) -> {
            if (CommandValidate.notPlayer(sender))
                return;

            // Check the number of params
            if (params.length != 1) {
                sender.sendMessage(ChatColor.RED + "Wrong parameters! /slr train <category-name>");
                return;
            }

            // Read configurations of sampler from config.yml. See config.yml for further information
            int duration_to_generate_a_vector = getConfig().getInt("sampler.duration_to_generate_a_vector");
            int vector_per_category = getConfig().getInt("sampler.vector_per_category");
            trainNetwork((Player) sender, params[0], duration_to_generate_a_vector, vector_per_category);
        });

        // /slr reload
        commandManager.register("reload", (sender, params) -> {
            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Reloaded config.");
        });

        // /slr rebuild
        commandManager.register("rebuild", ((sender, params) -> {
            rebuildNetworkWithDataset();
            sender.sendMessage(ChatColor.GREEN + "Rebuilt neural network.");
        }));

        // /slr test <player-name> <seconds>
        commandManager.register("test", (sender, params) -> {
            // Check the number of arguments
            if (params.length != 1 && params.length != 2) {
                sender.sendMessage(ChatColor.RED + "Wrong parameters! /slr test <player-name> [seconds]");
                return;
            }

            Player testPlayer = getServer().getPlayer(params[0]);

            // Check if the specified player is online
            if (testPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Unable to find the player " + ChatColor.YELLOW + params[0]);
                return;
            }

            // Check if the given duration is valid
            if (params.length == 2)
                if (!StringUtils.isNumeric(params[1])) {
                    sender.sendMessage(ChatColor.YELLOW + params[1] + ChatColor.RED + " is not a valid number");
                    return;
                }

            // If the duration is not set, it would be the default duration specified in config.yml
            int duration = params.length == 1 ? getConfig().getInt("test.default_duration") : Integer.valueOf(params[1]);

            sender.sendMessage(ChatColor.GREEN + "Attempting to sample motion of " + ChatColor.YELLOW + params[0]
                    + ChatColor.GREEN + " for " + ChatColor.YELLOW + duration + ChatColor.GREEN + " seconds");
            classifyPlayer(testPlayer, duration , result -> {
                // likelihood =  max possible difference - difference
                double likelihood = MathUtil.round(result.getLikelihood() * 100, 2, RoundingMode.HALF_UP);

                sender.sendMessage(ChatColor.GREEN + "Neural network classification result:");
                sender.sendMessage(ChatColor.GREEN + "  Best matched: " + ChatColor.YELLOW + getCategoryNameFromID(result.getCategory()));
                sender.sendMessage(ChatColor.GREEN + "  Difference: " + ChatColor.YELLOW + result.getDifference());
                sender.sendMessage(ChatColor.GREEN + "  Likelihood: " + ChatColor.YELLOW + likelihood + "%");
            });
        });

        // /slr mob
        commandManager.register("mob", (sender, params) -> {
            if (CommandValidate.notPlayer(sender))
                return;

            // Spawn a punchbag villager (having 50 hearts) in front of the player for training or practicing
            Player p = (Player) sender;
            Location spawnLoc = p.getLocation().add(p.getEyeLocation().getDirection().multiply(5));
            spawnLoc.setY(spawnLoc.getY() + 2);
            Villager villager = (Villager) p.getWorld().spawnEntity(spawnLoc, EntityType.VILLAGER);
            villager.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100.0D);
            villager.setHealth(100.0D);
        });

        // /slr _printnn
        commandManager.register("_printnn", (sender, params) -> neuralNetwork.printStats(getLogger()));
    }

    // Train the network. Tell the network there is a category waiting to be learned from player.
    @SuppressWarnings("unchecked")
    public void trainNetwork(Player player, String category, int duration_to_generate_a_vector, int vector_per_category) {
        // Check if angle sequence of the given player is already being logged
        if (angleLogger.getRegisteredPlayers().contains(player.getName())) {
            player.sendMessage(ChatColor.RED + "Player is already in a sampling process. Please stop sampling first.");
            return;
        }

        // display training parameters
        player.sendMessage(ChatColor.GREEN + "Attempt to sample player's motion. ("
                + ChatColor.YELLOW + duration_to_generate_a_vector + ChatColor.GREEN + " ms for a vector, "
                + ChatColor.YELLOW + vector_per_category + ChatColor.GREEN + " vectors are needed in total)");

        /* FIRST. We need to get angle sequence of a player, and then extract features from the sequence */

        // do asynchronously
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            // vectors are stored here
            double[][] vectors = new double[vector_per_category][SnowLeopardReboot.FEATURE_COUNT];

            angleLogger.registerPlayer(player);

            // sample for the given times
            for (int i = 1; i <= vector_per_category; i++) {
                player.sendMessage(ChatColor.GREEN + "Sampling player's motion (" + i + "/" + vector_per_category + ")");
                try {
                    // while we wait, the angleLogger is logging player's angles unweariedly
                    Thread.sleep(duration_to_generate_a_vector);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // get the angle sequence containing angles in the past duration_to_generate_a_vector milliseconds
                List<Float> angleSequence = angleLogger.getLoggedAngles(player);
                // do nothing if the player does not attack anybody in the past duration
                if (angleSequence == null)
                    break;
                // extract the features from the sequence and save them to a temporary array
                vectors[i - 1] = MathUtil.extractFeatures(angleSequence);
                // clear saved angles to get ready for new angles
                angleLogger.clearLoggedAngles(player);
            }
            angleLogger.unregisterPlayer(player);
            player.sendMessage(ChatColor.GREEN + "Finished sampling player's motion. Saving samples...");

            /* SECOND. Save the vector expressing properties of the sequence to disk. */
            /*         our leopard will use these vectors to train the network */
            try {
                // get the file to be saved
                File saveFile = new File(getDataFolder(), SnowLeopardReboot.DIRNAME_CATEGORY + File.separator + category + ".yml");
                FileConfiguration saveFileYaml = new YamlConfiguration();

                List<Double[]> samplesSection = new ArrayList<>();
                // append vectors to the category if the category already exists
                if (!saveFile.createNewFile()) {
                    saveFileYaml.load(saveFile);
                    samplesSection.addAll((Collection<? extends Double[]>) saveFileYaml.getList("samples"));
                    player.sendMessage(ChatColor.GREEN + "Category " + ChatColor.YELLOW + category + ChatColor.GREEN
                            + " already exists. Appending samples to the category...");
                }
                for (double[] vector : vectors)
                    samplesSection.add(ArrayUtils.toObject(vector));

                saveFileYaml.set("samples", samplesSection);
                saveFileYaml.save(saveFile);

                player.sendMessage(ChatColor.GREEN + "Samples saved.");
            } catch (IOException | InvalidConfigurationException e) {
                getLogger().severe("Unable to save sample for category '" + category + "'");
                e.printStackTrace();
                player.sendMessage(ChatColor.RED + "Unable to save samples due to an I/O error");
            }
        });
    }

    //returns whether or not player is being classifed
    public boolean isTesting(Player player) {
        return angleLogger.getRegisteredPlayers().contains(player.getName());
    }
    
    public void stopTesting(Player player) {
        if(!isTesting(player) {
            return;
        }
        //TODO: FOR NOVA!!! I don't fully know what is done to the player while they're being tested, but it looks like AT LEAST the following
        //should be done. The scheduler should be stopped as well if possible! In this case I'd consider using a BukkitRunnable as these can be cancelled.
        angleLogger.unregisterPlayer(player);
        angleLogger.clearLoggedAngles(player);
        //HERE IS WHERE THE TASK SHOULD BE CANCELLED!
     }
           
    
    // Let the network guess which category does the player result in
    public void classifyPlayer(Player player, int duration, Consumer<LVQNeuralNetworkPredictResult> consumer) {
        // Check if angle sequence of the given player is already being logged
        if (angleLogger.getRegisteredPlayers().contains(player.getName())) {
            player.sendMessage(ChatColor.YELLOW + player.getName() + ChatColor.RED + " is already in a sampling process. Please stop sampling first.");
            return;
        }

        angleLogger.registerPlayer(player);
        getServer().getScheduler().runTaskLater(this, () -> {
            List<Float> angleSequence = angleLogger.getLoggedAngles(player);
            angleLogger.unregisterPlayer(player);
            angleLogger.clearLoggedAngles(player);

            // Call the consumer with classification result
            double[] extractedFeatures = MathUtil.extractFeatures(angleSequence);
            consumer.accept(neuralNetwork.predict(extractedFeatures));
        }, duration * 20L);
    }

    // Register a category. Map a unique id to it.
    private void registerCategory(String name) {
        categoryNameMap.put(name, categoryNameMap.size());
    }

    // Convert category id from a predict result of the neural network to actual name of category
    public String getCategoryNameFromID(int id) {
        for (Map.Entry<String, Integer> entry : categoryNameMap.entrySet())
            if (entry.getValue() == id)
                return entry.getKey();
        return null;
    }

    // Clear category mappings, used when you need to reload all stored samples.
    private void unregisterAllCategories() {
        categoryNameMap.clear();
    }

    // Validation methods useful for processing commands
    private static final class CommandValidate {
        // Validate if sender is not a instance of player and notify the sender upon failed validation
        private static boolean notPlayer(CommandSender sender) {
            if (!(sender instanceof Player))
                sender.sendMessage(ChatColor.RED + "This command can only executed by a player.");
            return !(sender instanceof Player);
        }
    }

}
