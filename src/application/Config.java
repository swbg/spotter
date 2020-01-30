package application;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Config implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private File file;
	private transient Mat mat;
	private short[] serialized_mat;
	private transient MainController controller;
	public final int x, y;
	public int rows, cols, size;
	public double x_upper, y_upper, x_lower, y_lower, x_dist, y_dist,
		x_highValue, x_lowValue, y_highValue, y_lowValue, maskThreshold;
	private boolean saved, fixed;
	private Analysis analysis;
	
	public double brightness, contrast;
	
	public double fractionBrightestPixels;
	public int minBrightestPixels;
	
	// first dimension corresponds to type of antibody
	public boolean[][] masked;
	public ArrayList<Double> autoValues = new ArrayList<>();

	public Config(Mat mat, MainController controller, File file, Prop prop) {
		this.mat = mat;
		this.controller = controller;
		this.file = file;
		this.x = mat.cols();
		this.y = mat.rows();
		this.analysis = new Analysis(this);
		saved = true;
		
		fractionBrightestPixels = prop.fractionBrightestPixels;
		minBrightestPixels = prop.minBrightestPixels;
		
		brightness = prop.defaultBrightness;
		contrast = prop.defaultContrast;
		
		update();
	}
	
	public void serialize_mat() {
		serialized_mat = new short[mat.rows()*mat.cols()*(int)mat.elemSize()];
		mat.get(0, 0, serialized_mat);
	}
	
	public void deserialize_mat() {
		if (serialized_mat != null) {
			mat = new Mat(y, x, CvType.CV_16UC3);
			mat.put(0, 0, serialized_mat);
		}
	}
	
	public void update() {
		if (fixed) {
			return;
		}
		if (rows != controller.rows.getValue() || cols != controller.cols.getValue()) {
			masked = new boolean[controller.cols.getValue()][controller.rows.getValue()];
		}
		rows = controller.rows.getValue();
		cols = controller.cols.getValue();
		size = controller.size.getValue();
		y_upper = (1-controller.y_range.getHighValue())*y;
		y_lower = (1-controller.y_range.getLowValue())*y;
		x_upper = controller.x_range.getHighValue()*x;
		x_lower = controller.x_range.getLowValue()*x;
		y_dist = (y_lower-y_upper)/(rows-1);
		x_dist = (x_upper-x_lower)/(cols-1);
		
		x_highValue = controller.x_range.getHighValue();
		x_lowValue = controller.x_range.getLowValue();
		y_highValue = controller.y_range.getHighValue();
		y_lowValue = controller.y_range.getLowValue();
		
		maskThreshold = Double.parseDouble(controller.mask.getText());
	}
	
	public void updateControls() {
		fix();
		controller.rows.getEditor().setText("" + rows);
		controller.cols.getEditor().setText("" + cols);
		controller.size.getEditor().setText("" + size);
		
		controller.x_range.setHighValue(x_highValue);
		controller.x_range.setLowValue(x_lowValue);
		controller.y_range.setHighValue(y_highValue);
		controller.y_range.setLowValue(y_lowValue);
		
		controller.x_range.setHighValue(x_highValue);
		controller.x_range.setLowValue(x_lowValue);
		controller.y_range.setHighValue(y_highValue);
		controller.y_range.setLowValue(y_lowValue);
		unfix();
	}
	
	public boolean isSaved() {
		return saved;
	}
	
//	public void save() {
//		saved = true;
//		controller.setNotification("Saved current configuration.");
//	}
	
//	public void unsave() {
//		saved = false;
//		controller.setNotification("Removed previously saved configuration.");
//	}
	
	public void fix() {
		fixed = true;
	}
	
	public void unfix() {
		fixed = false;;
	}
	
	public void analyzeConfig() {
		analyzeConfig(false);
	}
	
	public void analyzeConfig(boolean autoMask) {
		analysis.analyzeConfig(autoMask);
		System.out.println(analysis);
	}
	
	public void toggleMask(int x, int y) {
		if (x < cols && y < rows) {
			masked[x][y] = !masked[x][y];
		}
	}
	
	public void writeAnalysis() {
		analysis.analyzeConfig();
		analysis.toTSV(file.getAbsolutePath() + "_result.csv");
		controller.setNotification("Wrote analysis to " + file.getAbsolutePath() + "_result.csv.");
	}
	
	public void resetMask() {
		masked = new boolean[mat.cols()][mat.rows()];
	}
	
