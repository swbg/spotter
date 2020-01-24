package application;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.controlsfx.control.RangeSlider;

public class MainController {
	
	private int currentIndex;
	public Prop prop;
	private ObservableList<Config> configs;
	private boolean viewFixed;
	private boolean showGrid = true;
	
	private File lastDirectory = new File(System.getProperty("user.home"));
	
	// private double brightness, contrast;
	
	// private Window stage;

	@FXML
	private Button prev;
	@FXML
	private Button next;
	// @FXML
	// private CheckBox save;
	@FXML
	public ImageView displayedImage;
	@FXML
	public ImageView valueDisplay;
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
	@FXML
	private ScrollPane scroller;
	// @FXML
	// private Button autoButton;
	@FXML
	private Button removeMaskButton;
	@FXML
	private Button analyzeButton;

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
	
	private class BrightnessSliderListener implements ChangeListener<Number> {

		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			getConfig().brightness = brightnessSlider.getValue();
			drawGrid();
		}
	}
	
	private class ContrastSliderListener implements ChangeListener<Number> {

		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			getConfig().contrast = contrastSlider.getValue();
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
		
		brightnessSlider.valueProperty().addListener(new BrightnessSliderListener());
		contrastSlider.valueProperty().addListener(new ContrastSliderListener());
		
		displayedImage.addEventHandler(MouseEvent.MOUSE_CLICKED, new ImageClickHandler());
		
		prop = new Prop();
		
		configs = FXCollections.observableArrayList();
		
		/*
		File folder = new File("/home/stefan/Documents/eclipse/spotter/data/5x21_2");
		for (File f : folder.listFiles()) {
			if (f.isDirectory()) { 
				continue;
			}
			Mat m = Util.fromFile(f);
			if (m != null) {
				configs.add(new Config(Util.fromFile(f), this, f, prop));
			}
		}
		*/

		currentIndex = 0;
		openFiles.setItems(configs);
		
		rows.getValueFactory().setValue(prop.defaultRows);
		cols.getValueFactory().setValue(prop.defaultCols);
		size.getValueFactory().setValue(prop.defaultSize);
		
		// autoAll();
		
		scroller.prefHeightProperty().bind(anchorPane.heightProperty());
		
		unfixView();
	}
	
	private Config getConfig() {
		if (configs != null & configs.size() > 0) {
			return configs.get(currentIndex);
		}
		else {
			Mat mat = new Mat(600, 800, CvType.CV_16UC3, new Scalar(0, 0, 0));
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
		
		brightnessSlider.setValue(config.brightness);
		contrastSlider.setValue(config.contrast);
		
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
		
		// disable controls
		if (getConfig().getAnalysis().analyzed) {
			rows.setDisable(true);
			cols.setDisable(true);
			size.setDisable(true);
			mask.setDisable(true);
			analyzeButton.setDisable(true);
			// autoButton.setDisable(true);
			removeMaskButton.setDisable(true);
		} else {
			rows.setDisable(false);
			cols.setDisable(false);
			size.setDisable(false);
			mask.setDisable(false);
			analyzeButton.setDisable(false);
			// autoButton.setDisable(false);
			removeMaskButton.setDisable(false);
		}

		// save.selectedProperty().set(getConfig().isSaved());
		openFiles.getSelectionModel().select(currentIndex);
		viewFixed = oldViewFixed;
		drawGrid();
	}
	
	public void setRowsAndCols(int rows, int cols) {
		fixView();
		for (Config c : configs) {
			if (c.rows != rows || c.cols != cols) {
				c.rows = rows;
				c.cols = cols;
				c.masked = new boolean[cols][rows];
				c.getAnalysis().analyzed = false;
			}
		}
		this.cols.getEditor().setText("" + cols);
		this.rows.getEditor().setText("" + rows);
		unfixView();
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
		// anchorPane.setMaxWidth(config.x+16);
		anchorPane.setMinWidth(config.x+16);
		// anchorPane.setMaxHeight(config.y+16);
		anchorPane.setMinHeight(config.y+16);
		
		
		// scroller.setMinViewportHeight(config.y+16);
		// scroller.setMaxHeight(scroller.getMaxHeight()-156);
		// scroller.setMinHeight(config.y+16+300);
		
		Mat mat = getMat(config);
		
		y_range.prefHeightProperty().unbind();
		y_range.prefHeightProperty().set(mat.rows());
		
		if (config.getAnalysis().analyzed) {
			y_range.setVisible(false);
			x_range.setVisible(false);
		} else {
			y_range.setVisible(true);
			x_range.setVisible(true);
		}
		
		displayedImage.setImage(Util.toImage(mat));
		displayedImage.setFitHeight(mat.rows());
		displayedImage.setFitWidth(mat.cols());

		viewFixed = oldViewFixed;
	}
	
	private Mat getMat(Config config) {
		Mat mat = config.getMat().clone();

		double alpha = Math.pow(1.1, config.contrast-25);
		double beta = (config.brightness-100)*65535/100;
		mat.convertTo(mat, mat.type(), alpha, beta);
		
		// draw guide lines
//		Imgproc.line(mat, new Point(0, config.y_upper), new Point(mat.cols(), config.y_upper),
//				new Scalar(0, 0, 255), 1);
//		Imgproc.line(mat, new Point(0, config.y_lower), new Point(mat.cols(), config.y_lower),
//				new Scalar(0, 0, 255), 1);
//		Imgproc.line(mat, new Point(config.x_upper, 0), new Point(config.x_upper, mat.rows()),
//				new Scalar(0, 0, 255), 1);
//		Imgproc.line(mat, new Point(config.x_lower, 0), new Point(config.x_lower, mat.rows()),
//				new Scalar(0, 0, 255), 1);
		
		
		if (showGrid) {		
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
			
	//		// draw auto indicators
	//		for (Double d : config.autoValues) {
	//			Imgproc.line(mat, new Point(d, 0), new Point(d, 9), new Scalar(0, 255, 255), 1);
	//			// Imgproc.line(mat, new Point(d, mat.rows()-10), new Point(d, mat.rows()), new Scalar(0, 255, 255), 1);
	//		}
	
			// label rows
			for (int i = 0; i < config.rows; i++) {
				Imgproc.putText(mat, "" + (char) (65 + i),
						new Point(config.x_lower - 20 - 0.5*config.size, 5 + config.y_upper + i*(config.y_lower-config.y_upper)/(config.rows-1)),
						Core.FONT_ITALIC, 0.5, new Scalar(0, 0, 255));
			}
			// label cols
			for (int i = 0; i < config.cols; i++) {
				int correction = (i<9 ? 5 : (i<19) ? 12 : 10);
				Imgproc.putText(mat, "" + (1 + i),
						new Point(config.x_lower - correction + i*(config.x_upper-config.x_lower)/(config.cols-1), config.y_upper - 10 - 0.5*config.size),
						Core.FONT_ITALIC, 0.5, new Scalar(0, 0, 255));
			}
			
			// draw mask crosses
			for (int i = 0; i < config.rows; i++) {
				for (int j = 0; j < config.cols; j++) {
					if (config.masked[j][i]) {
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
			
		// indicate masked spots
		Analysis analysis = config.getAnalysis();
		if (analysis.analyzed) {
			// crop image
			mat = mat.rowRange((int) Math.round(config.y_upper-50),	(int) Math.round(config.y_lower+30));

			int displaySize = 110 + (config.rows+1)*60;
			BufferedImage bufferedImage = new BufferedImage(mat.cols(), displaySize, BufferedImage.TYPE_3BYTE_BGR); //USHORT_555_RGB);
	        Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
	        graphics.setRenderingHint(
	                RenderingHints.KEY_TEXT_ANTIALIASING,
	                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	        graphics.setColor(Color.WHITE);
	        graphics.fillRect(0, 0, mat.cols(), displaySize);
	        graphics.setColor(Color.BLACK);
	        graphics.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 16));
	        
	        for (int i = 0; i < config.cols; i++) {
				if (i % 2 == 0) {
					graphics.drawString("" + (1 + i), 40 + i*30, 20);
				} else {
					graphics.drawString("" + (1 + i), 40 + i*30, 20+20);
				}
			}
	        
	        for (int i = 0; i < config.rows; i++) {
	        	graphics.drawString("" + (char) (65 + i), 14, 80 + i*60);
				for (int j = 0; j < config.cols; j++) {
					if (config.masked[j][i]) {
						graphics.setColor(Color.GRAY);
				        graphics.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 16));
					} else {
						graphics.setColor(Color.BLACK);
				        graphics.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 16));
					}
					if (j % 2 == 0) {
						graphics.drawString("" + (int) Math.round(analysis.getSpots()[j][i]*65535),
								40 + j*30, 80 + i*60);
					} else {
						graphics.drawString("" + (int) Math.round(analysis.getSpots()[j][i]*65535),
								40 + j*30, 100 + i*60);
					}
				}
			}
	        
	        graphics.drawLine(10, 52, mat.cols()-10, 52);
	        graphics.drawLine(32, 10, 32, displaySize-10);
	        
	        graphics.drawLine(10, 52+60*config.rows, mat.cols()-10, 52+60*config.rows);
	        
	        int i = config.rows;
	        graphics.drawString("m", 13, 80 + i*60);
	        graphics.drawString("sd", 11, 80 + (i+1)*60);
	        
	        // means
	        for (int j = 0; j < config.cols; j++) {
				if (j % 2 == 0) {
					graphics.drawString("" + (int) Math.round(analysis.getMean()[j]*65535),
							40 + j*30, 80 + i*60);
				} else {
					graphics.drawString("" + (int) Math.round(analysis.getMean()[j]*65535),
							40 + j*30, 100 + i*60);
				}
			}
	        
	        // sd
	        for (int j = 0; j < config.cols; j++) {
	        	if (j % 2 == 0) {
					graphics.drawString("" + (int) Math.round(analysis.getSd()[j]*65535),
							40 + j*30, 80 + (i+1)*60);
				} else {
					graphics.drawString("" + (int) Math.round(analysis.getSd()[j]*65535),
							40 + j*30, 100 + (i+1)*60);
				}
			}
	        
	        mat.push_back(Util.bufferedImageToMat(bufferedImage));
			
			/*
			int displaySize = 350;
			Mat tmp = new Mat(displaySize, mat.cols(), mat.type(), new Scalar(255,255,255));
			mat.push_back(tmp);
			
			for (int i = 0; i < config.cols; i++) {
				Point p;
				if (i % 2 == 0) {
					p = new Point(40 + i*30, mat.rows()-displaySize+20);
				} else {
					p = new Point(40 + i*30, mat.rows()-displaySize+20+20);
				}
				Imgproc.putText(mat, "" + (1 + i),
						p,
						Core.FONT_ITALIC, 0.5, new Scalar(255, 0, 0));
			}
			
			Imgproc.line(mat,
					new Point(30, mat.rows()-displaySize+20+30),
					new Point(mat.cols()-30, mat.rows()-displaySize+20+30),
					new Scalar(255, 0, 0),
					1);
			Imgproc.line(mat,
					new Point(30, mat.rows()-displaySize+20+30),
					new Point(30, mat.rows()-30),
					new Scalar(255, 0, 0),
					1);
			
			for (int i = 0; i < config.rows; i++) {
				Imgproc.putText(mat, "" + (char) (65 + i),
						new Point(15, mat.rows()-displaySize+70 + i*60), Core.FONT_ITALIC, 0.45, new Scalar(255, 0, 0));
				for (int j = 0; j < config.cols; j++) {
					Point p;
					if (j % 2 == 0) {
						p = new Point(40 + j*30, mat.rows()-displaySize+70 + i*60);
					} else {
						p = new Point(40 + j*30, mat.rows()-displaySize+20+70 + i*60);
					}
					Imgproc.putText(mat, "" + (int) Math.round(analysis.getSpots()[j][i]*65535),
							p, Core.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 0, 0));
				}
			}
			*/
		}
		
		// print analysis values
//		for (int i = 0; i < config.rows; i++) {
//			for (int j = 0; j < config.cols; j+=2) {
//				
//			}
//			for (int j = 1; j < config.cols; j+=2) {
//				
//			}
//		}
		
		return mat;		
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
	protected void analyzeConfig() {
		if (configs.size() == 0) {
			return;
		}
		getConfig().analyzeConfig();
		updateView();
	}
	
	@FXML
	protected void analyzeAll() {
		for (Config c : configs) {
			c.analyzeConfig();
		}
	}
	
	@FXML
	protected void analyzeAndNext() {
		analyzeConfig();
		incrementIndex();
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
		fileChooser.setInitialDirectory(lastDirectory);
		Stage stage = (Stage) rows.getScene().getWindow();
		List<File> list = fileChooser.showOpenMultipleDialog(stage);

        if (list != null) {
        	lastDirectory = list.get(0).getParentFile();
    		
            for (File file : list) {
            	if (Util.getExtension(file).equals("txt")) {
                	Mat mat = Util.fromFile(file);
                    if (mat != null) {
                        configs.add(new Config(mat, this, file, prop));
                    }
                    else {
                    	Alert alert = new Alert(AlertType.NONE,
                    			"File\n" + file.getName() + "\nis not formatted correctly!", ButtonType.OK);
                    	alert.showAndWait();
                    }
            	} else if (Util.getExtension(file).equals("ser")) {
            		try {
            			FileInputStream fi = new FileInputStream(file);
            			ObjectInputStream oi = new ObjectInputStream(fi);
            			for (Object o = oi.readObject(); o != null; o = oi.readObject()) {
            				Config c = (Config) o;
                            c.deserialize_mat();
                            c.setController(this);
                            System.out.println("read " + c.getFileName());
                            configs.add(c);
            			}
                        oi.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
            		
            	} else if (Util.getExtension(file).equals("png")) {
            		Mat mat = Util.fromPNG(file);
            		if (mat != null) {
                        configs.add(new Config(mat, this, file, prop));
                    }
                    else {
                    	Alert alert = new Alert(AlertType.NONE,
                    			"File\n" + file.getName() + "\nis not formatted correctly!", ButtonType.OK);
                    	alert.showAndWait();
                    }
            	} else {
            		Alert alert = new Alert(AlertType.NONE,
                			"File " + file.getName() + " has unknown file extension!", ButtonType.OK);
                	alert.showAndWait();
            	}
            }
        }
        if (configs.size() > 0) {
        	getConfig().updateControls();
        }
        
        // System.out.println("Index: " + currentIndex);
        
        // autoAll();
        // autoDetect();
        updateView();
	}
	
	@FXML
	protected void closeFile() {
		if (configs == null || configs.size() == 0) {
			return;
		}
		configs.remove(currentIndex);
		currentIndex = Math.max(0, Math.min(currentIndex, configs.size()-1));
		updateView();
	}
	
	@FXML
	protected void closeAllFiles() {
		if (configs == null || configs.size() == 0) {
			return;
		}
		configs.clear();
		currentIndex = 0;
		updateView();
	}
	
//	@FXML
//	protected void saveConfig() {
//		if (save.isSelected()) {
//			getConfig().save();
//		}
//		else {
//			getConfig().unsave();
//		}
//	}
	
	@FXML
	protected void showAbout() {
		Alert alert = new Alert(AlertType.NONE,
    			"Spotter v0.1\n" +
    			"contact" +
    			"\tStefan Weißenberger\n" +
    			"\t\tweissenberger.stefan@gmx.net\n" +
    			"\t\tgithub.com/swbg/spotter\n" +
    			"built with" +
    			"\tOpenCV 3.4.2\n" +
    			"\t\tcontrolsfx 8.40.14\n" +
    			"\t\tApache POI 4.0.0\n" +
    			"\t\tApache Commons Compress 1.18", ButtonType.OK);
    	alert.showAndWait();
	}
	
	@FXML
	protected void autoDetect() {
		// fixView();
		// getConfig().semiAuto(0);
		// getConfig().getAnalysis().analyzed = false;
		// unfixView();
		autoAll();
	}
	
	@FXML
	protected void propagateGrid() {
		Config template = getConfig();
		
		for (Config c : configs) {
			if (c.equals(template)) {
				continue;
			}
			c.update();
		}		
	}
	
	@FXML
	protected void autoAll() {
//		fixView();
//		for (Config c : configs) {
//			if (!c.equals(getConfig())) {
//				c.semiAuto(0);
//			}
//		}
//		getConfig().semiAuto(0);
//		unfixView();
		fixView();
		ArrayList<ArrayList<Double>> l = new ArrayList<>();
		for (Config c : configs) {
			c.cols = prop.defaultCols;
			c.rows = prop.defaultRows;
			try {
				l.add(c.batchAuto());
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		ArrayList<Double> all_points = new ArrayList<>();
		ArrayList<Integer> sizes = new ArrayList<>();
		for (Config c : configs) {
			sizes.add(c.size);
		}
		double size_stride = 0.7*Util.calculateMedian(sizes);
		for (ArrayList<Double> n : l) {
			if (n.size() > 0) {
				n.remove(0);
			}
			all_points.addAll(n);
		}
		all_points.add(0.0);
		Collections.sort(all_points);
		ArrayList<Double> centers = new ArrayList<>();
		if (all_points.isEmpty()) {
			System.out.println("Auto detection failed");
			return;
		}
		for (int i = 0; i < 2; i++) {
			centers.clear();
			centers.add(0.0);
			double offset = all_points.get(0);
			double acc = offset;
			System.out.println("acc=" + acc);
			int cnt = 1;
			for (int j = 1; j < all_points.size(); j++) {
				if ((all_points.get(j) - offset) < size_stride) {
					acc += all_points.get(j);
					cnt += 1;
				} else {
					centers.add(acc/cnt);
					offset = acc = all_points.get(j);
					cnt = 1;
					System.out.println(acc);
				}
			}
			// last iteration
			centers.add(acc/cnt);
			System.out.println(acc);
			
			all_points.clear();
			for (Double d : centers) {
				all_points.add(d);
			}
		}
		
		// if enough data points, remove last center since it is often an artifact
		if (centers.size() > 3) {
			centers.remove(centers.size()-1);
		}
					
		// contract centers until not more than requested
		while (centers.size() > prop.defaultCols) {
			// find shortest distance between centers
			double min_distance = Double.MAX_VALUE;
			int contraction_index = 0;
			for (int i = 1; i < centers.size(); i++) {
				if (centers.get(i) - centers.get(i-1) < min_distance) {
					contraction_index = i;
					min_distance = centers.get(i) - centers.get(i-1);
				}
			}
			double new_value = (centers.get(contraction_index) + centers.get(contraction_index-1))/2;
			centers.remove(contraction_index);
			centers.remove(contraction_index-1);
			centers.add(contraction_index-1, new_value);
		}
		
		// now do what semiAuto does
		if (centers.size() < 2) {
			System.out.println("Could not determine horizontal spacing.");
			for (Config c : configs) {
				c.x_highValue = c.cols*(c.y_highValue-c.y_lowValue)/c.rows;
				
				c.updateControls();
				c.update();
				
				c.getAnalysis().calculateSpots();
				c.getAnalysis().autoMask();
			}
		} else if (centers.size() == 2) {
			System.out.println("Could not determine horizontal spacing.");
			double width = (centers.get(1)-centers.get(0));
			for (Config c : configs) { 
				double y_dist = (c.y_highValue-c.y_lowValue)*c.getMat().rows()/(c.rows-1);
				int index = (int) Math.round(width/y_dist);
				index = Math.min(c.cols-1, index-1);
				c.x_highValue = (centers.get(0) + width/index*(c.cols-1))/c.getMat().cols();
				
				c.updateControls();
				c.update();
				
				c.getAnalysis().calculateSpots();
				c.getAnalysis().autoMask();
			}
		} else {
			double min_loss = Double.MAX_VALUE;
			int min_index = centers.size()-1;
			for (int i = min_index+1; i < prop.defaultCols; i++) {
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
			// System.out.println("index: " + min_index);
			double dist = centers.get(centers.size()-1)/(min_index);
			for (Config c : configs) {
				c.x_highValue = ((c.x_lowValue * c.getMat().cols()) + dist*(prop.defaultCols-1))/c.getMat().cols();
				
				c.updateControls();
				c.update();
				
				c.getAnalysis().calculateSpots();
				c.getAnalysis().autoMask();
				
				ArrayList<Double> tmp = new ArrayList<>();
				for (Double d : centers) {
					tmp.add((c.x_lowValue * c.getMat().cols()) + d);
				}
				c.autoValues = tmp;
			}
		}
		getConfig().updateControls();
		
		for (Config c : configs) {
			c.analyzeConfig();
		}
		
		unfixView();
	}
	
	@FXML
	protected void removeMask() {
		getConfig().masked = new boolean[cols.getValue()][rows.getValue()];
		updateView();
	}
	
	@FXML
	protected void openProjectPreferences() {
		Stage stage = new Stage();
	    BorderPane layout;
		
		try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("ProjectPreferences.fxml"));
            layout = (BorderPane) loader.load();
            
            Scene scene = new Scene(layout);
            stage.setScene(scene);
            stage.setTitle("Project preferences");
            stage.show();
            
            ProjectPreferencesController controller = loader.getController();
            controller.setParentController(this);
            controller.reinitialize();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	@FXML
	protected void saveFile() {
		FileChooser fileChooser = new FileChooser();
		if (lastDirectory != null) {
			fileChooser.setInitialDirectory(lastDirectory);
		}
		fileChooser.setInitialFileName(getConfig().getFileName() + ".ser");
		File f = fileChooser.showSaveDialog(rows.getScene().getWindow());
		if (f == null) {
			return;
		}
		lastDirectory = f.getParentFile();
		if (!Util.getExtension(f).equals("ser")) {
			f = new File(f.getAbsolutePath() + ".ser");
		}
		try {
			ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(f));
			getConfig().serialize_mat();
			o.writeObject(getConfig());
			o.writeObject(null);
			o.close();
		} catch (IOException e) {
			System.err.println("Error occured during saving file.");
			e.printStackTrace();
		}
	}
	
	@FXML
	protected void saveAllFiles() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(lastDirectory);
		fileChooser.setInitialFileName("project.ser");
		File f = fileChooser.showSaveDialog(rows.getScene().getWindow());
		if (f == null) {
			return;
		}
		if (!Util.getExtension(f).equals("ser")) {
			f = new File(f.getAbsolutePath() + ".ser");
		}
		
		lastDirectory = f.getParentFile();
		
		try {
			ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(f));
			for (Config c : configs) {
				c.serialize_mat();
				o.writeObject(c);
			}
			o.writeObject(null);
			o.close();
		} catch (IOException e) {
			System.err.println("Error occured during saving file.");
			e.printStackTrace();
		}
	}
	
	public void setNotification(String text) {
		notificationBar.setText(text);
	}
	
	@FXML
	protected void resetConfig() {
		// getConfig().resetMask();
		getConfig().getAnalysis().analyzed = false;
		updateView();
	}
	
	@FXML
	protected void exportTSV() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(lastDirectory);
		fileChooser.setInitialFileName("analysis.tsv");
		File f = fileChooser.showSaveDialog(rows.getScene().getWindow());
		if (f == null) {
			return;
		}
		if (!Util.getExtension(f).equals("tsv")) {
			f = new File(f.getAbsolutePath() + ".tsv");
		}
		
		lastDirectory = f.getParentFile();
		
		boolean all_analyzed = true;
		try {
			PrintWriter pw = new PrintWriter(f);
			for (Config c : configs) {
				if (c.getAnalysis().analyzed) {
					pw.write(c.getAnalysis().toString());
				} else {
					all_analyzed = false;
				}
			}
	        pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (!all_analyzed) {
			Alert alert = new Alert(AlertType.NONE,
        			"Analysis missing for some patterns!", ButtonType.OK);
        	alert.showAndWait();
		}
	}
	
	@FXML
	protected void exportExcel() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(lastDirectory);
		fileChooser.setInitialFileName("analysis.xls");
		File f = fileChooser.showSaveDialog(rows.getScene().getWindow());
		if (f == null) {
			return;
		}
		if (!Util.getExtension(f).equals("xls")) {
			f = new File(f.getAbsolutePath() + ".xls");
		}
		
		lastDirectory = f.getParentFile();
		
		Workbook workbook = new HSSFWorkbook();
		Sheet sheet = workbook.createSheet("results");
		
		Font boldFont = workbook.createFont();
		boldFont.setBold(true);
		CellStyle boldStyle = workbook.createCellStyle();
		boldStyle.setFont(boldFont);
		CellStyle boldRedStyle = workbook.createCellStyle();
		boldRedStyle.setFont(boldFont);
		boldRedStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
		boldRedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		CellStyle boldGreenStyle = workbook.createCellStyle();
		boldGreenStyle.setFont(boldFont);
		boldGreenStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
		boldGreenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		boldGreenStyle.setLeftBorderColor(IndexedColors.BRIGHT_GREEN.getIndex());
		boldGreenStyle.setRightBorderColor(IndexedColors.BRIGHT_GREEN.getIndex());
		boldGreenStyle.setTopBorderColor(IndexedColors.BRIGHT_GREEN.getIndex());
		boldGreenStyle.setBottomBorderColor(IndexedColors.BRIGHT_GREEN.getIndex());
		
		SheetConditionalFormatting scf1 = sheet.getSheetConditionalFormatting();
		
		boolean all_analyzed = true;
		
		int rowNumber = 0;
		for (Config c : configs) {
			// c.semiAuto(0);
			if (!c.getAnalysis().analyzed) {
				all_analyzed = false;
				continue;
			}
			double[][] spots = c.getAnalysis().getSpots();
			boolean[][] masked = c.masked;
			
			Row r;
			Cell cell;
			
			r = sheet.createRow(rowNumber++);
			cell = r.createCell(0);
			cell.setCellValue(c.getFileName());
            cell.setCellStyle(boldStyle);
            rowNumber++;
			
			for (int i = 0; i < spots[0].length; i++) {
				r = sheet.createRow(rowNumber++);
				cell = r.createCell(0);
	            cell.setCellValue("row_" + (i+1));
	            cell.setCellStyle(boldStyle);
	            
	        	for (int j = 0; j < spots.length; j++) {
	        		cell = r.createCell(j+1);
		            cell.setCellValue((int) Math.round(spots[j][i]*65535));
		            if (masked[j][i]) {
		            	cell.setCellStyle(boldRedStyle);
		            }
		            else {
		            	cell.setCellStyle(boldGreenStyle);
		            }
	        	}
	        }
			
			// add mean
			r = sheet.createRow(rowNumber++);
			cell = r.createCell(0);
            cell.setCellValue("mean");
            cell.setCellStyle(boldStyle);
            for (int i = 0; i < spots.length; i++) {
            	cell = r.createCell(i+1);
            	String cell_range_cond = Util.getRowRepresentation(i+1) + (rowNumber+2) +
            			":" + Util.getRowRepresentation(i+1) + (rowNumber+2+spots[0].length-1);
            	/*
            	String cell_range_cont = Util.getRowRepresentation(i+1) + (rowNumber-spots[0].length) +
            			":" + Util.getRowRepresentation(i+1) + (rowNumber-1);
            	*/
            	String formula = "SUM(";
            	for (int j = 0; j < spots[0].length; j++) {
            		String cont = Util.getRowRepresentation(i+1) + (rowNumber-spots[0].length+j);
            		String cond = Util.getRowRepresentation(i+1) + (rowNumber+2+j);
            		formula += "IF(" + cond + "=0," + cont + ",0)";
            		//formula += Util.getRowRepresentation(i+1) + (rowNumber-spots[0].length+j)
            		//		+ "*(1-" + Util.getRowRepresentation(i+1) + (rowNumber+2+j) + ")";
            		if (j < spots[0].length-1) {
            			formula += ",";
            		}
            	}
            	String included_values = spots[0].length + "-SUM(" + cell_range_cond + ")";
	            cell.setCellFormula(formula + ")/(" + included_values + ")");
	            cell.setCellStyle(boldStyle);
			}
            
            // add sd
            r = sheet.createRow(rowNumber++);
			cell = r.createCell(0);
            cell.setCellValue("sd");
            cell.setCellStyle(boldStyle);
            for (int i = 0; i < spots.length; i++) {
            	cell = r.createCell(i+1);
            	String cell_range_cond = Util.getRowRepresentation(i+1) + (rowNumber+1) +
            			":" + Util.getRowRepresentation(i+1) + (rowNumber+1+spots[0].length-1);
            	String included_values = spots[0].length + "-SUM(" + cell_range_cond + ")";
            	
            	String formula = "SQRT((1/(" + included_values + "-1))*SUM(";
            	for (int j = 0; j < spots[0].length; j++) {
            		String cond = Util.getRowRepresentation(i+1) + (rowNumber+1+j);
            		String cont = Util.getRowRepresentation(i+1) + (rowNumber-1-spots[0].length+j);
            		String mean = Util.getRowRepresentation(i+1) + (rowNumber-1);
            		formula += "IF(" + cond + "=0,(" + mean + "-" + cont + ")^2,0)";
            		if (j < spots[0].length-1) {
            			formula += ",";
            		}
            	}
            	// System.out.println(formula);
	            cell.setCellFormula(formula + "))");
	            cell.setCellStyle(boldStyle);
			}
			
            // add mask
            for (int i = 0; i < spots[0].length; i++) {
				r = sheet.createRow(rowNumber++);
				cell = r.createCell(0);
	            cell.setCellValue("mask_" + (i+1));
	            
	        	for (int j = 0; j < spots.length; j++) {
	        		cell = r.createCell(j+1);
		            cell.setCellValue(masked[j][i] ? 1 : 0);     
	        	}
	        }
			
			ConditionalFormattingRule cfr1 = scf1.createConditionalFormattingRule("(indirect(address(row()+" +
					(c.rows + 2) + ", column()))) > 0");
			PatternFormatting patternFmt = cfr1.createPatternFormatting();
		    patternFmt.setFillBackgroundColor(IndexedColors.RED.getIndex());
		    patternFmt.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());
		    
		    ConditionalFormattingRule[] cfRules = {cfr1};
		    int tmp = rowNumber - 1 - 2*(c.rows);
		    CellRangeAddress[] regions = {CellRangeAddress.valueOf("B" + tmp + ":" + ((char)('A' + c.cols)) + (tmp - 1 + c.rows))};
		    // System.out.println("B" + tmp + ":" + ((char)('A' + c.cols)) + (tmp - 1 + c.rows));
		    scf1.addConditionalFormatting(regions, cfRules);

			// two empty rows
			rowNumber += 2;
		}
		
		try {
			FileOutputStream fileOut = new FileOutputStream(f);
			workbook.write(fileOut);
	        fileOut.close();
	        workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (!all_analyzed) {
			Alert alert = new Alert(AlertType.NONE,
        			"Analysis missing for some patterns!", ButtonType.OK);
        	alert.showAndWait();
		}
	}
	
	@FXML
	protected void exportImages() {
		DirectoryChooser fileChooser = new DirectoryChooser();
		fileChooser.setInitialDirectory(lastDirectory);
		File f = fileChooser.showDialog(rows.getScene().getWindow());
		Path dir = Paths.get(f.getPath());
		
		lastDirectory = f.getParentFile();
		
		boolean all_analyzed = true;
		
		for (Config c : configs) {
			if (c.getAnalysis().analyzed) {
				Mat mat = getMat(c);
				mat.convertTo(mat, CvType.CV_8UC3);
				Imgcodecs.imwrite(dir.resolve(c.getFileName() + ".png").toString(), mat);
			} else {
				all_analyzed = false;
			}
		}
		
		if (!all_analyzed) {
			Alert alert = new Alert(AlertType.NONE,
        			"Analysis missing for some patterns!", ButtonType.OK);
        	alert.showAndWait();
		}
	}
	
	@FXML
	protected void toggleShowGrid() {
		showGrid = !showGrid;
		updateView();
	}
}
