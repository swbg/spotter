package application;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
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

	public Config(Mat mat, FXController controller, File file) {
		this.mat = mat;
		this.controller = controller;
		this.file = file;
		this.x = mat.cols();
		this.y = mat.rows();
		saved = false;
		update();
	}
	
	public Mat getMat() {
		return mat;
	}
	
	public void update() {
		if (fixed) {
			return;
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
		analysis = new Analysis(this);
	}
	
	public void printAnalysis() {
		analysis = new Analysis(this);
		System.out.println(analysis);
	}
	
	public void writeAnalysis() {
		analysis = new Analysis(this);
		analysis.toTSV(file.getAbsolutePath() + "_result.csv");
		controller.setNotification("Wrote analysis to " + file.getAbsolutePath() + "_result.csv.");
	}
	
	public void deleteAnalysis() {
		if (analysis != null) {
			controller.setNotification("Removed previously saved analysis.");
		}
		analysis = null;
	}
	
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
	
	public void autoDetect() {
		Mat tmp = mat.clone();
		Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
		Imgproc.GaussianBlur(tmp, tmp, new Size(11, 11), 0, 0);
		Imgproc.threshold(tmp, tmp, 150, 255, Imgproc.THRESH_BINARY);
		
		// remove noise
		Imgproc.erode(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
		Imgproc.dilate(tmp, tmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6)));
		
		// find contours
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		tmp.convertTo(tmp, CvType.CV_8UC1);
		
		
//		Imgproc.findContours(tmp, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
//		
//		tmp = new Mat(tmp.rows(), tmp.cols(), CvType.CV_8UC1, new Scalar(0, 0, 0));
//		if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
//			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0])
//		        {
//		                Imgproc.drawContours(tmp, contours, idx, new Scalar(255, 255, 255));
//		        }
//		}
		
//		Mat hor = new Mat();
//		Mat ver = new Mat();
//		Core.reduce(tmp, hor, 0, Core.REDUCE_MAX);
//		Core.reduce(tmp, ver, 1, Core.REDUCE_MAX);
//		
//		ArrayList<Integer> transitions = new ArrayList<>();
//		ArrayList<Double> centers = new ArrayList<>();
//		
//		double width = 0;
//		
//		// identify first, last and number of blobs in one dimension
//		// identify median of distance between start of blobs
//		
//		boolean white = false;
//		
//		// identify transitions from black to white and vice versa
//		for (int i = 0; i < hor.cols(); i++) {
//			// current pixel is white and previous one was black
//			if (hor.get(0, i)[0] > 0 && !white) {
//				white = true;
//				transitions.add(i);
//			}
//			// current pixel is black and previous one was white
//			else if (hor.get(0, i)[0] == 0 && white) {
//				white = false;
//				transitions.add(i);
//			}
//		}
//		// just to prevent exceptions
//		if (transitions.size() % 2 != 0) {
//			transitions.add(hor.cols()-1);
//			System.out.println("Auto detection failed.");
//		}
//		
//		// remove transitions that are less than three pixels apart
//		for (int i = 0; i < transitions.size()-1; i++) {
//			if (transitions.get(i+1) - transitions.get(i) <= 3) {
//				transitions.remove(i);
//				transitions.remove(i);
//			}
//		}
//		
//		// calculate centers of blobs and average size of blobs
//		for (int i = 0; i < transitions.size()-1; i+=2) {
//			centers.add((transitions.get(i+1)+transitions.get(i))/2.0);
//			width += transitions.get(i+1) - transitions.get(i);
//		}
//		
//		// calculate median distances
//		ArrayList<Double> dist = new ArrayList<>();
//		for (int i = 0; i < centers.size()-1; i++) {
//			dist.add(centers.get(i+1) - centers.get(i));
//		}
//		// just to prevent exceptions
//		if (dist.size() == 0) {
//			dist.add(5.0);
//			System.out.println("Auto detection failed.");
//		}
//		Collections.sort(dist);
//		double med_dist = dist.get(dist.size()/2);
//		
//		// this is just temporary, should use median of size to calculate number of rows/cols
//		cols = (int) Math.round((centers.get(centers.size()-1) - centers.get(0))/med_dist) + 1;
//		size = (int) Math.round((width /= cols)*1.1);
//		
//		x_lowValue = centers.get(0)/(double)x;
//		x_highValue = centers.get(centers.size()-1)/(double)x;
//		
//		System.out.println((int) Math.round((width /= rows)*1.1));
		
		double size_est = setAutoValues(true, tmp);
		size_est += setAutoValues(false, tmp);
		size = (int) Math.round(size_est/1.9);
		
		updateControls();
		update();
		controller.drawGrid();
		
		// mat = tmp;
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