//	public void deleteAnalysis() {
//		if (analysis != null) {
//			controller.setNotification("Removed previously saved analysis.");
//		}
//		analysis = null;
//	}
	
	public Analysis getAnalysis() {
		return analysis;
	}
	
	@Override
	public String toString() {
		if (file != null) {
			return file.getName();
		} else {
			return "";
		}
	}
	
	public int[] getClickedSpot(double x, double y) {
		if (this.analysis.analyzed) {
			// correction in case image is cropped
			y += (int) Math.round(this.y_upper-50);
		}
		
		x = x - x_lower + x_dist/2;
		y = y - y_upper + y_dist/2;
		
		int x_index = (int)Math.floor(x / x_dist);
		int y_index = (int)Math.floor(y / y_dist);
		
		if (x_index >= cols || x_index < 0 || y_index >= rows || y_index < 0) {
			return null;
		}
		
		return new int[] {x_index, y_index};
	}
	
	public void autoDetect() {
		Mat tmp = mat.clone();
		Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
		Imgproc.GaussianBlur(tmp, tmp, new Size(11, 11), 0, 0);
		Imgproc.threshold(tmp, tmp, 150, 255, Imgproc.THRESH_BINARY);
		
		// remove noise
		Imgproc.erode(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1)));
		Imgproc.dilate(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6)));
		
		tmp.convertTo(tmp, CvType.CV_8UC1);
		
