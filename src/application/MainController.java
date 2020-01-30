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
	
	@FXML
	private Button prev;
	@FXML
	private Button next;
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
			
			if (!s.getEditor().getText().isEmpty()) {
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
			
			if (!t.getText().isEmpty()) {
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
		currentIndex = 0;
		openFiles.setItems(configs);
		
		rows.getValueFactory().setValue(prop.defaultRows);
		cols.getValueFactory().setValue(prop.defaultCols);
		size.getValueFactory().setValue(prop.defaultSize);
		mask.setText("" + prop.defaultMask);
		
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
		} else {
			rows.setDisable(false);
			cols.setDisable(false);
			size.setDisable(false);
			mask.setDisable(false);
			analyzeButton.setDisable(false);
		}

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
		anchorPane.setMinWidth(config.x+16);
		anchorPane.setMinHeight(config.y+16);
		
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

		double alpha = Math.pow(1.1, config.contrast-75);
		double beta = (config.brightness-50)*65536/50*256/65536;
		mat.convertTo(mat, mat.type(), alpha, beta);
		
		if (showGrid) {		
			// draw grid
			for (int i = 0; i < config.rows; i++) {
				for (int j = 0; j < config.cols; j++) {
					Imgproc.rectangle(mat,
							new Point(config.x_lower - config.size/2 + j*config.x_dist, config.y_upper - config.size/2 + i*config.y_dist),
							new Point(config.x_lower + config.size/2 + j*config.x_dist, config.y_upper + config.size/2 + i*config.y_dist),
							new Scalar(0, 0, 255),
							1);
				}	
			}
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
		
		Analysis analysis = config.getAnalysis();
		if (analysis.analyzed) {
			// crop image
			mat = mat.rowRange((int) Math.round(config.y_upper-50),	(int) Math.round(config.y_lower+30));
			
			int displaySize = 110 + (config.rows+1)*60;
			BufferedImage bufferedImage = new BufferedImage(mat.cols(), displaySize, BufferedImage.TYPE_3BYTE_BGR); //USHORT_555_RGB);
	        Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
	        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
	        );
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
	        
	        graphics.setColor(Color.BLACK);
	        graphics.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 16));
	        
	        int i = config.rows;
	        graphics.drawString("m", 13, 80 + i*60);
	        graphics.drawString("sd", 11, 80 + (i+1)*60);
	        
	        analysis.calculateStatistics();
	        
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
		}

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
		boolean autoMask = !getConfig().isMasked();
		getConfig().analyzeConfig(autoMask);
		updateView();
	}
	
	@FXML
	protected void analyzeAll() {
		for (Config c : configs) {
			c.analyzeConfig(!c.isMasked());
		}
		updateView();
	}
	
	@FXML
	protected void resetAll() {
		for (Config c : configs) {
			c.getAnalysis().analyzed = false;
		}
		updateView();
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
	
	@FXML
	protected void showAbout() {
		Alert alert = new Alert(AlertType.NONE,
    			"MCR Spot Reader\n" +
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
	protected void removeMask() {
		getConfig().masked = new boolean[cols.getValue()][rows.getValue()];
		updateView();
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
            	String formula = "SUM(";
            	for (int j = 0; j < spots[0].length; j++) {
            		String cont = Util.getRowRepresentation(i+1) + (rowNumber-spots[0].length+j);
            		String cond = Util.getRowRepresentation(i+1) + (rowNumber+2+j);
            		formula += "IF(" + cond + "=0," + cont + ",0)";
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
