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

public class FXController {

	@FXML
	private Button button;
	@FXML
	private ImageView displayedImage;
	@FXML
	private Spinner<Integer> rows;
	@FXML
	private Spinner<Integer> columns;
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
	
	@SuppressWarnings("unchecked")
	@FXML
	private void initialize() {
		for (Spinner<Integer> s : new Spinner[]{rows, columns, size, x_dist, y_dist, x_offset, y_offset}) {
			s.getEditor().textProperty().addListener(new SpinnerListener(s));
		}
	}
	
	@FXML
	protected void drawGrid() {
		System.out.println("drawGrid");
		int width = 800;
		int height = 400;
		
		Mat mat = new Mat(height, width, CvType.CV_8UC3, new Scalar(205, 205, 225));
		
		for (int i = 0; i < rows.getValue(); i++) {
			for (int j = 0; j < columns.getValue(); j++) {
				Imgproc.circle(mat, new Point(
						x_offset.getValue() + size.getValue() + i*x_dist.getValue(),
						y_offset.getValue() + size.getValue() + j*y_dist.getValue()),
						size.getValue(),
						new Scalar(255, 0, 0),
						2);
			}	
		}
		
		displayedImage.setImage(mat2img(mat));
		displayedImage.setFitWidth(width);
		displayedImage.setFitHeight(height);
	}
	
	@FXML
	protected void updateRows() {
		System.out.println("updateRows");
	}
	
	@FXML
	protected void updateColumns() {
		System.out.println("updateColumns");
	}
	
	// https://stackoverflow.com/a/33605064
	private Image mat2img(Mat mat) {
		MatOfByte byteMat = new MatOfByte();
		Imgcodecs.imencode(".bmp", mat, byteMat);
		return new Image(new ByteArrayInputStream(byteMat.toArray()));
	}
	
}
