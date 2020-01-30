package application;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

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
