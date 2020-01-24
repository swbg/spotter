package application;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
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
	
	// https://stackoverflow.com/a/34293310
	public static Mat bufferedImageToMat(BufferedImage bi) {
		  Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3); //CV_16UC3);
		  // short[] data = ((DataBufferUShort) bi.getRaster().getDataBuffer()).getData();
		  byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		  mat.put(0, 0, data);
		  // Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2RGB);
		  mat.convertTo(mat, CvType.CV_16UC3);
		  return mat;
	}
	
	public static Mat fromFile(File file) {
		Mat mat = null;
		BufferedReader reader = null;
		
		// adapted from https://stackoverflow.com/a/3806154
		try {
			reader = new BufferedReader(new FileReader(file));

			// get image dimensions
		    int cols = Integer.parseInt(reader.readLine().trim());
		    int rows = Integer.parseInt(reader.readLine().trim());
		    mat = new Mat(rows, cols, CvType.CV_16UC1);
		    
		    // skip empty line
		    reader.readLine();
		    
		    for (int i = rows-1; i >= 0; i--) {
		    	for (int j = cols-1; j >= 0; j--) {
		    		short val = (short)(Integer.parseInt(reader.readLine().trim()) / 2);
		    		mat.put(i, j, new short[]{val});
		    	}
		    }
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
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
		
		if (mat == null) {
			return null;
		}
		
		mat.put(0, 0, new short[]{0});
		mat.put(0, 1, new short[]{(short) (65535 / 2)});
		// Core.normalize(mat, mat, 0, 255, Core.NORM_MINMAX);
		Core.normalize(mat, mat, 0, 65535, Core.NORM_MINMAX);
		mat.put(0, 0, new short[]{0});
		mat.put(0, 1, new short[]{0});
		Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2RGB);
		return mat;
	}
	
	public static Mat fromPNG(File file) {
		Mat mat = Imgcodecs.imread(file.getPath(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
		/*mat.put(0, 0, new short[]{0});
		mat.put(0, 1, new short[]{(short) 65535});
		Core.normalize(mat, mat, 0, 255, Core.NORM_MINMAX);
		mat.put(0, 0, new short[]{0});
		mat.put(0, 1, new short[]{0});*/
		Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2RGB);
		return mat;
	}
	
	public static Mat fromFile(String path) {
		return fromFile(new File(path));
	}
	
	public static double calculateMean(double[] values) {
		if (values == null || values.length == 0) {
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
		if (values == null || values.length < 2) {
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
	
	public static double calculateMedian(List<Integer> l) {
		if (l == null || l.size() == 0) {
			return 0.0;
		}
		if (l.size() == 1) {
			return l.get(0);
		}
		List<Integer> tmp = new ArrayList<>(l);
		Collections.sort(tmp);
		if (tmp.size()%2 != 0) {
			return (tmp.get(tmp.size()/2) + tmp.get(tmp.size()/2 + 1))/2.0;
		} else {
			return tmp.get(tmp.size()/2);
		}
	}
	
	public static double calculateMedianDouble(List<Double> l) {
		if (l == null || l.size() == 0) {
			return 0.0;
		}
		if (l.size() == 1) {
			return l.get(0);
		}
		List<Double> tmp = new ArrayList<>(l);
		Collections.sort(tmp);
		if (tmp.size()%2 != 0) {
			return (tmp.get(tmp.size()/2) + tmp.get(tmp.size()/2 + 1))/2.0;
		} else {
			return tmp.get(tmp.size()/2);
		}
	}
	
	// caluclate median of first row of Mat
	public static double calculateMedian(Mat m) {
		ArrayList<Integer> l = new ArrayList<>();
		for (int i = 0; i < m.cols(); i++) {
			l.add((int) m.get(0, i)[0]);
		}
		return calculateMedian(l);
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
		// double sd = calculateSD(selected);
		
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
	
	public static int findClosestIndex(ArrayList<Double> l, double d) {
		// assume l is not empty
		int result = 0;
		double diff = 800;
		for (int i = 0; i < l.size(); i++) {
			if (Math.abs(d-l.get(i)) < diff) {
				result = i;
				diff = Math.abs(d-l.get(i));
			}
		}
		return result;
	}
	
	public static String getExtension(File f) {
		String ext = "";
    	int i = f.getName().lastIndexOf('.');
    	if (i > 0) {
    		ext = f.getName().substring(i+1);
    	}
    	return ext.toLowerCase();
	}
	
	public static String getRowRepresentation(int rowNumber) {
		String result = "";
		int offset = rowNumber/26;
		if (offset > 0) {
			result += "" + (char) (offset+64);
		}
		result += (char) (rowNumber%26+65);
		return result;
	}
}
