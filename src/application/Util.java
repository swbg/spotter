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
		    
		    // mat = new Mat(rows, cols, CvType.CV_32SC3);
		    // mat = new Mat(rows, cols, CvType.CV_16UC3);
		    mat = new Mat(rows, cols, CvType.CV_16UC1);
		    
		    // skip empty line
		    reader.readLine();
		    
		    int max = 0;
		    
//		    for (int i = 0; i < rows; i++) {
//		    	for (int j = 0; j < cols; j++) {
//		    		int val = Integer.parseInt(reader.readLine());
//		    		if (val > max) {
//		    			max = val;
//		    		}
//		    		val = (int)(Math.log(val)*50)-50;
//		    		mat.put(i, j, new int[]{val/10, val/5, val});
//		    	}
//		    }
//		    for (int i = 0; i < rows; i++) {
//		    	for (int j = 0; j < cols; j++) {
//		    		short val = (short)(Integer.parseInt(reader.readLine())%65000);
//		    		mat.put(i, j, new short[]{val, val, val});
//		    	}
//		    }
		    for (int i = 0; i < rows; i++) {
		    	for (int j = 0; j < cols; j++) {
		    		short val = (short)(Integer.parseInt(reader.readLine())%65000);
		    		if (val > max) {
		    			max = val;
		    		}
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
		
//		Mat mat2 = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1);
//		
//		Imgproc.cvtColor(mat, mat2, Imgproc.COLOR_BGR2GRAY, 1);
//		
//		System.out.println(mat2.channels());
//		mat2.convertTo(mat2, CvType.CV_8UC1);
//		Imgproc.equalizeHist(mat2, mat2);
//		return mat2;
		
		// mat.convertTo(mat, CvType.CV_8UC1);
		// Imgproc.equalizeHist(mat, mat);
		
		Core.normalize(mat, mat, 0, 255, Core.NORM_MINMAX);
		return mat;
	}

}