//		mat = tmp;
//		if (true)
//			return;
		
		double size_est = setAutoValues(true, tmp);
		size_est += setAutoValues(false, tmp);
		size = (int) Math.round(size_est/1.9);
		
		updateControls();
		update();
		
		analysis.calculateSpots();
		analysis.autoMask();
		
		// save();
	}
	
	public ArrayList<Double> batchAuto() {
		return batchAuto(0);
	}
	
	private ArrayList<Double> batchAuto(int retry) {
		System.out.println("batchAuto(" + retry + ");");
		System.out.println("Attempting auto detection for " + file.getName() +
				" with " + rows + " rows and " + cols + " columns.");
		
		Mat tmp = mat.clone();
		Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
		
		// fix for 255 65535
		tmp.convertTo(tmp, tmp.type(), 255.0/65535.0, 0);
		
		Imgproc.GaussianBlur(tmp, tmp, new Size(11, 11), 0, 0);
		// batchAuto is more stringent than semiAuto
		tmp.convertTo(tmp, tmp.type(), 1.0, -100/(retry+1));
		
		// remove noise
		Imgproc.erode(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1)));
		Imgproc.dilate(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6)));
		
		tmp.convertTo(tmp, CvType.CV_8UC1);
		
		// determine horizontal pattern
		Mat vec = new Mat();
		Core.reduce(tmp, vec, 1, Core.REDUCE_SUM, CvType.CV_32S);
		Core.transpose(vec, vec);
		
		ArrayList<Integer[]> transitions = findIslands(vec, 511);
		int max = (int) Math.round(Core.minMaxLoc(vec).maxVal);
		
		// if there are too many islands, check for longitudinal structures and mask these
		ArrayList<Integer> cnts = new ArrayList<>();
		if (transitions.size() > rows) {
			for (Integer[] i : transitions) {
				Mat tmp_vec = new Mat();
				Core.reduce(tmp.rowRange(i[0], i[1]), tmp_vec, 0, Core.REDUCE_MAX);
				int cnt = 0;
				for (int j = 0; j < tmp_vec.cols(); j++) {
					if (tmp_vec.get(0, j)[0] > 0) {
						cnt++;
					}
				}
				cnts.add(cnt);
			}
			for (int i = 0; i < cnts.size(); i++) {
				if (cnts.get(i) > 3.0*Util.calculateMedian(cnts)) {
					for (int j = transitions.get(i)[0]; j < transitions.get(i)[1]; j++) {
						vec.put(0, j, 0);
					}
				}
			}
		}
		
		for (int ws = 512; transitions.size() != rows; ws++) {
			if (ws >= max) {
				System.out.println("Error during auto detection.");
				if (retry < 2) {
					System.out.println("Retrying with higher sensitivity.");
					semiAuto(retry+1);
				}
				return new ArrayList<>();
			}
			transitions = findIslands(vec, ws);
		}
		
		y_highValue = 1 - (transitions.get(0)[0]+transitions.get(0)[1])/(2.0*vec.cols());
		y_lowValue = 1 - (transitions.get(transitions.size()-1)[0]+transitions.get(transitions.size()-1)[1])/(2.0*vec.cols());
		
		// determine average size of spots
		double d = 0.0;
		for (Integer[] i : transitions) {
			d += i[1] - i[0];
		}
		size = (int) Math.round(1.2*d/rows);
		
		// determine vertical pattern
		tmp = mat.clone();
		Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
		
		// fix for 255 65535
		tmp.convertTo(tmp, tmp.type(), 255.0/65535.0, 0);
		
		Imgproc.GaussianBlur(tmp, tmp, new Size(11, 11), 0, 0);
		
		// make anything outside the main spot pattern black
		for (int i = 0; i < transitions.size()-1; i++) {
			tmp.rowRange(transitions.get(i)[1], transitions.get(i+1)[0]).setTo(new Scalar(0));
		}
		tmp = tmp.rowRange(transitions.get(0)[0], transitions.get(transitions.size()-1)[1]);
		
		// remove noise
		tmp.convertTo(tmp, tmp.type(), 1.0, -40/(retry+1));
		Imgproc.erode(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1)));
		Imgproc.dilate(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6)));
		
		tmp.convertTo(tmp, CvType.CV_8UC1);
		vec = new Mat();
		Core.reduce(tmp, vec, 0, Core.REDUCE_SUM, CvType.CV_32S);
		
		int ws = (int) Math.round((1/controller.prop.autoSensitivity)*1.5*Util.calculateMedian(vec));
		// correction for images with low signal
		if (ws < 20) {
			ws += 5;
		}
		
		// correction to enforce strong signal for first column
		for (int i = 1; i < vec.cols(); i++) {
			if (vec.get(0, i)[0] > 512) {
				vec.colRange(0, i).setTo(new Scalar(0));
				break;
			}
		}
		
		// first identify real islands then split islands by subtracting the mean and finding new islands
		transitions = findIslands(vec, ws);

		ArrayList<Integer[]> del = new ArrayList<>();
		ArrayList<Integer[]> split = new ArrayList<>();
		for (Integer[] i : transitions) {
			if (i[1]-i[0] < 0.2*size) {
				del.add(i);
			} else if (i[1]-i[0] > 2*size) {
				split.add(i);
			}
		}
		// delete small islands
		for (Integer[] i : del) {
			transitions.remove(i);
		}
		// split large islands
		for (Integer[] i : split) {
			int island_ws = (int) Math.round(0.5*Util.calculateMedian(vec.colRange(i[0], i[1])));
			boolean repeat;
			do {
				repeat = false;
				vec.colRange(i[0], i[1]).convertTo(vec.colRange(i[0], i[1]), vec.type(), 1.0, -island_ws);
				ArrayList<Integer[]> new_transitions = findIslands(vec.colRange(i[0], i[1]), ws);
				for (Integer[] j : new_transitions) {
					if (j[1]-j[0] > 2*size) {
						repeat = true;
						break;
					}
				}
			} while (repeat);
		}
		if (split.size() > 0) {
			transitions = findIslands(vec, ws);
		}
		
		System.out.println(vec.dump());
		System.out.println(ws);
		for (Integer[] d2 : transitions) {
			System.out.println(Arrays.toString(d2));
		}
		
		x_lowValue = (transitions.get(0)[0]+transitions.get(0)[1])/(2.0*mat.cols());
		System.out.println(x_lowValue*mat.cols());
		
		// calculate centers of transitions to get estimate for column position
		ArrayList<Double> centers = new ArrayList<>();
		ArrayList<Double> result = new ArrayList<>();
		for (Integer[] i : transitions) {
			centers.add((i[0]+i[1])/2.0);
		}
		Double lower = centers.get(0);
		for (Double c : centers) {
			result.add(c - lower);
		}
		return result;
	}
	
	public void semiAuto(int retry) {
		System.out.println("Attempting auto detection for " + file.getName() + " with " + rows + " rows and " + cols + " columns.");
		
		Mat tmp = mat.clone();
		Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
		
		tmp.convertTo(tmp, tmp.type(), 255.0/65535.0, 0);
		
		Imgproc.GaussianBlur(tmp, tmp, new Size(11, 11), 0, 0);
		// Imgproc.threshold(tmp, tmp, 80, 255, Imgproc.THRESH_BINARY);
		tmp.convertTo(tmp, tmp.type(), 1.0, -80/(retry+1));
		
		// remove noise
		Imgproc.erode(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1)));
		Imgproc.dilate(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6)));
		
		tmp.convertTo(tmp, CvType.CV_8UC1);
		
		// determine horizontal pattern
		// alternative option: take positive control for granted, search for first column
		Mat vec = new Mat();
		Core.reduce(tmp, vec, 1, Core.REDUCE_SUM, CvType.CV_32S);
		Core.transpose(vec, vec);
		
		ArrayList<Integer[]> transitions = findIslands(vec, 511);
		int max = (int) Math.round(Core.minMaxLoc(vec).maxVal);
		
		// if there are too many islands, check for longitudinal structures and mask these
		ArrayList<Integer> cnts = new ArrayList<>();
		if (transitions.size() > rows) {
			for (Integer[] i : transitions) {
				Mat tmp_vec = new Mat();
				Core.reduce(tmp.rowRange(i[0], i[1]), tmp_vec, 0, Core.REDUCE_MAX);
				int cnt = 0;
				for (int j = 0; j < tmp_vec.cols(); j++) {
					if (tmp_vec.get(0, j)[0] > 0) {
						cnt++;
					}
				}
				cnts.add(cnt);
			}
			for (int i = 0; i < cnts.size(); i++) {
				if (cnts.get(i) > 3.0*Util.calculateMedian(cnts)) {
					for (int j = transitions.get(i)[0]; j < transitions.get(i)[1]; j++) {
						vec.put(0, j, 0);
					}
				}
			}
		}
		
		for (int ws = 512; transitions.size() != rows; ws++) {
			if (ws >= max) {
				System.out.println("Error during auto detection.");
				if (retry < 3) {
					System.out.println("Retrying with higher sensitivity.");
					semiAuto(retry+1);
				}
				return;
			}
			transitions = findIslands(vec, ws);
		}
		
