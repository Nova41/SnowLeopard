package org.encanta.mc.ac;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.InvalidConfigurationException;

public class AimDataSeries {
	private List<Float> viewAngles;

	public AimDataSeries() {
		this.viewAngles = new ArrayList<Float>();
	}

	public void add(float angle) {
		this.viewAngles.add(angle);
	}

	public void clear() {
		this.viewAngles.clear();
	}

	public List<Float> getAngleSeries() {
		return this.viewAngles;
	}

	public double getClicksPerSec(long trainTimeLength) {
		return Math.sqrt(this.getLength());
	}

	public double getStddev() {
		float stddev = 0;
		for (float singleData : viewAngles) {
			stddev += Math.pow(singleData - this.getMean(), 2);
		}
		return Math.sqrt(stddev / this.getLength());
	}

	public double getMean() {
		return (this.getSum() / this.getLength());
	}

	public double getDeltaStddev() {
		double delta_stddev = 0;
		double[] deltas = this.getDeltas();
		for (double delta : deltas) {
			delta_stddev += Math.pow(delta - this.getDeltaMean(), 2);
		}
		return Math.sqrt(delta_stddev / this.getDeltaLength());
	}

	public double getDeltaMean() {
		return (this.getDeltaSum() / this.getDeltaLength());
	}

	public double getDeltaSum() {
		double delta_sum = 0;
		double[] deltas = this.getDeltas();
		for (double delta_line : deltas) {
			delta_sum += delta_line;
		}
		return delta_sum;
	}

	
	//Add a check here to make sure the delta array is not less than 0. Not going to add this as I don't know how this exactly works and I don't
	//want to mess up the algorithm.
	public double[] getDeltas() {
		double[] deltas = new double[this.getDeltaLength()];
		for (int i = 0; i <= this.getDeltaLength() - 1; i++) {
			deltas[i] = Math.abs(this.getAngleSeries().get(i + 1) - this.getAngleSeries().get(i));
		}
		return deltas;
	}

	public Double[] getAllDump() {
		return new Double[]{this.getStddev(), this.getDeltaStddev(), this.getMean(), this.getDeltaMean()};
	}
	
	public int getDeltaLength() {
		return this.getLength() - 1;
	}
	
	public int getLength() {
		return this.viewAngles.size();
	}

	public float getSum() {
		float sum = 0;
		for (float singleData : this.getAngleSeries()) {
			sum += singleData;
		}
		return sum;
	}

	public void save(String filename) throws FileNotFoundException, IOException, InvalidConfigurationException {
		Utils.saveCapturedData(filename, this);
	}
}
