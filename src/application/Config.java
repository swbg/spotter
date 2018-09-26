package application;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Config {
	
	private Mat mat;
	private FXController controller;
	private File file;
	public int x, y, rows, cols, size;
	public double x_upper, y_upper, x_lower, y_lower, x_dist, y_dist,
		x_highValue, x_lowValue, y_highValue, y_lowValue, maskThreshold;
	private boolean saved, fixed;
	private Analysis analysis;
	
	public double fractionBrightestPixels;
	public int minBrightestPixels;
	
	// first dimension corresponds to type of antibody
	public boolean[][] masked;

	public Config(Mat mat, FXController controller, File file, Prop prop) {
		this.mat = mat;
		this.controller = controller;
		this.file = file;
		this.x = mat.cols();
		this.y = mat.rows();
		this.analysis = new Analysis(this);
		this.masked = new boolean[mat.cols()][mat.rows()];
		saved = false;
		
		fractionBrightestPixels = prop.fractionBrightestPixels;
		minBrightestPixels = prop.minBrightestPixels;
		
		update();
	}
	
	public Mat getMat() {
		return mat;
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
	
	private void updateControls() {
		fix();
		controller.rows.getEditor().setText("" + rows);
		controller.cols.getEditor().setText("" + cols);
		controller.size.getEditor().setText("" + size);
		
		controller.x_range.setHighValue(x_highValue);
		controller.x_range.setLowValue(x_lowValue);
		controller.y_range.setHighValue(y_highValue);
		controller.y_range.setLowValue(y_lowValue);
		unfix();
	}
	
	public boolean isSaved() {
		return saved;
	}
	
	public void save() {
		saved = true;
		controller.setNotification("Saved current configuration.");
	}
	
	public void unsave() {
		saved = false;
		controller.setNotification("Removed previously saved configuration.");
	}
	
	public void fix() {
		fixed = true;
	}
	
	public void unfix() {
		fixed = false;;
	}
	
	public void analyze() {
		analysis.performAnalysis(false);
	}
	
	public void toggleMask(int x, int y) {
		if (x < cols && y < rows) {
			masked[x][y] = !masked[x][y];
		}
	}
	
	public void printAnalysis() {
		analysis.performAnalysis(false);
		System.out.println(analysis);
	}
	
	public void writeAnalysis() {
		analysis.performAnalysis(false);
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
	
	public void setController(FXController controller) {
		this.controller = controller;
	}
	
	public FXController getController() {
		return controller;
	}
	
	@Override
	public String toString() {
		return file.getName();
	}
	
	public int[] getClickedSpot(double x, double y) {
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
		Imgproc.erode(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
		Imgproc.dilate(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6)));
		
		tmp.convertTo(tmp, CvType.CV_8UC1);
		
		double size_est = setAutoValues(true, tmp);
		size_est += setAutoValues(false, tmp);
		size = (int) Math.round(size_est/1.9);
		
		updateControls();
		update();
		
		analysis.calculateSpots();
		analysis.autoMask();
		
		System.out.println("-- " + cols + " " + rows);
		System.out.println(".. " + masked.length + " " + masked[0].length);
		
		save();
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
	
}
