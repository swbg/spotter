package application;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Prop {
	
	private static final String CONFIGPATH = System.getProperty("user.dir") + "/config/config.properties";

	public int defaultRows;
	public int defaultCols;
	public int defaultSize;
	
	public double fractionBrightestPixels;
	public int minBrightestPixels;
	
	public double defaultMask;
	public double defaultBrightness, defaultContrast;
	
	public Prop() {
		Properties prop = new Properties();
		try {
			InputStream in = new FileInputStream(CONFIGPATH);
			prop.load(in);
		} catch (IOException e) {
			System.out.println("Error loading conifg.properties.");
			System.exit(1);
		}
		System.out.println("### Loading " +  CONFIGPATH + " ###");
		defaultRows = Integer.parseInt(prop.getProperty("defaultRows", "5"));
		System.out.println("defaultRows=" + defaultRows);
		defaultCols = Integer.parseInt(prop.getProperty("defaultCols", "16"));
		System.out.println("defaultCols=" + defaultCols);
		defaultSize = Integer.parseInt(prop.getProperty("defaultSize", "20"));
		System.out.println("defaultSize=" + defaultSize);
		defaultBrightness = Double.parseDouble(prop.getProperty("defaultBrightness", "50.0"));
		System.out.println("defaultBrightness=" + defaultBrightness);
		defaultContrast = Double.parseDouble(prop.getProperty("defaultContrast", "25.0"));
		System.out.println("defaultContrast=" + defaultContrast);
		fractionBrightestPixels = Double.parseDouble(prop.getProperty("fractionBrightestPixels", "0.1"));
		System.out.println("fractionBrightestPixels=" + fractionBrightestPixels);
		minBrightestPixels = Integer.parseInt(prop.getProperty("minBrightestPixels", "10"));
		System.out.println("minBrightestPixels=" + minBrightestPixels);
		defaultMask = Double.parseDouble(prop.getProperty("defaultMask", "0.10"));
		System.out.println("minBrightestPixels=" + minBrightestPixels);
	}
	
}