//		for (Integer[] d : transitions) {
//			System.out.println(Arrays.toString(d));
//		}
		
		y_highValue = 1 - (transitions.get(0)[0]+transitions.get(0)[1])/(2.0*vec.cols());
		y_lowValue = 1 - (transitions.get(transitions.size()-1)[0]+transitions.get(transitions.size()-1)[1])/(2.0*vec.cols());
		
		double d = 0.0;
		for (Integer[] i : transitions) {
			d += i[1] - i[0];
		}
		
		size = (int) Math.round(d/rows);
		System.out.println("size: " + size);
		
		// determine vertical pattern
		tmp = mat.clone();
		Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
		
		// fix for 255 65535
		tmp.convertTo(tmp, tmp.type(), 255.0/65535.0, 0);
		
		Imgproc.GaussianBlur(tmp, tmp, new Size(11, 11), 0, 0);
		// tmp.convertTo(tmp, tmp.type(), 1.0, -40/(retry+1));
		for (int i = 0; i < transitions.size()-1; i++) {
			tmp.rowRange(transitions.get(i)[1], transitions.get(i+1)[0]).setTo(new Scalar(0));
			// just for debugging
			// mat.rowRange(transitions.get(i)[1], transitions.get(i+1)[0]).setTo(new Scalar(0));
		}
		// just for debugging
