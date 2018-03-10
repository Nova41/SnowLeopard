package org.encanta.mc.ac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class LVQNeuronNetwork {
	private List<Dataset> input_layer;
	private List<Dataset> output_layer;
	private Map<Double, Dataset> distances;
	private int input_features;
	private double step_alpha;
	private double step_alpha_del_rate;
	private List<Double> input_mins;
	private List<Double> input_maxs;

	public LVQNeuronNetwork(double step_alpha, double step_alpha_del_rate) {
		this.input_layer = new ArrayList<Dataset>();
		this.output_layer = new ArrayList<Dataset>();
		this.distances = new HashMap<Double, Dataset>();
		this.step_alpha = step_alpha;
		this.step_alpha_del_rate = step_alpha_del_rate;
		this.input_mins = new ArrayList<Double>();
		this.input_maxs = new ArrayList<Double>();
	}

	public void reset() {
		this.input_layer = new ArrayList<Dataset>();
		this.output_layer = new ArrayList<Dataset>();
		this.distances = new HashMap<Double, Dataset>();
		this.input_mins = new ArrayList<Double>();
		this.input_maxs = new ArrayList<Double>();
	}
	
	public void initialize() {
		// initialize output layers according to the number of egories and features
		Set<String> set = new HashSet<String>();
		for (Dataset entry : this.input_layer) {
			set.add(entry.category);
		}
		for (String category : set) {
			Double[] randOutput = new Double[input_features];
			for (int i = 0; i <= input_features - 1; i++)
				randOutput[i] = randdouble();
			output_layer.add(new Dataset(category, randOutput));
		}
	}

	public void normalize() {
		List<Double> featureColumn = new ArrayList<Double>();
		for (int i = 0; i <= this.input_features - 1; i++) {
			for (Dataset entry : this.input_layer) {
				featureColumn.add(entry.data[i]);
			}
			double min = this.getMin(featureColumn);
			double max = this.getMax(featureColumn);
			this.input_mins.add(min);
			this.input_maxs.add(max);
			for (Dataset entry : this.input_layer) {
				entry.data[i] = (entry.data[i] - min) / (max - min);
			}
			featureColumn.clear();
		}
	}
	
	public Double[] normalizeInput(Double[] input) {
		Double[] normalized = input.clone();
		for (int i = 0; i <= this.input_features - 1; i++) {
			double min = this.input_mins.get(i);
			double max = this.input_maxs.get(i);
			normalized[i] = (normalized[i] - min) / (max - min);
		}
		return normalized;
	}
		
	public PredictResult predict(Double[] input) {
		String matchedCat = this.output_layer.get(this.getWinner(this.normalizeInput(input))).category;
		Double matchedDis = this.getWinnerDistance(this.normalizeInput(input));
		PredictResult result = new PredictResult(matchedCat, matchedDis);
		return result;
	}
	
	public void printPredictResult(Double[] input) {
		this.print_inputlayers();
		this.print_outputlayers();
		System.out.println("Input:" + Arrays.asList(input));
		System.out.println("Normalized input: " + Arrays.asList(this.normalizeInput(input)));
	}

	public void input(String category, Double[] data) {
		this.input_layer.add(new Dataset(category, data));
		//this.input_layer_old.add(new Dataset(category, data));
		// simulate how many features(dimensions) do the input data have
		//Object[] values = this.input_layer.toArray();
		//input_features = ((Dataset) values[0]).data.length;
		input_features = data.length;
	}
	
	public void input(Dataset dataset) {
		this.input_layer.add(dataset);
		input_features = dataset.data.length;
	}

	public int getWinner(Double[] input) {
		// Winner is the neuron nearest to the input
		// return the index of the winner in the output_layer
		this.distances.clear();
		for (Dataset entry : this.output_layer) {
			double distance = 0;
			Double[] output = entry.data;
			for (int i = 0; i <= input.length - 1; i++)
				distance += Math.pow(input[i] - output[i], 2);
			// I don't open root here as it's unnecessary
			// System.out.println(distance);
			distances.put(distance, entry);
		}
		//System.out.println("Winner: " + getMinKey(distances).category);
		// Arrays.asList(getMinKey(distances).data));
		// System.out.println(this.output_layer.indexOf(getMinKey(distances)));
		return this.output_layer.indexOf(distances.get(getMinKey(distances)));
	}
	
	public double getWinnerDistance(Double[] input) {
		this.distances.clear();
		for (Dataset entry : this.output_layer) {
			double distance = 0;
			Double[] output = entry.data;
			for (int i = 0; i <= input.length - 1; i++)
				distance += Math.pow(input[i] - output[i], 2);
			distances.put(distance, entry);
		}
		return getMinKey(distances);
	}

	// 将输出节点朝着当前的节点逼近或远离
	public void move(Dataset input, int output_index) {
		Dataset output = this.output_layer.get(output_index);
		if (input.category.equals(output.category)) {
			for (int i = 0; i <= this.input_features - 1; i++)
				// + and - alternatively changes
				this.output_layer.get(output_index).data[i] += this.step_alpha * (input.data[i] - output.data[i]);
		} else {
			for (int i = 0; i <= this.input_features - 1; i++)
				this.output_layer.get(output_index).data[i] -= this.step_alpha * (input.data[i] - output.data[i]);
		}
	}

	public void train() {
		for (Dataset input : this.input_layer) {
			this.move(input, this.getWinner(input.data));
		}
	}

	public int trainUntil(double max_epoch) {
		int generate = 0;
		while (this.step_alpha >= max_epoch) {
			// System.out.println("==============================================");
			this.train();
			generate++;
			this.step_alpha *= this.step_alpha_del_rate; // 步长衰减
			System.out.println(">> generate: " + generate + " epoch: " + this.step_alpha);
			this.print_outputlayers();
			/*try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
		return generate;
	}

	public Dataset getInput(int index) {
		return this.input_layer.get(index);
	}

	public void print_inputlayers() {
		System.out.println("Input layers: " + this.input_layer.size() + " category(s)");
		for (Dataset input : this.input_layer) {
			System.out.println("  " + input.category + " " + Arrays.asList(input.data));
		}
	}
	
	public void print_outputlayers() {
		System.out.println("Output layers: " + this.output_layer.size() + " category(s)");
		for (Dataset output : this.output_layer) {
			System.out.println("  " + output.category + " " + Arrays.asList(output.data));
		}
	}
	
	public int getInputLayerSize() {
		return this.input_layer.size();
	}
	
	public int getOutputLayerSize() {
		return this.output_layer.size();
	}

	private double randdouble() {
		return new Random().nextDouble();
	}

	private Double getMinKey(Map<Double, Dataset> map) {
		if (map == null)
			return null;
		Set<Double> set = map.keySet();
		Object[] obj = set.toArray();
		Arrays.sort(obj);
		//return distances.get((Double) obj[0]);
		return (Double) obj[0];
	}

	
	private Double getMin(List<Double> list) {
		Collections.sort(list);
		return list.get(0);
	}
	
	private Double getMax(List<Double> list) {
		Collections.sort(list);
		Collections.reverse(list);
		return list.get(0);
	}
}

class Dataset {
	String category;
	Double[] data;

	public Dataset(String category, Double[] data) {
		this.category = category;
		this.data = data;
	}
	
	public String getCategory() {
		return this.category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}
	
	public Double[] getData() {
		return this.data;
	}

	
	public void setData(Double[] data) {
		this.data = data;
	}
}

class PredictResult{
	String bestMatched;
	double distance;
	
	public PredictResult(String bestMatched, double distance) {
		this.bestMatched = bestMatched;
		this.distance = distance;
	}
}
