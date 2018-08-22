package application;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.scene.image.Image;

public class Util {
	
	// https://stackoverflow.com/a/33605064
	public static Image toImage(Mat mat) {
		MatOfByte byteMat = new MatOfByte();
		Imgcodecs.imencode(".bmp", mat, byteMat);
		return new Image(new ByteArrayInputStream(byteMat.toArray()));
	}
	
	public static Mat fromFile(String path) {
		Mat mat = null;
		BufferedReader reader = null;
		
		// adapted from https://stackoverflow.com/a/3806154
		try {
			reader = new BufferedReader(new FileReader(new File(path)));

			// get image dimensions
		    int cols = Integer.parseInt(reader.readLine());
		    int rows = Integer.parseInt(reader.readLine());
		    mat = new Mat(rows, cols, CvType.CV_16UC1);
		    
		    // skip empty line
		    reader.readLine();
		    
		    for (int i = rows-1; i >= 0; i--) {
		    	for (int j = cols-1; j >= 0; j--) {
		    		short val = (short)(Integer.parseInt(reader.readLine()));
		    		mat.put(i, j, new short[]{val});
		    	}
		    }
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
		    try {
		        if (reader != null) {
		            reader.close();
		        }
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
		}
		
		Core.normalize(mat, mat, 0, 255, Core.NORM_MINMAX);
		Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2RGB);
		return mat;
	}

}
