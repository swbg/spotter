package application;

import java.io.File;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ProjectPreferencesController {
	
	private MainController parentController;

	@FXML
	protected Spinner<Integer> rowSpinner;
	@FXML
	protected Spinner<Integer> colSpinner;
	@FXML
	protected TextField filenameTextField;
	@FXML
	protected TextField sensitivityTextField;
	
	private DisabledTextFieldListener l;
	
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
			}
			inProgress = false;
		}
	}
	
	private class SensitivityTextFieldListener implements ChangeListener<String> {
		
		private boolean inProgress = false;
		private TextField t;
		
		private SensitivityTextFieldListener(TextField t) {
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

			inProgress = false;
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
			t.setText(oldValue);
			inProgress = false;
		}
	}

	@SuppressWarnings("unchecked")
	@FXML
	private void initialize() {
		for (Spinner<Integer> s : new Spinner[]{rowSpinner, colSpinner}) {
			s.getEditor().textProperty().addListener(new SpinnerListener(s));
		}
		sensitivityTextField.textProperty().addListener(new SensitivityTextFieldListener(sensitivityTextField));
	}
	
	public void setParentController(MainController p) {
		parentController = p;
	}
	
	public void reinitialize() {
		rowSpinner.getEditor().setText("" + parentController.prop.defaultRows);
		colSpinner.getEditor().setText("" + parentController.prop.defaultCols);
		sensitivityTextField.setText("" + parentController.prop.autoSensitivity);
		
		l = new DisabledTextFieldListener(filenameTextField);
		filenameTextField.setText(parentController.prop.typePath);
		filenameTextField.textProperty().addListener(l);
	}
	
	@FXML
	protected void choosePatternFile() {
		FileChooser fileChooser = new FileChooser();
		File f = fileChooser.showOpenDialog(rowSpinner.getScene().getWindow());
		filenameTextField.textProperty().removeListener(l);
		filenameTextField.setText(f.getAbsolutePath());
		filenameTextField.textProperty().addListener(l);
	}
	
	@FXML
	protected void cancelPreferences() {
		Stage stage = (Stage) rowSpinner.getScene().getWindow();
	    stage.close();
	}
	
	@FXML
	protected void applyPreferences() {
		parentController.setRowsAndCols(rowSpinner.getValue(), colSpinner.getValue());
		parentController.prop.defaultRows = rowSpinner.getValue();
		parentController.prop.defaultCols = colSpinner.getValue();
		parentController.prop.autoSensitivity = Double.parseDouble(sensitivityTextField.getText());
		parentController.prop.updateTypeFile(filenameTextField.getText());
		Stage stage = (Stage) rowSpinner.getScene().getWindow();
	    stage.close();
	}
}
