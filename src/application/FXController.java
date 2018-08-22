package application;

import java.awt.Button;
import java.io.ByteArrayInputStream;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import org.controlsfx.control.RangeSlider;

public class FXController {
	
	private Mat currentMat;
	private Config config;

	@FXML
	private Button button;
	@FXML
	private ImageView displayedImage;
	@FXML
	private Spinner<Integer> rows;
	@FXML
	private Spinner<Integer> cols;
	@FXML
	private Spinner<Integer> size;
	@FXML
	private Spinner<Integer> x_dist;
	@FXML
	private Spinner<Integer> y_dist;
	@FXML
	private Spinner<Integer> x_offset;
	@FXML
	private Spinner<Integer> y_offset;
	@FXML
	private RangeSlider x_range;
	@FXML
	private RangeSlider y_range;
	@FXML
	private AnchorPane anchorPane;
	
	private class SpinnerListener implements ChangeListener<String> {
		
		private boolean inProgress = false;
		private Spinner<Integer> s;
		
		private SpinnerListener(Spinner<Integer> s) {
			this.s = s;
		}
		
		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			if (inProgress) {
				return;
			}
			inProgress = true;
			if (!newValue.matches("\\d*")) {
	            s.getEditor().setText(newValue.replaceAll("[^\\d]", ""));
	            if(s.getEditor().getText().isEmpty()) {
	            	s.getEditor().setText(oldValue);
	            }
	        }
			
			s.increment(0);
			drawGrid();
			inProgress = false;
		}
	}
	
	private class RangeSliderListener implements ChangeListener<Number> {
		
		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			drawGrid();
		}
	}
	
	@SuppressWarnings("unchecked")
	@FXML
	private void initialize() {
//		for (Spinner<Integer> s : new Spinner[]{rows, cols, size, x_dist, y_dist, x_offset, y_offset}) {
//			s.getEditor().textProperty().addListener(new SpinnerListener(s));
//		}
		
		for (Spinner<Integer> s : new Spinner[]{rows, cols, size}) {
			s.getEditor().textProperty().addListener(new SpinnerListener(s));
		}
		
		x_range.highValueProperty().addListener(new RangeSliderListener());
		x_range.lowValueProperty().addListener(new RangeSliderListener());
		y_range.highValueProperty().addListener(new RangeSliderListener());
		y_range.lowValueProperty().addListener(new RangeSliderListener());
		
		String path = "/home/stefan/Documents/eclipse/spotter/data/Legn Pneum 7_CK_20171213_66_5_2017.12.13-10.38.44.txt";
		currentMat = Util.fromFile(path);
		
		config = new Config(currentMat.cols(), currentMat.rows(), rows, cols, size, x_range, y_range);
		drawGrid();
	}
	
	@FXML
	protected void drawGrid() {
		// set AnchorPane dimensions 
		anchorPane.setMaxWidth(currentMat.cols()+16);
		anchorPane.setMaxHeight(currentMat.rows()+16);
		anchorPane.setMinWidth(currentMat.cols()+16);
		anchorPane.setMinHeight(currentMat.rows()+16);
		
		Mat mat = currentMat.clone();
		
		config.update();
		
		// draw upper horizontal line
		Imgproc.line(mat, new Point(0, config.y_upper), new Point(mat.cols(), config.y_upper),
				new Scalar(0, 0, 255), 1);
		// draw lower horizontal line
		Imgproc.line(mat, new Point(0, config.y_lower), new Point(mat.cols(), config.y_lower),
				new Scalar(0, 0, 255), 1);
		// draw right vertical line
		Imgproc.line(mat, new Point(config.x_upper, 0), new Point(config.x_upper, mat.rows()),
				new Scalar(0, 0, 255), 1);
		// draw left vertical line
		Imgproc.line(mat, new Point(config.x_lower, 0), new Point(config.x_lower, mat.rows()),
				new Scalar(0, 0, 255), 1);

		// draw grid
		for (int i = 0; i < config.rows; i++) {
			for (int j = 0; j < config.cols; j++) {
				Imgproc.circle(mat,
						new Point(config.x_lower + j*config.x_dist,	config.y_upper + i*config.y_dist),
						config.size,
						new Scalar(255, 0, 0),
						2);
			}	
		}
		
		displayedImage.setImage(Util.toImage(mat));
		displayedImage.setFitWidth(mat.cols());
		displayedImage.setFitHeight(mat.rows());
		System.out.println("done");
	}
	
	@FXML
	protected void updateRows() {
		System.out.println("updateRows");
	}
	
	@FXML
	protected void updateColumns() {
		System.out.println("updateColumns");
	}
	
}
