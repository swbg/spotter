package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
// import java.util.ArrayList;
// import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

public class Analysis implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Config config;
	private double[][] spots;
	private double[] mean;
	private double[] sd;
	// private ArrayList<Pair<String, Double>> scores;
	public boolean analyzed;
	
	private class MaxComparator implements Comparator<Short> {

		@Override
		public int compare(Short o1, Short o2) {
			return o1 > o2 ? 1 : -1;
		}	
	}
	
	/*
	private class PairComparator<K, V extends Comparable<V>> implements Comparator<Pair<K, V>> {

		@Override
		public int compare(Pair<K, V> o1, Pair<K, V> o2) {
			return o1.value.compareTo(o2.value);
		}	
	}
	*/
	
	/*
	private class Pair<K, V> implements Serializable {
		private static final long serialVersionUID = 3L;
		public K key;
		public V value;
		
		Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public String toString() {
			return key + ": " + value;
		}
	}
	*/
	
	public Analysis(Config config) {
		this.config = config;
		analyzed = false;
	}
	
	public void analyzeConfig() {
		analyzeConfig(false);
	}
	
	public void analyzeConfig(boolean autoMask) {
		calculateSpots();
		if (autoMask) {
			autoMask();
		}
		calculateStatistics();
		// calculateType();
		analyzed = true;
	}
	
	/*
	private void calculateType() {
		ArrayList<String> types = config.getController().prop.types;
		ArrayList<Double[]> typeMeans = config.getController().prop.typeMeans;
		if (typeMeans.size() < 1) {
			System.out.println("No type data available.");
			return;
		}
		int size = typeMeans.get(0).length;
		if (size != mean.length) {
			System.out.println("Spot pattern does not match type pattern. Restricting analysis to minimum column number.");
			size = Math.min(size, mean.length);
		}
		ArrayList<Pair<String, Double>> scores = new ArrayList<>();
		for (int i = 0; i < types.size(); i++) {
			double ls = 0.0;
			for (int j = 0; j < size; j++) {
				ls += (typeMeans.get(i)[j]-mean[j])*(typeMeans.get(i)[j]-mean[j]);
			}
			scores.add(new Pair<String, Double>(types.get(i), ls));
		}
		Collections.sort(scores, new PairComparator<String, Double>());
		this.scores = scores;
	}
	*/
	
	public void calculateSpots() {
		spots = new double[config.cols][config.rows];
		short[] tmp = new short[3];
		
		int brightestPixels = Math.max((int)Math.round(config.size*config.size*config.fractionBrightestPixels),
				config.minBrightestPixels);
		
		PriorityQueue<Short> q = new PriorityQueue<>(brightestPixels, new MaxComparator());
		
		for (int i = 0; i < config.rows; i++) {
			for (int j = 0; j < config.cols; j++) {
				int sum = 0;
				int x_start = (int)Math.round(config.x_lower - config.size/2 + j*config.x_dist),
					x_end = (int)Math.round(config.x_lower + config.size/2 + j*config.x_dist),
					y_start = (int)Math.round(config.y_upper - config.size/2 + i*config.y_dist),
					y_end = (int)Math.round(config.y_upper + config.size/2 + i*config.y_dist);
				for (int x = x_start; x <= x_end; x++) {
					for (int y = y_start; y <= y_end; y++) {
						config.getMat().get(y, x, tmp);
						//sum += tmp[0];
						if (q.size() > brightestPixels) {
							q.poll();
						}
						q.add(tmp[0]);
					}
				}
				q.poll();
				// int num = (x_end - x_start + 1)*(y_end - y_start + 1);
				for (Short s : q) {
					sum += s;
				}
				spots[j][i] = ((double) sum / (double) brightestPixels) / 65535.0;
				q.clear();
			}
		}
		// System.out.println(spots[0][1]);
		// System.out.println(spots[1][0]);
	}
	
	public void autoMask() {
		config.masked = new boolean[config.cols][config.rows];
		
		for (int i = 0; i < config.cols; i++) {
			boolean[] indexer = Util.findClosest(spots[i], config.maskThreshold);

			for (int j = 0; j < config.rows; j++) {
				config.masked[i][j] = !indexer[j];
			}
		}
	}
	
	public void calculateStatistics() {
		mean = new double[config.cols];
		sd = new double[config.cols];
		
		for (int i = 0; i < config.cols; i++) {
			int num = 0;
			
			for (int j = 0; j < config.rows; j++) {
				if (!config.masked[i][j]) {
					num++;
				}
			}
			
			// selected is array of values that were not masked
			double[] selected = new double[num];
			for (int j = 0, k = 0; j < config.rows; j++) {
				if (!config.masked[i][j]) {
					selected[k] = spots[i][j];
					k++;
				}
			}
			mean[i] = Util.calculateMean(selected);
			sd[i] = Util.calculateSD(selected);
		}
	}
	
	private String toString(char sep, boolean transposed) {
		int factor = 1;
		if (spots.length == 0) {
			return "no data for " + config.getFileName();
		}
		StringBuilder sb = new StringBuilder();
        	
		if (transposed) {
			for (int i = 0; i < spots[0].length; i++) {
	        	sb.append(config.getFileName() + sep + "row_" + (i+1));
	        	for (int j = 0; j < spots.length; j++) {
	        		sb.append(sep);
	        		sb.append((int) Math.round(spots[j][i]*factor));	        		
	        	}
	        	sb.append("\n");
	        }
			
			for (int i = 0; i < spots[0].length; i++) {
	        	sb.append(config.getFileName() + sep + "msk_" + (i+1));
	        	for (int j = 0; j < spots.length; j++) {
	        		sb.append(sep);
	        		sb.append(config.masked[j][i] ? "1" : "0");
	        	}
	        	sb.append("\n");
	        }
			
			sb.append(config.getFileName() + sep + "mean");
			for (int i = 0; i < spots.length; i++) {
				sb.append(sep);
				sb.append(mean[i] * factor);
			}
			sb.append("\n");
			
			sb.append(config.getFileName() + sep + "sd");
			for (int i = 0; i < spots.length; i++) {
				sb.append(sep);
				sb.append(sd[i] * factor);
			}
			sb.append("\n");
		}
		else {
			sb.append("file" + sep + "row" + sep);
			for (int i = 0; i < spots[0].length; i++) {
	        	sb.append("spot_" + (i+1) + sep);
	        }
	        for (int i = 0; i < spots[0].length; i++) {
	        	sb.append("msk_" + (i+1) + sep);
	        }
	        sb.append("mean");
	        sb.append(sep);
	        sb.append("sd");
	        sb.append("\n");
	        
	        for (int i = 0; i < spots.length; i++) {
	        	sb.append(config.getFileName() + sep + i + sep);
		        
	        	for (int j = 0; j < spots[0].length; j++) {
	        		sb.append((int) Math.round(spots[j][i]*factor));
	        		sb.append(sep);
	        	}
	        	for (int j = 0; j < spots[0].length; j++) {
	        		sb.append(config.masked[i][j] ? "1" : "0");
	        		sb.append(sep);
	        	}
	        	sb.append(mean[i] * factor);
	        	sb.append(sep);
	        	sb.append(sd[i] * factor);
	        	sb.append("\n");
	        }
		}
		
		// sb.append(scores);
		
        return sb.toString();
	}
	
	@Override
	public String toString() {
		return toString('\t', true);
	}
	
	public void toTSV(String path)	{
		try {
			PrintWriter pw = new PrintWriter(new File(path));
	        pw.write(toString(',', true));
	        pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public double[][] getSpots() {
		return spots;
	}
	
	public double[] getMean() {
		return mean;
	}
	
	public double[] getSd() {
		return sd;
	}
}
