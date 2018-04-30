/*
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

public class LVQNeuralNetwork {
	
	/**
	 * A java implementation of learning-vector-quanzitation neural network
	 * 
	 * References: T. Kohonen, "Improved Versions of Learning Vector
	 * Quantization", International Joint Conference on Neural Networks (IJCNN),
	 * 1990.
	 * 
	 * @author Nova41
	 * 
	 */
	 
	private List<Dataset> input_layer;
	private List<Dataset> output_layer;
	private Map<Double, Dataset> distances;
	private int input_features;
	private double step_alpha;
	private double step_alpha_del_rate;
	private List<Double> input_mins;
	private List<Double> input_maxs;
	
	/**
	 * The constructor of the LVQ neural network.
	 * 
	 * @param features
	 *            the number of the features
	 * @param step_alpha
	 * @param step_alpha_del_rate
	 * 
	 */
	
	public LVQNeuralNetwork(double step_alpha, double step_alpha_del_rate) {
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
			distances.put(distance, entry);
		}
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

	public void move(Dataset input, int output_index) {
		Dataset output = this.output_layer.get(output_index);
		if (input.category.equals(output.category)) {
			for (int i = 0; i <= this.input_features - 1; i++)
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
			this.train();
			generate++;
			this.step_alpha *= this.step_alpha_del_rate;
			//System.out.println(">> generate: " + generate + " epoch: " + this.step_alpha);
			//this.print_outputlayers();
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
