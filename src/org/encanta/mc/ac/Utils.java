package org.encanta.mc.ac;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class Utils {
	private static File getFile(String file) {
		return new File(EncantaAC.getInstance().getDataFolder(), file);
	}

	public static void saveCapturedData(String name, AimDataSeries data)
			throws FileNotFoundException, IOException, InvalidConfigurationException {
		FileConfiguration config = new YamlConfiguration();
		File file = getFile("captures\\" + name + ".yml");
		file.createNewFile();
		config.load(file);
		config.set("stddev", data.getAngleSeries());
		config.set("delta-stddev", data.getDeltaStddev());
		config.save(file);
	}

	public static void initFolders() {
		EncantaAC.getInstance().getDataFolder().mkdirs();
		getFile("captures").mkdirs();
		getFile("categories").mkdirs();
	}

	public static void saveCategory(String category, List<Dataset> samples)
			throws IOException, InvalidConfigurationException {
		FileConfiguration config = new YamlConfiguration();
		File file = getFile("categories\\" + category + ".yml");
		file.createNewFile();
		config.load(file);
		List<Double[]> dumps = new ArrayList<Double[]>();
		for (Dataset dataset : samples) {
			dumps.add(dataset.data);
		}
		config.set("category", category);
		config.set("timestamp", System.currentTimeMillis());
		config.set("samples", dumps);
		config.save(file);
	}

	@SuppressWarnings("unchecked")
	public static List<Dataset> getAllCategory()
			throws FileNotFoundException, IOException, InvalidConfigurationException {
		List<Dataset> stored_samples = new ArrayList<Dataset>();
		File cat_folder = getFile("categories");
		for (File file : cat_folder.listFiles()) {
			FileConfiguration sample = new YamlConfiguration();
			sample.load(file);
			Object[] dumps = sample.getList("samples").toArray();
			for (Object o : dumps) {
				List<Double> line = (List<Double>) o;
				Double[] dump = new Double[line.size()];
				line.toArray(dump);
				stored_samples.add(new Dataset(sample.getString("category"), dump));
			}
		}
		return stored_samples;
	}

	public static void sendInfoToPlayer(Player p)
			throws FileNotFoundException, IOException, InvalidConfigurationException {
		int categoryNum = Utils.getStoredCategoryNum();
		p.sendMessage(ChatColor.AQUA + "  Stored " + categoryNum + (categoryNum != 1 ? " categories" : "category"));
		File cat_folder = getFile("categories");
		for (File file : cat_folder.listFiles()) {
			FileConfiguration sample = new YamlConfiguration();
			sample.load(file);
			p.sendMessage("   Category " + ChatColor.YELLOW + sample.getString("category") + ChatColor.RESET
					+ " sample_size: " + ChatColor.YELLOW + sample.getList("samples").size() + ChatColor.RESET
					+ " timestamp: " + ChatColor.YELLOW + sample.getString("timestamp"));
		}
	}

	public static int getStoredCategoryNum() {
		return getFile("categories").listFiles().length;
	}

}
