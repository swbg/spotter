package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Analysis {
	
	private double[][] spots;
	private boolean[][] masked;
	private double[] mean;
	private double[] sd;
	
	public Analysis(Config config) {
		// first dimension corresponds to type of antibody
		spots = new double[config.cols][config.rows];
		masked = new boolean[config.cols][config.rows];
		mean = new double[config.cols];
		sd = new double[config.cols];
		
		short[] tmp = new short[3];
		
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
						sum += tmp[0];
					}
				}
				int num = (x_end - x_start + 1)*(y_end - y_start + 1);
				spots[j][i] = sum/num;
			}
		}
		
		for (int i = 0; i < config.cols; i++) {
			boolean[] indexer = Util.findClosest(spots[i], config.maskThreshold);
			int num = 0;
			
			for (int j = 0; j < config.rows; j++) {
				masked[i][j] = !indexer[j];
				if (indexer[j]) {
					num++;
				}
			}
			
			double[] selected = new double[num];
			for (int j = 0, k = 0; j < config.rows; j++) {
				if (indexer[j]) {
					selected[k] = spots[i][j];
					k++;
				}
			}
			mean[i] = Util.calculateMean(selected);
			sd[i] = Util.calculateSD(selected);
		}
		
//		for (int i = 0; i < config.cols; i++) {
//			double zscores[] = Util.calculateZScores(spots[i]);
//			System.out.println(Arrays.toString(zscores));
//			System.out.println(Util.calculateMean(spots[i]));
//			System.out.println(Arrays.toString(spots[i]));
//			int retain = 0;
//			for (int j = 0; j < zscores.length; j++) {
//				if (Math.abs(zscores[j]) < CUTOFF) {
//					retain++;
//				}
//				else {
//					masked[i][j] = true;
//				}
//			}
//			double[] retained = new double[retain];
//			for (int j = 0, k = 0; j < retain; j++) {
//				if (Math.abs(zscores[j]) < CUTOFF) {
//					retained[k] = spots[i][j];
//					k++;
//				}
//			}
//			mean[i] = Util.calculateMean(retained);
//			sd[i] = Util.calculateSD(retained);
//		}
	}
	
	private String toString(char sep) {
		if(spots.length == 0) {
			return "no data";
		}
		
		StringBuilder sb = new StringBuilder();
        
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
        	for (int j = 0; j < spots[0].length; j++) {
        		sb.append(spots[i][j]);
        		sb.append(sep);
        	}
        	for (int j = 0; j < spots[0].length; j++) {
        		sb.append(masked[i][j] ? "1" : "0");
        		sb.append(sep);
        	}
        	sb.append(mean[i]);
        	sb.append(sep);
        	sb.append(sd[i]);
        	sb.append("\n");
        }
        return sb.toString();
	}
	
	@Override
	public String toString() {
		return toString('\t');
	}
	
	public void toTSV(String path)	{
		try {
			PrintWriter pw = new PrintWriter(new File(path));
	        pw.write(toString(','));
	        pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public boolean[][] getMasked() {
		return masked;
	}
}
