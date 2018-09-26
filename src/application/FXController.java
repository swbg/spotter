package application;

import java.io.File;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.controlsfx.control.RangeSlider;

public class FXController {
	
	private int currentIndex;
	private Prop prop;
	private ObservableList<Config> configs;
	
	private boolean viewFixed;
	
	private Window stage;

	@FXML
	private Button prev;
	@FXML
	private Button next;
	@FXML
	private CheckBox save;
	@FXML
	private ImageView displayedImage;
	@FXML
	private TextField filename;
	@FXML
	protected Spinner<Integer> rows;
	@FXML
	protected Spinner<Integer> cols;
	@FXML
	protected Spinner<Integer> size;
	@FXML
	protected RangeSlider x_range;
	@FXML
	protected RangeSlider y_range;
	@FXML
	private AnchorPane anchorPane;
	@FXML
	private ListView<Config> openFiles;
	@FXML
	private Label notificationBar;
	@FXML
	protected TextField mask;
	@FXML
	private Slider brightnessSlider;
	@FXML
	private Slider contrastSlider;

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
			
			// only allow integer input
			if (!newValue.matches("\\d*")) {
	            s.getEditor().setText(oldValue);
	        }
			
			if(!s.getEditor().getText().isEmpty()) {
				// fix spinner bug
				s.increment(0);
				drawGrid();
			}
			inProgress = false;
		}
	}
	
	private class SliderListener implements ChangeListener<Number> {

		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			drawGrid();
		}
	}
	
	private class RangeSliderListener implements ChangeListener<Number> {
		
		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			drawGrid();
		}
	}
	
	private class DisabledTextFieldListener implements ChangeListener<String> {
		
		private boolean inProgress = false;
		private TextField t;
		
		private DisabledTextFieldListener(TextField t) {
			this.t = t;
		}
		
		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			if (inProgress) {
				return;
			}
			inProgress = true;
			// prevent editing of file name
			t.setText(getConfig().toString());
			inProgress = false;
		}
	}
	
	private class MaskTextFieldListener implements ChangeListener<String> {
		
		private boolean inProgress = false;
		private TextField t;
		
		private MaskTextFieldListener(TextField t) {
			this.t = t;
		}
		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			if (inProgress) {
				return;
			}
			inProgress = true;
			
			if (!newValue.matches("\\d{0,7}([\\.]\\d{0,4})?")) {
            	t.setText(oldValue);
            }
			
			if(!t.getText().isEmpty()) {
				getConfig().update();
				getConfig().resetMask();
			}
			
			inProgress = false;
        }
	}
	
	private class ImageClickHandler implements EventHandler<MouseEvent> {

		@Override
		public void handle(MouseEvent event) {
			int[] index = getConfig().getClickedSpot(event.getX(), event.getY());
			if (index != null) {
				getConfig().toggleMask(index[0], index[1]);
				drawGrid();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@FXML
	private void initialize() {
		fixView();
		for (Spinner<Integer> s : new Spinner[]{rows, cols, size}) {
			s.getEditor().textProperty().addListener(new SpinnerListener(s));
		}
		
		mask.textProperty().addListener(new MaskTextFieldListener(mask));
		
		x_range.highValueProperty().addListener(new RangeSliderListener());
		x_range.lowValueProperty().addListener(new RangeSliderListener());
		y_range.highValueProperty().addListener(new RangeSliderListener());
		y_range.lowValueProperty().addListener(new RangeSliderListener());
		
		filename.textProperty().addListener(new DisabledTextFieldListener(filename));
		
		brightnessSlider.valueProperty().addListener(new SliderListener());
		contrastSlider.valueProperty().addListener(new SliderListener());
		
		displayedImage.addEventHandler(MouseEvent.MOUSE_CLICKED, new ImageClickHandler());
		
		prop = new Prop();
		
		configs = FXCollections.observableArrayList();
		
		File folder = new File("/home/stefan/Documents/eclipse/spotter/data/test");
		for (File f : folder.listFiles()) {
			configs.add(new Config(Util.fromFile(f), this, f, prop));
		}

		currentIndex = 0;
		openFiles.setItems(configs);
		
		rows.getValueFactory().setValue(prop.defaultRows);
		cols.getValueFactory().setValue(prop.defaultCols);
		size.getValueFactory().setValue(prop.defaultSize);
		
		// autoAll();
		
		unfixView();
	}
	
	private Config getConfig() {
		if (configs != null & configs.size() > 0) {
			return configs.get(currentIndex);
		}
		else {
			Mat mat = new Mat(400, 600, CvType.CV_16UC3, new Scalar(0, 0, 0));
			return new Config(mat, this, null, prop);
		}
		
	}
	
	private void readConfig() {
		fixView();
		Config config = getConfig();
		config.fix();
		
		cols.getEditor().setText("" + config.cols);
		rows.getEditor().setText("" + config.rows);
		size.getEditor().setText("" + config.size);
		
		x_range.setHighValue(config.x_highValue);
		x_range.setLowValue(config.x_lowValue);
		x_range.setHighValue(config.x_highValue);

		y_range.setHighValue(config.y_highValue);
		y_range.setLowValue(config.y_lowValue);
		y_range.setHighValue(config.y_highValue);
		
		config.unfix();
		unfixView();
	}
	
	private void updateView() {
		if (viewFixed) {
			return;
		}
		boolean oldViewFixed = viewFixed;
		fixView();
		filename.setText(getConfig().toString());
		save.selectedProperty().set(getConfig().isSaved());
		openFiles.getSelectionModel().select(currentIndex);
		viewFixed = oldViewFixed;
		drawGrid();
	}
	
	public void fixView() {
		viewFixed = true;
	}
	
	public void unfixView() {
		viewFixed = false;
		updateView();
	}
	
	@FXML
	protected void drawGrid() {
		if (viewFixed) {
			return;
		}
		boolean oldViewFixed = viewFixed;
		fixView();
		Config config = getConfig();
		config.update();
		
		// set AnchorPane dimensions
		anchorPane.setMaxWidth(config.x+16);
		anchorPane.setMaxHeight(config.y+16);
		anchorPane.setMinWidth(config.x+16);
		anchorPane.setMinHeight(config.y+16);

		Mat mat = config.getMat().clone();
		
		double alpha = Math.pow(1.1, contrastSlider.getValue()-25);
		double beta = (brightnessSlider.getValue()-50)*255/50;
		mat.convertTo(mat, mat.type(), alpha, beta);
		
		// draw guide lines
		Imgproc.line(mat, new Point(0, config.y_upper), new Point(mat.cols(), config.y_upper),
				new Scalar(0, 0, 255), 1);
		Imgproc.line(mat, new Point(0, config.y_lower), new Point(mat.cols(), config.y_lower),
				new Scalar(0, 0, 255), 1);
		Imgproc.line(mat, new Point(config.x_upper, 0), new Point(config.x_upper, mat.rows()),
				new Scalar(0, 0, 255), 1);
		Imgproc.line(mat, new Point(config.x_lower, 0), new Point(config.x_lower, mat.rows()),
				new Scalar(0, 0, 255), 1);

		// draw grid
		for (int i = 0; i < config.rows; i++) {
			for (int j = 0; j < config.cols; j++) {
				Imgproc.rectangle(mat,
						new Point(config.x_lower - config.size/2 + j*config.x_dist, config.y_upper - config.size/2 + i*config.y_dist),
						new Point(config.x_lower + config.size/2 + j*config.x_dist, config.y_upper + config.size/2 + i*config.y_dist),
						new Scalar(0, 0, 255),
						1);
//				Imgproc.circle(mat,
//						new Point(config.x_lower + j*config.x_dist,	config.y_upper + i*config.y_dist),
//						config.size,
//						new Scalar(0, 0, 255),
//						1);
			}	
		}
		
		System.out.println("rows/cols " + config.rows + " " + config.cols);
		System.out.println("spinner rows/cols" + rows.getValue() + " " + cols.getValue());
		System.out.println(getConfig().masked.length + " " + getConfig().masked[0].length);
		
		// indicate masked spots
		Analysis analysis = getConfig().getAnalysis();
		if (analysis != null) {
			for (int i = 0; i < config.rows; i++) {
				for (int j = 0; j < config.cols; j++) {
					if (getConfig().masked[j][i]) {
						Imgproc.line(mat,
								new Point(config.x_lower - config.size/2 + j*config.x_dist, config.y_upper - config.size/2 + i*config.y_dist),
								new Point(config.x_lower + config.size/2 + j*config.x_dist, config.y_upper + config.size/2 + i*config.y_dist),
								new Scalar(0, 0, 255),
								1);
						Imgproc.line(mat,
								new Point(config.x_lower + config.size/2 + j*config.x_dist, config.y_upper - config.size/2 + i*config.y_dist),
								new Point(config.x_lower - config.size/2 + j*config.x_dist, config.y_upper + config.size/2 + i*config.y_dist),
								new Scalar(0, 0, 255),
								1);
					}
				}
			}
		}
		
		displayedImage.setImage(Util.toImage(mat));
		displayedImage.setFitWidth(mat.cols());
		displayedImage.setFitHeight(mat.rows());
		viewFixed = oldViewFixed;
	}
	
	@FXML
	protected void incrementIndex() {
		if (currentIndex < configs.size()-1) {
			currentIndex++;
			if(getConfig().isSaved()) {
				readConfig();
			}
			setNotification("");
			updateView();
		}	
	}
	
	@FXML
	protected void decrementIndex() {
		if (currentIndex > 0) {
			currentIndex--;
			if(getConfig().isSaved()) {
				readConfig();
			}
			setNotification("");
			updateView();
		}
	}
	
	@FXML
	protected void analyzeMatrix() {
		getConfig().analyze();
		getConfig().printAnalysis();
		// getConfig().writeAnalysis();
		updateView();
	}
	
	@FXML
	protected void selectItem() {
		currentIndex = openFiles.getSelectionModel().getSelectedIndex();
		if(getConfig().isSaved()) {
			readConfig();
		}
		setNotification("");
		updateView();
	}
	
	@FXML
	protected void quitProgram() {
		System.exit(0);
	}
	
	@FXML
	protected void openFiles() {
		FileChooser fileChooser = new FileChooser();
		List<File> list = fileChooser.showOpenMultipleDialog(stage);
        if (list != null) {
            for (File file : list) {
                Mat mat = Util.fromFile(file);
                if (mat != null) {
                    configs.add(new Config(mat, this, file, prop));
                }
                else {
                	Alert alert = new Alert(AlertType.NONE,
                			"File " + file.getName() + " is not formatted correctly!", ButtonType.OK);
                	alert.showAndWait();
                }
            }
        }
        autoAll();
        autoDetect();
        // updateView();
	}
	
	@FXML
	protected void closeFile() {
		configs.remove(currentIndex);
		currentIndex = Math.min(currentIndex, configs.size()-1);
		updateView();
	}
	
	@FXML
	protected void closeAllFiles() {
		configs.clear();
		currentIndex = 0;
		updateView();
	}
	
	@FXML
	protected void saveConfig() {
		if (save.isSelected()) {
			getConfig().save();
		}
		else {
			getConfig().unsave();
		}
	}
	
	@FXML
	protected void showAbout() {
		Alert alert = new Alert(AlertType.NONE,
    			"Spotter v0.1\n" +
    			"github.com/swbg/spotter\n" +
    			"weissenberger.stefan@gmx.net\n" +
    			"uses OpenCV 3.4.2 and controlsfx 8.40.14", ButtonType.OK);
    	alert.showAndWait();
	}
	
	@FXML
	protected void autoDetect() {
		fixView();
		getConfig().autoDetect();
		unfixView();
	}
	
	@FXML
	protected void autoAll() {
		for (Config c : configs) {
			c.autoDetect();
		}
		getConfig().autoDetect();
		updateView();
	}
	
	@FXML
	protected void analyzeAll() {
		for (Config c : configs) {
			c.analyze();
		}
	}
	
	@FXML
	protected void analyzeAndNext() {
		analyzeMatrix();
		incrementIndex();
	}
	
	@FXML
	protected void removeMask() {
		getConfig().masked = new boolean[cols.getValue()][rows.getValue()];
		updateView();
	}
	
	@FXML
	protected void toggleZoom() {
		
	}
	
	public void setStage(Window stage) {
		this.stage = stage;
	}
	
	public void setNotification(String text) {
		notificationBar.setText(text);
	}
	
}
