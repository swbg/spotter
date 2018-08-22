package application;

import org.controlsfx.control.RangeSlider;

import javafx.scene.control.Spinner;

public class Config {
	
	private int x;
	private int y;
	private Spinner<Integer> rows_elem;
	private Spinner<Integer> cols_elem;
	private Spinner<Integer> size_elem;
	private RangeSlider x_range_elem;
	private RangeSlider y_range_elem;
	
	public int rows, cols, size;
	public double x_upper, y_upper, x_lower, y_lower, x_dist, y_dist;

//	public Config(int rows, int cols, int x_dist, int y_dist, int x_offset, int y_offset) {
//		this.rows = rows;
//		this.cols = cols;
//		this.x_dist = x_dist;
//		this.y_dist = y_dist;
//		this.x_offset = x_offset;
//		this.y_offset = y_offset;
//	}
	
	public Config(int x, int y, Spinner<Integer> rows, Spinner<Integer> cols,
			Spinner<Integer> size, RangeSlider x_range, RangeSlider y_range) {
		this.x = x;
		this.y = y;
		this.rows_elem = rows;
		this.cols_elem = cols;
		this.size_elem = size;
		this.x_range_elem = x_range;
		this.y_range_elem = y_range;
		update();
	}
	
	public void update() {
		rows = rows_elem.getValue();
		cols = cols_elem.getValue();
		size = size_elem.getValue();
		y_upper = (1-y_range_elem.getHighValue())*y;
		y_lower = (1-y_range_elem.getLowValue())*y;
		x_upper = x_range_elem.getHighValue()*x;
		x_lower = x_range_elem.getLowValue()*x;
		y_dist = (y_lower-y_upper)/(rows-1);
		x_dist = (x_upper-x_lower)/(cols-1);
	}
	
}
