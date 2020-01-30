package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Comparator;
import java.util.PriorityQueue;

public class Analysis implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Config config;
	private double[][] spots;
	private double[] mean;
	private double[] sd;
	public boolean analyzed;
	
	private class MaxComparator implements Comparator<Short> {

		@Override
		public int compare(Short o1, Short o2) {
			return o1 > o2 ? 1 : -1;
		}	
	}
	
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
		analyzed = true;
	}
	
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
						if (q.size() > brightestPixels) {
							q.poll();
						}
						q.add(tmp[0]);
					}
				}
				q.poll();
				for (Short s : q) {
					sum += s;
				}
				spots[j][i] = ((double) sum / (double) brightestPixels) / 65535.0;
				q.clear();
			}
		}
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