//		Imgproc.cvtColor(tmp, mat, Imgproc.COLOR_GRAY2RGB);
//		mat.rowRange(0, transitions.get(0)[0]).setTo(new Scalar(0));
//		mat.rowRange(transitions.get(transitions.size()-1)[1], mat.rows()).setTo(new Scalar(0));
		
		tmp = tmp.rowRange(transitions.get(0)[0], transitions.get(transitions.size()-1)[1]);
		
		// remove noise
		Imgproc.erode(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1)));
		Imgproc.dilate(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6)));
		
		tmp.convertTo(tmp, CvType.CV_8UC1);
		vec = new Mat();
		Core.reduce(tmp, vec, 0, Core.REDUCE_SUM, CvType.CV_32S);
		
		int ws = (int) Math.round((1/controller.prop.autoSensitivity)*1.5*Util.calculateMedian(vec));
		
		// first identify real islands then split islands by subtracting the mean and finding new islands
		// make size constraints		
		System.out.println(vec.dump());
		transitions = findIslands(vec, ws);		

		System.out.println("printing raw transitions");
		for (Integer[] d2 : transitions) {
			System.out.println(Arrays.toString(d2));
		}
		System.out.println("##");

		ArrayList<Integer[]> del = new ArrayList<>();
		ArrayList<Integer[]> split = new ArrayList<>();
		for (Integer[] i : transitions) {
			if (i[1]-i[0] < 0.2*size) {
				del.add(i);
			} else if (i[1]-i[0] > 2*size) {
				split.add(i);
			}
		}
		for (Integer[] i : del) {
			transitions.remove(i);
		}
		System.out.println("selected for split");
		for (Integer[] i : split) {
			System.out.println(Arrays.toString(i));
//			// calculate mean of island
//			double m = 0.0;
//			for (int j = i[0]; j <= i[1]; j++) {
//				m += vec.get(0, j)[0];
//			}
//			m /= (i[1]-i[0]+1);
			
//			// subtract from vec
//			vec.colRange(i[0], i[1]).convertTo(vec.colRange(i[0], i[1]), vec.type(), 1.0, -0.5*m);
			
			int island_ws = (int) Math.round(0.5*Util.calculateMedian(vec.colRange(i[0], i[1])));
			boolean repeat;
			do {
				repeat = false;
				System.out.println("Splitting island.");
				vec.colRange(i[0], i[1]).convertTo(vec.colRange(i[0], i[1]), vec.type(), 1.0, -island_ws);
				ArrayList<Integer[]> new_transitions = findIslands(vec.colRange(i[0], i[1]), ws);
				for (Integer[] j : new_transitions) {
					if (j[1]-j[0] > 2*size) {
						repeat = true;
						break;
					}
				}
			} while (repeat);
		}
		if (split.size() > 0) {
			transitions = findIslands(vec, ws);
		}
		
		System.out.println(vec.dump());
