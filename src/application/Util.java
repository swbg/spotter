package application;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.scene.image.Image;

public class Util {
	
	// https://stackoverflow.com/a/33605064
	public static Image toImage(Mat mat) {
		MatOfByte byteMat = new MatOfByte();
		Imgcodecs.imencode(".bmp", mat, byteMat);
		return new Image(new ByteArrayInputStream(byteMat.toArray()));
	}
	
	public static Mat fromFile(File file) {
		Mat mat = null;
		BufferedReader reader = null;
		
		// adapted from https://stackoverflow.com/a/3806154
		try {
			reader = new BufferedReader(new FileReader(file));

			// get image dimensions
		    int cols = Integer.parseInt(reader.readLine());
		    int rows = Integer.parseInt(reader.readLine());
		    mat = new Mat(rows, cols, CvType.CV_16UC1);
		    
		    // skip empty line
		    reader.readLine();
		    
		    for (int i = rows-1; i >= 0; i--) {
		    	for (int j = cols-1; j >= 0; j--) {
		    		short val = (short)(Integer.parseInt(reader.readLine()));
		    		mat.put(i, j, new short[]{val});
		    	}
		    }
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		} catch (Exception e) {
			return null;
		} finally {
		    try {
		        if (reader != null) {
		            reader.close();
		        }
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
		}
		
		Core.normalize(mat, mat, 0, 255, Core.NORM_MINMAX);
		Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2RGB);
		return mat;
	}
	
	public static Mat fromFile(String path) {
		return fromFile(new File(path));
	}
	
	public static double calculateMean(double[] values) {
		if (values.length == 0) {
			return 0;
		}
		double sum = 0;
		for (Double d : values) {
			sum += d;
		}
		return sum/values.length;
	}
	
	public static double calculateMean(List<Double> values) {
		double[] newValues = new double[values.size()];
		for (int i = 0; i < values.size(); i++) {
			newValues[i] = values.get(i);
		}
		return calculateMean(newValues);
	}
	
	private static double calculateVariance(double[] values) {
		if (values.length < 2) {
			return 0;
		}
		double sum = 0;
		double mean = calculateMean(values);
		for (Double d : values) {
			sum += (mean - d)*(mean - d);
		}
		return sum/(values.length-1);
	}
	
	public static double calculateSD(double[] values) {
		return Math.sqrt(calculateVariance(values));
	}
	
	public static double calculateSD(List<Double> values) {
		double[] newValues = new double[values.size()];
		for (int i = 0; i < values.size(); i++) {
			newValues[i] = values.get(i);
		}
		return Math.sqrt(calculateVariance(newValues));
	}
	
	public static double[] calculateZScores(double[] values) {
		double mean = calculateMean(values);
		double sd = calculateSD(values);
		double[] result = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = (values[i]-mean)/sd;
		}
		return result;
	}
	
	public static boolean[] findClosest(double[] values, double cutoff) {
		if (values.length < 3) {
			return new boolean[]{};
		}
		// first find smallest interval containing three values
		Double[] newValues = new Double[values.length];
		for (int i = 0; i < values.length; i++) {
			newValues[i] = values[i];
		}
		ArrayList<Double> sorted = new ArrayList<Double>(Arrays.asList(newValues));
		Collections.sort(sorted);
		
		int index = 0;
		double diff = sorted.get(2) - sorted.get(0);
		for (int i = 1; i < sorted.size()-2; i++) {
			if (sorted.get(i+2) - sorted.get(i) < diff) {
				index = i;
				diff = sorted.get(i+2) - sorted.get(i);
			}
		}
		
		// calculate standard deviation of this cluster
		ArrayList<Double> selected = new ArrayList<>();
		selected.add(sorted.get(index));
		selected.add(sorted.get(index+1));
		selected.add(sorted.get(index+2));
		
		double mean = calculateMean(selected);
		double sd = calculateSD(selected);
		
		// add remaining data points if they are within cutoff standard deviations
		double[] remaining = new double[sorted.size()-3];
		for (int i = 0, j = 0; i < sorted.size(); i++) {
			if (i < index || i > index+2) {
				remaining[j] = sorted.get(i);
				j++;
			}
		}
		for (int i = 0; i < remaining.length; i++) {
//			if (Math.abs(remaining[i] - mean) < cutoff*sd) {
//				selected.add(remaining[i]);
//			}
			if (Math.abs(remaining[i] - mean) < cutoff*mean) {
				selected.add(remaining[i]);
			}
		}
		
		// determine original indices
		boolean[] result = new boolean[values.length];
		for (int i = 0; i < values.length; i++) {
			if (selected.contains(values[i])) {
				result[i] = true;
			}
		}
		
		return result;
	}

}
