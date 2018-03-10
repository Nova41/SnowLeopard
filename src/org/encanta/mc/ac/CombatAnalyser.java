package org.encanta.mc.ac;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class CombatAnalyser {
	private EncantaAC main;
	private LVQNeuronNetwork lvq;

	public CombatAnalyser(EncantaAC main) {
		this.main = main;

	}

	public void rebuild() throws FileNotFoundException, IOException, InvalidConfigurationException {
		this.lvq = new LVQNeuronNetwork(0.2, 0.95);
		for (Dataset dataset : Utils.getAllCategory())
			lvq.input(dataset);
		lvq.normalize();
		lvq.initialize();
		lvq.trainUntil(0.00000000001);
	}

	public void rebuild(Player callback) throws FileNotFoundException, IOException, InvalidConfigurationException {
		this.lvq = new LVQNeuronNetwork(0.2, 0.95);
		for (Dataset dataset : Utils.getAllCategory())
			lvq.input(dataset);
		lvq.normalize();
		lvq.initialize();
		callback.sendMessage(
				ChatColor.GREEN + "Rebuilded neuron network with epoch(es) " + lvq.trainUntil(0.00000000001));
	}

	public void sendAnalyse(Player callback, Double[] dump) {
		lvq.printPredictResult(dump);
		callback.sendMessage(ChatColor.GOLD + "** Analysis Report **");
		callback.sendMessage(ChatColor.GREEN + "  Best matched: " + ChatColor.YELLOW + lvq.predict(dump).bestMatched);
		callback.sendMessage(
				ChatColor.GREEN + "  Euclidean distance: " + ChatColor.YELLOW + lvq.predict(dump).distance);
	}

	public void sendInfoToPlayer(Player p) {
		p.sendMessage(ChatColor.AQUA + "  Neuron network: ");
		p.sendMessage("   Input layer: " + ChatColor.YELLOW + lvq.getInputLayerSize());
		p.sendMessage("   Output layer: " + ChatColor.YELLOW + lvq.getOutputLayerSize());
		lvq.print_outputlayers();

		try {
			Utils.sendInfoToPlayer(p);
		} catch (IOException | InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