//		Imgproc.cvtColor(tmp, mat, Imgproc.COLOR_GRAY2RGB);
		System.out.println("printing modified transitions");
		for (Integer[] d2 : transitions) {
			System.out.println(Arrays.toString(d2));
//			Imgproc.line(mat, new Point(d2[0], 0), new Point(d2[0], mat.rows()),
//					new Scalar(0, 0, 255), 1);
//			Imgproc.line(mat, new Point(d2[1], 0), new Point(d2[1], mat.rows()),
//					new Scalar(0, 0, 255), 1);
		}
		
		x_lowValue = (transitions.get(0)[0]+transitions.get(0)[1])/(2.0*mat.cols());
		
		// calculate centers of transitions to get estimate for column position
		ArrayList<Double> centers = new ArrayList<>();
		for (Integer[] i : transitions) {
			centers.add((i[0]+i[1])/2.0);
			// auto indicators
			// Imgproc.line(mat, new Point(1+(i[0]+i[1])/2.0, 0), new Point(1+(i[0]+i[1])/2.0, 10), new Scalar(0, 255, 255), 1);
		}
		autoValues = centers;
		if (transitions.size() < 2) {
			System.out.println("Could not determine horizontal spacing.");
			x_highValue = cols*(y_highValue-y_lowValue)/rows;
		} else if (transitions.size() == 2) {
			System.out.println("Could not determine horizontal spacing.");
			double width = (centers.get(1)-centers.get(0));
			double y_dist = (y_highValue-y_lowValue)*mat.rows()/(rows-1);
			System.out.println(y_dist);
			int index = (int) Math.round(width/y_dist);
			index = Math.min(cols-1, index-1);
			System.out.println(index);
			x_highValue = (centers.get(0) + width/index*(cols-1))/mat.cols();
		} else {
			double first = centers.get(0);
			ArrayList<Double> newCenters = new ArrayList<>();
			for (Double c : centers) {
				newCenters.add(c - first);
			}
			centers = newCenters;
//			for (Double d2 : centers) {
//				System.out.println("c:" + d2);
//			}
			double min_loss = Double.MAX_VALUE;
			int min_index = 0;
			for (int i = centers.size()-1; i < cols; i++) {
				double dist = centers.get(centers.size()-1)/i;
				double loss = 0.0;
				for (Double c : centers) {
					int col = (int) Math.round(c/dist);
					loss += (c-col*dist)*(c-col*dist);
				}
				if (loss < min_loss) {
					min_loss = loss;
					min_index = i;
				}
			}
			double dist = centers.get(centers.size()-1)/(min_index);
			x_highValue = (first + dist*(cols-1))/mat.cols();
		}

		updateControls();
		update();
		
		analysis.calculateSpots();
		analysis.autoMask();
		
		// save();
	}
	
	private ArrayList<Integer[]> findIslands(Mat vec, int ws) {
		ArrayList<Integer[]> result = new ArrayList<>();
		boolean white = false;
		Integer[] d = new Integer[2];
		// identify transitions from black to white and vice versa
		for (int i = 0; i < vec.cols(); i++) {
			// current pixel is above threshold and previous one was not
			if (vec.get(0, i)[0] > ws && !white) {
				white = true;
				d[0] = i;
			}
			// current pixel is below threshold and previous one was not
			else if (vec.get(0, i)[0] <= ws && white) {
				white = false;
				d[1] = i;
				result.add(d);
				d = new Integer[2];
			}
		}
		return result;
	}
	
	private double setAutoValues(boolean horizontal, Mat tmp) {
		Mat vec = new Mat();
		
		if (horizontal) {
			Core.reduce(tmp, vec, 0, Core.REDUCE_MAX);
		}
		else {
			Core.reduce(tmp, vec, 1, Core.REDUCE_MAX);
			Core.transpose(vec, vec);
		}
		
		ArrayList<Integer> transitions = new ArrayList<>();
		ArrayList<Double> centers = new ArrayList<>();
		
		double width = 0;
		
		// identify first, last and number of blobs in one dimension
		// identify median of distance between start of blobs
		
		boolean white = false;
		
		// identify transitions from black to white and vice versa
		for (int i = 0; i < vec.cols(); i++) {
			// current pixel is white and previous one was black
			if (vec.get(0, i)[0] > 0 && !white) {
				white = true;
				transitions.add(i);
			}
			// current pixel is black and previous one was white
			else if (vec.get(0, i)[0] == 0 && white) {
				white = false;
				transitions.add(i);
			}
		}
		// just to prevent exceptions
		if (transitions.size() % 2 != 0) {
			transitions.add(vec.cols()-1);
			System.out.println("Auto detection failed.");
		}
		
		// remove transitions that are less than three pixels apart
		for (int i = 0; i < transitions.size()-1; i++) {
			if (transitions.get(i+1) - transitions.get(i) <= 3) {
				transitions.remove(i);
				transitions.remove(i);
			}
		}
		
		// calculate centers of blobs and average size of blobs
		for (int i = 0; i < transitions.size()-1; i+=2) {
			centers.add((transitions.get(i+1)+transitions.get(i))/2.0);
			width += transitions.get(i+1) - transitions.get(i);
		}
		
		// calculate median distances
		ArrayList<Double> dist = new ArrayList<>();
		for (int i = 0; i < centers.size()-1; i++) {
			dist.add(centers.get(i+1) - centers.get(i));
		}
		// just to prevent exceptions
		if (dist.size() == 0) {
			dist.add(5.0);
			System.out.println("Auto detection failed.");
		}
		Collections.sort(dist);
		double med_dist = dist.get(dist.size()/2);
		
		if (centers.size() < 2) {
			System.out.println("Error auto");
			centers.add(5.0);
			centers.add(10.0);
		}
		
		if (horizontal) {
			cols = (int) Math.round((centers.get(centers.size()-1) - centers.get(0))/med_dist) + 1;
			
			x_lowValue = centers.get(0)/vec.cols();
			x_highValue = centers.get(centers.size()-1)/vec.cols();
			
			return width /= cols;
		}
		else {
			rows = (int) Math.round((centers.get(centers.size()-1) - centers.get(0))/med_dist) + 1;
			
			y_highValue = 1 - centers.get(0)/vec.cols();
			y_lowValue = 1 - centers.get(centers.size()-1)/vec.cols();
			
			return width /= rows;
		}
	}
	
	public String getFileName() {
		return file.getName();
	}
	
	public MainController getController() {
		return controller;
	}
	
	public void setController(MainController controller) {
		this.controller = controller;
	}
	
	public Mat getMat() {
		return mat;
	}
	
//	public void resetMat() {
//		mat = Util.fromFile(file);
//	}
	
	public boolean isMasked() {
		if (masked == null || masked.length < 1) {
			return false;
		}
		for (int i = 0; i < masked.length; i++) {
			for (int j = 0; j < masked[i].length; j++) {
				if (masked[i][j]) {
					return true;
				}
			}
		}
		return false;
	}
	
}
